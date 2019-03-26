package wikilinks;

import javafx.util.Pair;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import persistence.SQLApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class CreateWikiLinks {

    private static final int INTERVAL = 1000;
    private static final int EXTRACT_AMOUNT = 10000;
    private static final int TOTAL_DOCS = 18289732;
    private static final String ELASTIC_INDEX = "enwiki_v2";
    private static final String DB_NAME = "WikiLinks";
    private static final String TABLE_MENTIONS = "Mentions";
    private static final String TABLE_COREF = "CorefChains";

    private final RestHighLevelClient elasticClient = new RestHighLevelClient(
            RestClient.builder(new HttpHost("localhost", 9200, "http")).setRequestConfigCallback(
                    requestConfigBuilder -> requestConfigBuilder
                            .setConnectionRequestTimeout(60*60*1000)
                            .setConnectTimeout(60*60*1000)
                            .setSocketTimeout(60*60*1000))
                    .setMaxRetryTimeoutMillis(60*60*1000));

    private final SQLApi sqlApi = new SQLApi();

    public void readAllAndPerisist() throws IOException {
        System.out.println("Strating process, Reading all documents from wikipedia (elastic)");
        if(!createWikiLinksTables()) {
            System.out.println("Failed to create Database and tables, finishing process");
            return;
        }

        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        ExecutorService pool = new ThreadPoolExecutor(
                4,
                4,
                20,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4),
                rejectedExecutionHandler);

        final Scroll scroll = new Scroll(TimeValue.timeValueHours(5L));
        SearchResponse searchResponse = createElasticSearchResponse(scroll);

        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        int count = 0;
        while (searchHits != null && searchHits.length > 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = elasticClient.searchScroll(scrollRequest);
            scrollId = searchResponse.getScrollId();

            List<Pair<String, String>> pageTexts = new ArrayList<>();
            for (SearchHit hit : searchHits) {
                Pair<String, String> hitResult = extractFromHit(hit);
                if(hitResult != null) {
                    pageTexts.add(hitResult);
                }
            }

//            new ParseAndPersistDataRunner(pageTexts).run();
            pool.execute(new ParseAndPersistDataRunner(pageTexts));
            System.out.println((TOTAL_DOCS - count) + " documents to go");

            if(count >= EXTRACT_AMOUNT) {
                break;
            }

            count += searchHits.length;
            searchHits = searchResponse.getHits().getHits();
        }


        closeScroll(scrollId);

        pool.shutdown();
        closeExecuterService(pool);

        System.out.println("Persisting corefs tables values");
        if (!sqlApi.insertRowsToTable(DB_NAME, TABLE_COREF, new ArrayList<>(WikiLinksCoref.getGlobalCorefMap().values()))) {
            System.out.println("Failed to insert Corefs!!!!");
        }
    }

    private boolean createWikiLinksTables() {
        System.out.println("Creating SQL Tables");
        return sqlApi.createTable(DB_NAME, TABLE_MENTIONS, new WikiLinksMention()) &&
                sqlApi.createTable(DB_NAME, TABLE_COREF, WikiLinksCoref.getCorefChain("####TEMP####"));
    }

    private void closeScroll(String scrollId) throws IOException {
        System.out.println("Done going over all wikipedia documents");
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = elasticClient.clearScroll(clearScrollRequest);
        if(clearScrollResponse.isSucceeded()) {
            System.out.println("Done, Scroll closed!");
        }
    }

    private Pair<String, String> extractFromHit(SearchHit hit) {
        final Map map = hit.getSourceAsMap();
        final String text = (String)map.get("text");
        final String title = (String)map.get("title");
        final Map relations = (Map)map.get("relations");

        if((boolean)relations.get("isDisambiguation") ||
                text.startsWith("#redirect") ||
                text.startsWith("#REDIRECT") ||
                title.toLowerCase().startsWith("file:") ||
                title.toLowerCase().startsWith("wikipedia:")) {
            return null;
        }
        return new Pair<>(title, text);
    }

    private SearchResponse createElasticSearchResponse(Scroll scroll) throws IOException {
        final SearchRequest searchRequest = new SearchRequest(ELASTIC_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchAllQuery());
        searchSourceBuilder.size(INTERVAL);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(scroll);
        return elasticClient.search(searchRequest);
    }

    private void closeExecuterService(ExecutorService pool) {
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(5, TimeUnit.HOURS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void validateAllFutureDone(List<Future<?>> returnFutures) {
        List<WikiLinksMention> mentions = new ArrayList<>();
        if(returnFutures != null) {
            for (Future<?> future : returnFutures) {
                try {
                    future.get(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ParseAndPersistDataRunner implements Runnable {
        private List<Pair<String, String>> texts;

        public ParseAndPersistDataRunner(List<Pair<String, String>> texts) {
            this.texts = texts;
        }

        @Override
        public void run() {
            for(Pair<String, String> pair : this.texts) {
                try {
                    final List<WikiLinksMention> wikiLinksMentions = WikiLinksExtractor.extractFromFile(pair.getKey(), pair.getValue());
                    wikiLinksMentions.stream().forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
                    if (!sqlApi.insertRowsToTable(DB_NAME, TABLE_MENTIONS, wikiLinksMentions)) {
                        System.out.println("Failed to insert mentions Batch!!!!");
                    }
                } catch (Exception e) {
                    System.out.println("Failed to insert into DB with exception");
                    e.printStackTrace();
                }
            }
        }
    }
}
