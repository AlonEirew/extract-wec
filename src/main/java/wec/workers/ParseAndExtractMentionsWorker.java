package wec.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import wec.config.Configuration;
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

    private InfoboxFilter filter = new InfoboxFilter(Configuration.getConfiguration().getInfoboxConfiguration());
    private final WikipediaLinkExtractor extractor = new WikipediaLinkExtractor();

    public ParseAndExtractMentionsWorker() { }

    // Constructor for testing purposes
    ParseAndExtractMentionsWorker(InfoboxFilter filter) {
        this.filter = filter;
    }

    @Override
    public void run() {
        List<WECContext> finalToCommit = new ArrayList<>();
        LOGGER.debug("Preparing to parse " + this.getRawElasticResults().size() + " wikipedia pages and extract mentions");
        for(RawElasticResult rawResult : this.getRawElasticResults()) {
            if(rawResult.getInfobox() != null && !rawResult.getInfobox().isEmpty()) {
                List<WECContext> wecMentions = extractor.extract(rawResult);
                if (!wecMentions.isEmpty()) {
                    finalToCommit.addAll(wecMentions);
                }
            }
        }

        LOGGER.debug("Handle " + finalToCommit.size() + " extracted mentions");
        final Set<String> corefTitleSet = new HashSet<>();
        for(WECContext context : finalToCommit) {
            for (WECMention mention : context.getMentionList()) {
                if (!mention.getCorefChain().wasAlreadyRetrived()) {
                    corefTitleSet.add(mention.getCorefChain().getCorefValue());
                }
            }
        }

        Map<String, RawElasticResult> allWikiPagesTitleAndText = new HashMap<>();
        if(!corefTitleSet.isEmpty()) {
            LOGGER.debug("Sending-" + corefTitleSet.size() + " coref titles to be retrieved from elastic");
            allWikiPagesTitleAndText = WECResources.getElasticApi().getAllWikiCorefPagesFromTitle(corefTitleSet);
        }

        onResponse(finalToCommit, allWikiPagesTitleAndText);
        invokeListener();
    }

    private void onResponse(List<WECContext> finalToCommit, Map<String, RawElasticResult> pagesResults) {
        if(!finalToCommit.isEmpty()) {
            filterUnwantedMentions(finalToCommit, pagesResults);
            LOGGER.debug(+ finalToCommit.size() + " mentions remained after filter and to be committed");
            WECResources.getDbRepository().saveContexts(finalToCommit);
        }
    }

    void filterUnwantedMentions(List<WECContext> finalToCommit, Map<String, RawElasticResult> pagesResults) {
        Iterator<WECContext> contextIterator = finalToCommit.iterator();
        while(contextIterator.hasNext()) {
            WECContext context = contextIterator.next();
            final Iterator<WECMention> mentionsIterator = context.getMentionList().iterator();
            while (mentionsIterator.hasNext()) {
                WECMention ment = mentionsIterator.next();
                if (ment.getCorefChain().isMarkedForRemoval()) {
                    mentionsIterator.remove();
                } else if (!ment.getCorefChain().wasAlreadyRetrived()) {
                    final String corefValue = ment.getCorefChain().getCorefValue();
                    final RawElasticResult rawElasticResult = pagesResults.get(corefValue);
                    if (!this.filter.isConditionMet(rawElasticResult)) {
                        LOGGER.trace("Removing Coref page-" + corefValue);
                        ment.getCorefChain().setMarkedForRemoval(true);
                        mentionsIterator.remove();
                    }
                    ment.getCorefChain().setWasAlreadyRetrived(true);
                }
            }

            if(context.getMentionList().isEmpty()) {
                contextIterator.remove();
            }
        }
    }
}