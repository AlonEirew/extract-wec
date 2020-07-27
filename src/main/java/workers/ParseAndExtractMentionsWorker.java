package workers;

import data.RawElasticResult;
import data.WECMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import utils.ExecutorServiceFactory;
import wec.ICorefFilter;
import wec.WECLinksExtractor;

import java.util.*;

public class ParseAndExtractMentionsWorker extends AWorker {
    private final static Logger LOGGER = LogManager.getLogger(ParseAndExtractMentionsWorker.class);

    private final List<WECMention> finalToCommit = new ArrayList<>();

    private final SQLQueryApi sqlApi;
    private final ElasticQueryApi elasticApi;
    private final ICorefFilter filter;

    public ParseAndExtractMentionsWorker(List<RawElasticResult> rawElasticResults, SQLQueryApi sqlApi,
                                         ElasticQueryApi elasticApi, ICorefFilter filter) {
        super(rawElasticResults);
        this.sqlApi = sqlApi;
        this.elasticApi = elasticApi;
        this.filter = filter;
    }

    // Constructor for testing purposes
    ParseAndExtractMentionsWorker(List<WECMention> finalToCommit, ICorefFilter filter) {
        this(new ArrayList<>(), null, null, filter);
        this.finalToCommit.addAll(finalToCommit);
    }

    @Override
    public void run() {
        LOGGER.info("Parsing the wikipedia pages and extracting mentions");
        for(RawElasticResult rowResult : this.rawElasticResults) {
            List<WECMention> WECMentions = WECLinksExtractor.extractFromWikipedia(rowResult.getTitle(), rowResult.getText());
            WECMentions.forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
            this.finalToCommit.addAll(WECMentions);
        }

        LOGGER.info("Handle all worker mentions...in total-" + finalToCommit.size() + " will be handled");
        final Set<String> corefTitleSet = new HashSet<>();
        for(WECMention mention : this.finalToCommit) {
            if(!mention.getCorefChain().wasAlreadyRetrived()) {
                corefTitleSet.add(mention.getCorefChain().getCorefValue());
            }
        }

        if(!corefTitleSet.isEmpty()) {
            LOGGER.info("Sending-" + corefTitleSet.size() + " coref titles to be retrieved");
            ExecutorServiceFactory.submit(() -> this.elasticApi.getAllWikiPagesTitleAndTextAsync(corefTitleSet, this));
        } else {
            onResponse(new HashMap<>());
        }
    }

    public void onResponse(Map<String, String> pagesResults) {
        LOGGER.info("processing returned results from elastic MultiSearchRequest");
        filterUnwantedMentions(pagesResults);
        if(!this.finalToCommit.isEmpty()) {
            ExecutorServiceFactory.submit(() -> sqlApi.commitMentions(finalToCommit));
        }
    }

    void filterUnwantedMentions(Map<String, String> pagesResults) {
        final Iterator<WECMention> iterator = this.finalToCommit.iterator();
        while (iterator.hasNext()) {
            WECMention ment = iterator.next();

            if(ment.getCorefChain().isMarkedForRemoval()) {
                iterator.remove();
            } else if(!ment.getCorefChain().wasAlreadyRetrived()) {
                ment.getCorefChain().setWasAlreadyRetrived(true);
                final String corefValue = ment.getCorefChain().getCorefValue();
                final String pageText = pagesResults.get(corefValue);
                RawElasticResult rawElasticResult = new RawElasticResult(corefValue, pageText);
                if(!this.filter.isConditionMet(rawElasticResult)) {
                    ment.getCorefChain().setMarkedForRemoval(true);
                    iterator.remove();
                }
            }
        }
    }

    List<WECMention> getFinalToCommit() {
        return finalToCommit;
    }
}