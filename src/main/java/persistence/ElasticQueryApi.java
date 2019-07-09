package persistence;

import data.RawElasticResult;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import utils.ExecutorServiceFactory;
import wikilinks.WikiLinksExtractor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ElasticQueryApi {
    private final RestHighLevelClient elasticClient;
    private final String elasticIndex;
    private final int queryInterval;

    public ElasticQueryApi(String elasticIndex, int queryInterval, String host, int port) {
        this.elasticIndex = elasticIndex;
        this.queryInterval = queryInterval;

        this.elasticClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host,
                        port, "http")).setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder
                                .setConnectionRequestTimeout(60*60*1000)
                                .setConnectTimeout(60*60*1000)
                                .setSocketTimeout(60*60*1000))
                        .setMaxRetryTimeoutMillis(60*60*1000));
    }

    public void closeElasticQueryApi() {
        try {
            this.elasticClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getTotalDocsCount() throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(0);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(sourceBuilder);

        final SearchResponse search = this.elasticClient.search(searchRequest);
        final long hitsCount = search.getHits().getTotalHits();
        return hitsCount;
    }

    public String getPageText(String pageTitle) throws IOException {
        String pageText = null;
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchPhraseQuery("title.keyword", pageTitle));
        sourceBuilder.from(0);
        sourceBuilder.size(5);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(sourceBuilder);

        final SearchResponse search = this.elasticClient.search(searchRequest);
        final SearchHit[] hits = search.getHits().getHits();
        if(hits.length > 0) {
            final RawElasticResult rawElasticResult = extractFromHit(hits[0]);
            if(rawElasticResult != null) {
                pageText = rawElasticResult.getText();
            }
        }

        return pageText;
    }

    public Map<String, String> getAllWikiPagesTitleAndText(Set<String> pagesTitles) throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("Got total of-" + pagesTitles.size() + " coref pages to extract from elastic");

        ExecutorService elasticSearchPool = new ThreadPoolExecutor(
                10,
                20,
                20,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadPoolExecutor.CallerRunsPolicy());

        List<Future<List<RawElasticResult>>> futureList = new ArrayList<>();

        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        int index = 0;
        for(String page : pagesTitles) {
            SearchRequest searchRequest = new SearchRequest();
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("title.keyword", page));
            searchRequest.source(searchSourceBuilder);
            multiSearchRequest.add(searchRequest);

            index ++;
            if(index % 1000 == 0) {
                futureList.add(elasticSearchPool.submit(new ElasticSearchCallRequest(multiSearchRequest)));
                multiSearchRequest = new MultiSearchRequest();
                System.out.println("Done extracting " + index + " coref pages");
            }
        }

        futureList.add(elasticSearchPool.submit(new ElasticSearchCallRequest(multiSearchRequest)));
        elasticSearchPool.shutdown();

        Map<String, String> pagesResults = new HashMap<>();
        for(Future<List<RawElasticResult>> future : futureList) {
            final List<RawElasticResult> rawElasticResults = future.get(1000, TimeUnit.SECONDS);
            for(RawElasticResult result : rawElasticResults) {
                pagesResults.put(result.getTitle(), result.getText());
            }
        }

        ExecutorServiceFactory.closeService(elasticSearchPool);
        return pagesResults;
    }

    private RawElasticResult extractFromHit(SearchHit hit) {
        final String id = hit.getId();
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
        return new RawElasticResult(id, title, text);
    }

    public List<RawElasticResult> getNextScrollResults(SearchHit[] searchHits) throws IOException {
        List<RawElasticResult> rawElasticResults = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            RawElasticResult hitResult = extractFromHit(hit);
            if(hitResult != null) {
                rawElasticResults.add(hitResult);
            }
        }

        return rawElasticResults;
    }

    public SearchResponse getSearchScroll(SearchScrollRequest scrollRequest) throws IOException {
        return this.elasticClient.searchScroll(scrollRequest);
    }

    public SearchResponse createElasticSearchResponse(Scroll scroll) throws IOException {
        final SearchRequest searchRequest = new SearchRequest(this.elasticIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchAllQuery());
        searchSourceBuilder.size(this.queryInterval);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(scroll);
        return elasticClient.search(searchRequest);
    }

    public void closeScroll(String scrollId) throws IOException {
        System.out.println("Done going over all wikipedia documents");
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = this.elasticClient.clearScroll(clearScrollRequest);
        if(clearScrollResponse.isSucceeded()) {
            System.out.println("Done, Scroll closed!");
        }
    }

    class ElasticSearchCallRequest implements Callable<List<RawElasticResult>> {

        private MultiSearchRequest request;

        public ElasticSearchCallRequest(MultiSearchRequest request) {
            this.request = request;
        }

        @Override
        public List<RawElasticResult> call() throws Exception {
            List<RawElasticResult> rawResults = new ArrayList<>();
            MultiSearchResponse response = elasticClient.multiSearch(request);
            for(MultiSearchResponse.Item item : response.getResponses()) {
                final SearchHit[] hits = item.getResponse().getHits().getHits();
                if(hits.length > 0) {
                    final RawElasticResult rawElasticResult = extractFromHit(hits[0]);
                    if(rawElasticResult != null) {
                        final String infoBox = WikiLinksExtractor.extractPageInfoBox(rawElasticResult.getText());
                        rawResults.add(new RawElasticResult(rawElasticResult.getTitle(), infoBox));
                    }
                }
            }

            return rawResults;
        }
    }
}
