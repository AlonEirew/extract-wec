package wec.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import wec.data.RawElasticResult;
import wec.data.WECContext;
import wec.data.WECCoref;
import wec.data.WECMention;
import wec.extractors.WikipediaLinkExtractor;
import wec.filters.InfoboxFilter;
import wec.persistence.WECResources;

import javax.persistence.EntityTransaction;
import javax.transaction.UserTransaction;
import java.util.*;

public class ParseAndExtractMentionsWorker extends AWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseAndExtractMentionsWorker.class);

    private final InfoboxFilter filter;
    private final WikipediaLinkExtractor extractor = new WikipediaLinkExtractor();

    public ParseAndExtractMentionsWorker(List<RawElasticResult> rawElasticResults, InfoboxFilter filter) {
        super(rawElasticResults);
        this.filter = filter;
    }

    // Constructor for testing purposes
    ParseAndExtractMentionsWorker(InfoboxFilter filter) {
        this(new ArrayList<>(), filter);
    }

    @Override
    public void run() {
        List<WECMention> finalToCommit = new ArrayList<>();
        LOGGER.info("Parsing the wikipedia pages and extracting mentions");
        for(RawElasticResult rawResult : this.rawElasticResults) {
            if(rawResult.getInfobox() != null && !rawResult.getInfobox().isEmpty()) {
                List<WECMention> wecMentions = extractor.extract(rawResult);
                if (!wecMentions.isEmpty()) {
                    finalToCommit.addAll(wecMentions);
                }
            }
        }

        LOGGER.info("Handle all worker mentions...in total-" + finalToCommit.size() + " will be handled");
        final Set<String> corefTitleSet = new HashSet<>();
        for(WECMention mention : finalToCommit) {
            if(!mention.getCorefChain().wasAlreadyRetrived()) {
                corefTitleSet.add(mention.getCorefChain().getCorefValue());
            }
        }

        Map<String, RawElasticResult> allWikiPagesTitleAndText = new HashMap<>();
        if(!corefTitleSet.isEmpty()) {
            LOGGER.info("Sending-" + corefTitleSet.size() + " coref titles to be retrieved");
            allWikiPagesTitleAndText = WECResources.getElasticApi().getAllWikiCorefPagesFromTitle(corefTitleSet);
        }

        onResponse(finalToCommit, allWikiPagesTitleAndText);
    }

    private void onResponse(List<WECMention> finalToCommit, Map<String, RawElasticResult> pagesResults) {
        LOGGER.info("processing returned results and preparing to commit");
        if(!finalToCommit.isEmpty()) {
            filterUnwantedMentions(finalToCommit, pagesResults);
            WECResources.getDbRepository().saveMentionsList(finalToCommit);
        }
    }

    void filterUnwantedMentions(List<WECMention> finalToCommit, Map<String, RawElasticResult> pagesResults) {
        final Iterator<WECMention> iterator = finalToCommit.iterator();
        while (iterator.hasNext()) {
            WECMention ment = iterator.next();
            if(ment.getCorefChain().isMarkedForRemoval()) {
                iterator.remove();
            } else if(!ment.getCorefChain().wasAlreadyRetrived()) {
                final String corefValue = ment.getCorefChain().getCorefValue();
                final RawElasticResult rawElasticResult = pagesResults.get(corefValue);
                if(!this.filter.isConditionMet(rawElasticResult)) {
                    LOGGER.trace("Removing Coref page-" + corefValue);
                    ment.getCorefChain().setMarkedForRemoval(true);
                    iterator.remove();
                }
                ment.getCorefChain().setWasAlreadyRetrived(true);
            }
        }
    }
}