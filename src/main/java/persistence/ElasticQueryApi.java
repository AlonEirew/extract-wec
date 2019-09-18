package persistence;

import data.RawElasticResult;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import wikilinks.WikiLinksExtractor;
import workers.ParseAndExtractMentionsWorker;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ElasticQueryApi implements Closeable {
    private final static Logger LOGGER = LogManager.getLogger(ElasticQueryApi.class);
    private final RestHighLevelClient elasticClient;
    private final String elasticIndex;
    private final int queryInterval;

    private final AtomicInteger asyncRequests = new AtomicInteger(0);

    public ElasticQueryApi(String elasticIndex, int queryInterval, String host, int port) {
        this.elasticIndex = elasticIndex;
        this.queryInterval = queryInterval;

        this.elasticClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host,
                        port, "http")).setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder
                                .setConnectionRequestTimeout(60*60*10000)
                                .setConnectTimeout(60*60*10000)
                                .setSocketTimeout(60*60*10000))
                        .setMaxRetryTimeoutMillis(60*60*10000));
    }

    public long getTotalDocsCount() throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(0);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        SearchRequest searchRequest = new SearchRequest(this.elasticIndex);
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
        SearchRequest searchRequest = new SearchRequest(this.elasticIndex);
        searchRequest.source(sourceBuilder);

        final SearchResponse search = this.elasticClient.search(searchRequest);
        final SearchHit[] hits = search.getHits().getHits();
        if(hits.length > 0) {
            final RawElasticResult rawElasticResult = ElasticQueryApi.extractFromHit(hits[0]);
            if(rawElasticResult != null) {
                pageText = rawElasticResult.getText();
            }
        }

        return pageText;
    }

    public void getAllWikiPagesTitleAndTextAsync(Set<String> pagesTitles, ParseAndExtractMentionsWorker listener) {
        LOGGER.info("Got total of-" + pagesTitles.size() + " coref pages to extract from elastic");
        ElasticActionListener actionListener = new ElasticActionListener(listener);
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        int index = 0;
        try {
            for (String page : pagesTitles) {
                SearchRequest searchRequest = new SearchRequest(this.elasticIndex);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("title.keyword", page));
                searchRequest.source(searchSourceBuilder);
                multiSearchRequest.add(searchRequest);

                index++;
                if (index % 500 == 0) {
                    this.asyncRequests.incrementAndGet();
                    actionListener.incAsyncRequest();
                    elasticClient.multiSearchAsync(multiSearchRequest, actionListener);
                    multiSearchRequest = new MultiSearchRequest();
                    LOGGER.info("Done extracting " + index + " coref pages");
                }
            }
            actionListener.incAsyncRequest();
            this.asyncRequests.incrementAndGet();
            elasticClient.multiSearchAsync(multiSearchRequest, actionListener);

        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public static RawElasticResult extractFromHit(SearchHit hit) {
        final String id = hit.getId();
        final Map map = hit.getSourceAsMap();
        final String text = (String)map.get("text");
        final String title = (String)map.get("title");
        final Map relations = (Map)map.get("relations");

        if(relations != null && relations.containsKey("isDisambiguation")) {
            if ((boolean) relations.get("isDisambiguation") ||
                    text.startsWith("#redirect") ||
                    text.startsWith("#REDIRECT") ||
                    title.toLowerCase().startsWith("file:") ||
                    title.toLowerCase().startsWith("wikipedia:")) {
                return null;
            }
        }
        return new RawElasticResult(id, title, text);
    }

    public List<RawElasticResult> getNextScrollResults(SearchHit[] searchHits) {
        List<RawElasticResult> rawElasticResults = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            RawElasticResult hitResult = ElasticQueryApi.extractFromHit(hit);
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
        LOGGER.info("Done going over all wikipedia documents");
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = this.elasticClient.clearScroll(clearScrollRequest);
        if(clearScrollResponse.isSucceeded()) {
            LOGGER.info("Done, Scroll closed!");
        }
    }

    @Override
    public void close() {
        try {
            while(this.asyncRequests.get() > 0) {
                try {
                    LOGGER.info("There is still " + this.asyncRequests.get() + " async request running cannot close elastic client");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error("Failed to wait to running async threads...", e);
                }
            }
            this.elasticClient.close();
        } catch (IOException e) {
            LOGGER.error("Elastic Client did not close", e);
        }
    }

    class ElasticActionListener implements ActionListener<MultiSearchResponse> {

        private AtomicInteger asyncReq = new AtomicInteger(0);
        private ParseAndExtractMentionsWorker listener;
        private Map<String, String> pagesResults = new HashMap<>();

        public ElasticActionListener(ParseAndExtractMentionsWorker listener) {
            this.listener = listener;
        }

        @Override
        public void onResponse(MultiSearchResponse response) {
            final List<RawElasticResult> rawResults = new ArrayList<>();
            for(MultiSearchResponse.Item item : response.getResponses()) {
                final SearchHit[] hits = item.getResponse().getHits().getHits();
                if(hits.length > 0) {
                    final RawElasticResult rawElasticResult = ElasticQueryApi.extractFromHit(hits[0]);
                    if(rawElasticResult != null) {
                        final String infoBox = WikiLinksExtractor.extractPageInfoBox(rawElasticResult.getText());
                        rawResults.add(new RawElasticResult(rawElasticResult.getTitle(), infoBox));
                    }
                }
            }

            for(RawElasticResult result : rawResults) {
                pagesResults.put(result.getTitle(), result.getText());
            }

            this.decAsyncRequest();
            if(this.asyncReq.get() == 0) {
                this.listener.onResponse(pagesResults);
            }

            asyncRequests.decrementAndGet();
        }

        @Override
        public void onFailure(Exception e) {
            this.decAsyncRequest();
            asyncRequests.decrementAndGet();
            LOGGER.error("Failed to retrieve multiSearchRequest", e);
        }

        public void incAsyncRequest() {
            this.asyncReq.incrementAndGet();
        }

        public void decAsyncRequest() {
            this.asyncReq.decrementAndGet();
        }
    }
}
