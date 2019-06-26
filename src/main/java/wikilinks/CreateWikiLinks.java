package wikilinks;

import data.CorefType;
import data.RawElasticResult;
import data.WikiLinksCoref;
import data.WikiLinksMention;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import utils.ExecutorServiceFactory;
import workers.IWorkerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class CreateWikiLinks {

    private final SQLQueryApi sqlApi;
    private final Map<String, String> config;
    private final IWorkerFactory workerFactory;
    private final ElasticQueryApi elasticApi;

    public CreateWikiLinks(SQLQueryApi sqlApi, ElasticQueryApi elasticApi, Map<String, String> configuration, IWorkerFactory workerFactory) {
        this.sqlApi = sqlApi;
        this.elasticApi = elasticApi;
        this.config = configuration;
        this.workerFactory = workerFactory;
    }

    public void readAllWikiPagesAndProcess() throws IOException, SQLException {
        System.out.println("Strating process, Reading all documents from wikipedia (elastic)");

        if(!createSQLWikiLinksTables()) {
            System.out.println("Failed to create Database and tables, finishing process");
            return;
        }

        final int poolSize = Integer.parseInt(this.config.get("pool_size"));
        ExecutorService parsePool = ExecutorServiceFactory.getExecutorService(poolSize);

        long totalDocsCount = this.elasticApi.getTotalDocsCount();
        final int totalAmountToExtract = Integer.parseInt(this.config.get("total_amount_to_extract"));

        final Scroll scroll = new Scroll(TimeValue.timeValueHours(5L));
        SearchResponse searchResponse = this.elasticApi.createElasticSearchResponse(scroll);

        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        int count = 0;
        while (searchHits != null && searchHits.length > 0) {
            List<RawElasticResult> rawElasticResults = this.elasticApi.getNextScrollResults(scrollId, scroll, searchHits);
            parsePool.submit(this.workerFactory.createNewWorker(rawElasticResults));
            System.out.println((totalDocsCount - count) + " documents to go");

            if(count >= totalAmountToExtract) {
                break;
            }

            count += searchHits.length;
            searchHits = searchResponse.getHits().getHits();
        }


        elasticApi.closeScroll(scrollId);

        parsePool.shutdown();
        ExecutorServiceFactory.closeService(parsePool);

        System.out.println("Handling last mentions if exists");
        this.workerFactory.finalizeIfNeeded();

        System.out.println("Persisting corefs tables values");
        final Collection<WikiLinksCoref> allCorefs = WikiLinksCoref.getGlobalCorefMap().values();
        final Iterator<WikiLinksCoref> corefIterator = allCorefs.iterator();

        while(corefIterator.hasNext()) {
            final WikiLinksCoref wikiLinksCoref = corefIterator.next();
            if(wikiLinksCoref.getMentionsCount() < 2 || wikiLinksCoref.getCorefType() == CorefType.NA ||
                    wikiLinksCoref.isMarkedForRemoval()) {
                corefIterator.remove();
            }
        }

        try {
            if (!this.sqlApi.insertRowsToTable(new ArrayList<>(allCorefs))) {
                System.out.println("Failed to insert Corefs!!!!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean createSQLWikiLinksTables() throws SQLException {
        System.out.println("Creating SQL Tables");
        return this.sqlApi.createTable(new WikiLinksMention()) &&
                this.sqlApi.createTable(WikiLinksCoref.getAndSetIfNotExistCorefChain("####TEMP####"));
    }
}
