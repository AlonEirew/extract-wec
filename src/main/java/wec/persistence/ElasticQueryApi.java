package wec.persistence;

import wec.config.Configuration;
import wec.data.RawElasticResult;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ElasticQueryApi implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticQueryApi.class);
    private final RestHighLevelClient elasticClient;
    private final String elasticIndex;
    private final int queryInterval;
    private final int multiRequestInterval;

    public ElasticQueryApi(String elasticIndex, int queryInterval, int multiRequestInterval, String host, int port) {
        this.elasticIndex = elasticIndex;
        this.queryInterval = queryInterval;
        this.multiRequestInterval = multiRequestInterval;

        this.elasticClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host,
                        port, "http")).setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder
                                .setConnectionRequestTimeout(60*60*10000)
                                .setConnectTimeout(60*60*10000)
                                .setSocketTimeout(60*60*10000))
                        .setMaxRetryTimeoutMillis(60*60*10000));
    }

    public ElasticQueryApi() {
        this(Configuration.getConfiguration().getElasticWikiIndex(),
                Configuration.getConfiguration().getElasticSearchInterval(),
                Configuration.getConfiguration().getMultiRequestInterval(),
                Configuration.getConfiguration().getElasticHost(),
                Configuration.getConfiguration().getElasticPort());
    }

    public long getTotalDocsCount() throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(0);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        SearchRequest searchRequest = new SearchRequest(this.elasticIndex);
        searchRequest.source(sourceBuilder);

        final SearchResponse search = this.elasticClient.search(searchRequest);
        return search.getHits().getTotalHits();
    }

    public Map<String, RawElasticResult> getAllWikiCorefPagesFromTitle(Set<String> pagesTitles) {
        LOGGER.info("Got total of-" + pagesTitles.size() + " coref pages to extract from elastic");
        Map<String, RawElasticResult> allResponses = new HashMap<>();
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        int index = 0;
        try {
            for (String page : pagesTitles) {
                SearchRequest searchRequest = createSearchRequest(page);
                multiSearchRequest.add(searchRequest);

                index++;
                if (index % this.multiRequestInterval == 0) {
                    if(!multiSearchRequest.requests().isEmpty()) {
                        allResponses.putAll(onResponse(elasticClient.multiSearch(multiSearchRequest)));
                        multiSearchRequest = new MultiSearchRequest();
                        LOGGER.debug("Done extracting " + index + " coref pages");
                    }
                }
            }

            if(!multiSearchRequest.requests().isEmpty()) {
                allResponses.putAll(onResponse(elasticClient.multiSearch(multiSearchRequest)));
            }

        } catch (Exception e) {
            LOGGER.error("getAllWikiPagesTitleAndText:", e);
        }

        return allResponses;
    }

    private SearchRequest createSearchRequest(String query) {
        SearchRequest searchRequest = new SearchRequest(this.elasticIndex);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("title.keyword", query));
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

    private Map<String, RawElasticResult> onResponse(MultiSearchResponse response) throws IOException {
        final Map<String, RawElasticResult> rawResults = new HashMap<>();
        for(MultiSearchResponse.Item item : response.getResponses()) {
            SearchHit[] hits = item.getResponse().getHits().getHits();
            if(hits.length > 0) {
                RawElasticResult rawElasticResult = this.extractFromHit(hits[0]);
                if(rawElasticResult != null) {
                    if (rawElasticResult.getRedirect() != null && !rawElasticResult.getRedirect().isEmpty()) {
                        SearchResponse search = elasticClient.search(createSearchRequest(rawElasticResult.getRedirect()));
                        hits = search.getHits().getHits();
                        if (hits.length > 0) {
                            rawElasticResult = extractFromHit(hits[0]);
                            if (rawElasticResult != null) {
                                rawResults.put(rawElasticResult.getTitle(), rawElasticResult);
                            }
                        }
                    } else {
                        rawResults.put(rawElasticResult.getTitle(), rawElasticResult);
                    }
                }
            }
        }

        return rawResults;
    }

    public RawElasticResult extractFromHit(SearchHit hit) {
        final String id = hit.getId();
        final Map map = hit.getSourceAsMap();
        final String text = (String) map.get("text");
        final String title = (String) map.get("title");
        final String redirect = (String) map.get("redirectTitle");
        final Map relations = (Map) map.get("relations");
        final boolean isDisambig = (Boolean) relations.get("isDisambiguation");
        final String infobox = (String) relations.get("infobox");

        if (isDisambig) {
            return null;
        }

        return new RawElasticResult(id, title, text, infobox, redirect);
    }

    public List<RawElasticResult> getNextScrollResults(SearchHit[] searchHits) {
        List<RawElasticResult> rawElasticResults = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            RawElasticResult hitResult = this.extractFromHit(hit);
            if(hitResult != null && (hitResult.getRedirect() == null || hitResult.getRedirect().isEmpty())) {
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
            this.elasticClient.close();
        } catch (IOException e) {
            LOGGER.error("Elastic Client did not close", e);
        }
    }
}
