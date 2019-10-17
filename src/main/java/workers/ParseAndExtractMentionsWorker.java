package workers;

import data.RawElasticResult;
import data.WikiLinksMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.search.SearchHit;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import utils.ExecutorServiceFactory;
import wikilinks.ICorefFilter;
import wikilinks.WikiLinksExtractor;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ParseAndExtractMentionsWorker extends AWorker {
    private final static Logger LOGGER = LogManager.getLogger(ParseAndExtractMentionsWorker.class);

    private final List<WikiLinksMention> finalToCommit = new ArrayList<>();

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
    ParseAndExtractMentionsWorker(List<WikiLinksMention> finalToCommit, ICorefFilter filter) {
        this(new ArrayList<>(), null, null, filter);
        this.finalToCommit.addAll(finalToCommit);
    }

    @Override
    public void run() {
        LOGGER.info("Parsing the wikipedia pages and extracting mentions");
        for(RawElasticResult rowResult : this.rawElasticResults) {
            List<WikiLinksMention> wikiLinksMentions = WikiLinksExtractor.extractFromWikipedia(rowResult.getTitle(), rowResult.getText());
            wikiLinksMentions.forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
            this.finalToCommit.addAll(wikiLinksMentions);
        }

        LOGGER.info("Handle all worker mentions...in total-" + finalToCommit.size() + " will be handled");
        final Set<String> corefTitleSet = new HashSet<>();
        for(WikiLinksMention mention : this.finalToCommit) {
            if(!mention.getCorefChain().wasRetrived()) {
                corefTitleSet.add(mention.getCorefChain().getCorefValue());
            }
        }

        LOGGER.info("Sending-" + corefTitleSet.size() + " coref titles to be retrieved");
        ExecutorServiceFactory.submit(() -> this.elasticApi.getAllWikiPagesTitleAndTextAsync(corefTitleSet, this));
    }

    public void onResponse(Map<String, String> pagesResults) {
        LOGGER.info("processing returned results from elastic MultiSearchRequest");
        filterUnwantedMentions(pagesResults);
        ExecutorServiceFactory.submit(() -> sqlApi.commitMentions(finalToCommit));
    }

    void filterUnwantedMentions(Map<String, String> pagesResults) {
        final Iterator<WikiLinksMention> iterator = this.finalToCommit.iterator();
        while (iterator.hasNext()) {
            WikiLinksMention ment = iterator.next();

            if(ment.getCorefChain().isMarkedForRemoval()) {
                iterator.remove();
            } else if(!ment.getCorefChain().wasRetrived()) {
                ment.getCorefChain().setWasRetrived(true);
                final String corefValue = ment.getCorefChain().getCorefValue();
                final String pageText = pagesResults.get(corefValue);
                RawElasticResult rawElasticResult = new RawElasticResult(corefValue, pageText);
                if(this.filter.isConditionMet(rawElasticResult)) {
                    ment.getCorefChain().setMarkedForRemoval(true);
                    iterator.remove();
                }
            }
        }
    }

    List<WikiLinksMention> getFinalToCommit() {
        return finalToCommit;
    }
}