package workers;

import data.RawElasticResult;
import data.WECMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.WECResources;
import wec.InfoboxFilter;
import wec.extractors.WikipediaLinkExtractor;

import java.util.*;

public class ParseAndExtractMentionsWorker extends AWorker {
    private final static Logger LOGGER = LogManager.getLogger(ParseAndExtractMentionsWorker.class);

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
        LOGGER.info("processing returned results from elastic MultiSearchRequest");
        if(!finalToCommit.isEmpty()) {
            finalToCommit = filterUnwantedMentions(finalToCommit, pagesResults);
            WECResources.getSqlApi().commitMentions(finalToCommit);
        }
    }

    List<WECMention> filterUnwantedMentions(List<WECMention> finalToCommit, Map<String, RawElasticResult> pagesResults) {
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

        return finalToCommit;
    }
}