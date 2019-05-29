package wikilinks;

import data.CorefType;
import data.RawElasticResult;
import data.WikiLinksCoref;
import data.WikiLinksMention;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import persistence.SQLQueryApi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class CreateWikiLinks {

    private final SQLQueryApi sqlApi;
    private final Map<String, String> config;

    private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

    private final RestHighLevelClient elasticClient;

    public CreateWikiLinks(SQLQueryApi sqlApi, Map<String, String> configuration) {
        this.sqlApi = sqlApi;
        this.config = configuration;
        this.elasticClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost(this.config.get("elastic_host"),
                        Integer.parseInt(this.config.get("elastic_port")), "http")).setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder
                                .setConnectionRequestTimeout(60*60*1000)
                                .setConnectTimeout(60*60*1000)
                                .setSocketTimeout(60*60*1000))
                        .setMaxRetryTimeoutMillis(60*60*1000));
    }

    public void readAllAndPerisist() throws IOException, SQLException {
        System.out.println("Strating process, Reading all documents from wikipedia (elastic)");
        if(!createWikiLinksTables()) {
            System.out.println("Failed to create Database and tables, finishing process");
            return;
        }

        final int pool_size = Integer.parseInt(this.config.get("pool_size"));
        ExecutorService parsePool = new ThreadPoolExecutor(
                pool_size,
                2 * pool_size,
                20,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                rejectedExecutionHandler);

        long totalDocsCount = getTotalDocsCount();
        final int totalAmountToExtract = Integer.parseInt(this.config.get("total_amount_to_extract"));

        final Scroll scroll = new Scroll(TimeValue.timeValueHours(5L));
        SearchResponse searchResponse = createElasticSearchResponse(scroll);

//        ParseListener listener = new ParseListener(this.sqlApi, (ICorefFilter<WikiLinksMention>) input -> input.getAndSetIfNotExistCorefChain().getMentionsCount() == 1);
        ParseListener listener = new ParseListener(this.sqlApi, this, new PersonOrEventFilter());

        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        int count = 0;
        while (searchHits != null && searchHits.length > 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = this.elasticClient.searchScroll(scrollRequest);
            scrollId = searchResponse.getScrollId();

            List<RawElasticResult> rawElasticResults = new ArrayList<>();
            for (SearchHit hit : searchHits) {
                RawElasticResult hitResult = extractFromHit(hit);
                if(hitResult != null) {
                    rawElasticResults.add(hitResult);
                }
            }

//            new ParseAndExtractMentions(pageTexts).run();
            parsePool.submit(new ParseAndExtractMentions(rawElasticResults, listener));
            System.out.println((totalDocsCount - count) + " documents to go");

            if(count >= totalAmountToExtract) {
                break;
            }

            count += searchHits.length;
            searchHits = searchResponse.getHits().getHits();
        }


        closeScroll(scrollId);

        parsePool.shutdown();
        closeExecuterService(parsePool);

        System.out.println("Handling last mentions if exists");
        listener.handle();

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

    long getTotalDocsCount() throws IOException {
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

    String getPageText(String pageTitle) throws IOException {
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

    Map<String, String> getAllPagesTitleAndText(Set<String> pagesTitles) throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("Got total of-" + pagesTitles.size() + " coref pages to extract from elastic");

        ExecutorService elasticSearchPool = new ThreadPoolExecutor(
                10,
                20,
                20,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                this.rejectedExecutionHandler);

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

        closeExecuterService(elasticSearchPool);
        return pagesResults;
    }

    private boolean createWikiLinksTables() throws SQLException {
        System.out.println("Creating SQL Tables");
        return this.sqlApi.createTable(new WikiLinksMention()) &&
                this.sqlApi.createTable(WikiLinksCoref.getAndSetIfNotExistCorefChain("####TEMP####"));
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

    private SearchResponse createElasticSearchResponse(Scroll scroll) throws IOException {
        final SearchRequest searchRequest = new SearchRequest(this.config.get("elastic_wiki_index"));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchAllQuery());
        searchSourceBuilder.size(Integer.parseInt(this.config.get("elastic_search_interval")));
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

    class ParseAndExtractMentions implements Runnable {
        private List<RawElasticResult> rawElasticResults;
        private ParseListener listener;

        public ParseAndExtractMentions(List<RawElasticResult> rawElasticResults, ParseListener listener) {
            this.rawElasticResults = rawElasticResults;
            this.listener = listener;
        }


        @Override
        public void run() {
            for(RawElasticResult rowResult : this.rawElasticResults) {
                List<WikiLinksMention> wikiLinksMentions = WikiLinksExtractor.extractFromFile(rowResult.getTitle(), rowResult.getText());
                wikiLinksMentions.stream().forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
                this.listener.handle(wikiLinksMentions);
            }
        }
    }
}
