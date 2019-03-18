package persistence;

import javafx.util.Pair;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import wikilinks.WikiLinksMention;
import wikilinks.WikilinksExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ElasticFullDataReader {

    private static final int INTERVAL = 1000;
    private static final int EXTRACT_AMOUNT = 10000;
    private static final int TOTAL_DOCS = 18289732;

    public List<WikiLinksMention> readAll(String host, int port, String scheme, String index) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        System.out.println("Strating process, Reading all documents from wikipedia (elastic)");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
        final SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchAllQuery());
        searchSourceBuilder.size(INTERVAL);
        searchRequest.source(searchSourceBuilder);

        final Scroll scroll = new Scroll(TimeValue.timeValueHours(5L));
        searchRequest.scroll(scroll);
        SearchResponse searchResponse = client.search(searchRequest);

        List<Future<List<WikiLinksMention>>> returnFutures = new ArrayList<>();

        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        int count = 0;
        while (searchHits != null && searchHits.length > 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = client.searchScroll(scrollRequest);
            scrollId = searchResponse.getScrollId();

            List<Pair<String, String>> pageTexts = new ArrayList<>();
            for (SearchHit hit : searchHits) {
                final Map map = hit.getSourceAsMap();
                final String text = (String)map.get("text");
                final String title = (String)map.get("title");
                final Map relations = (Map)map.get("relations");

                if((boolean)relations.get("isDisambiguation") ||
                        text.startsWith("#redirect") ||
                        text.startsWith("#REDIRECT") ||
                        title.toLowerCase().startsWith("file:") ||
                        title.toLowerCase().startsWith("wikipedia:")) {
                    continue;
                }

                pageTexts.add(new Pair<>(title, text));
            }

            returnFutures.add(pool.submit(new ParseDataRunner(pageTexts)));

            System.out.println((TOTAL_DOCS - count) + " documents to go");

            count += searchHits.length;
            searchHits = searchResponse.getHits().getHits();
        }

        System.out.println("Done going over all wikipedia documents");
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest);
        if(clearScrollResponse.isSucceeded()) {
            System.out.println("Done, Scroll closed!");
        }

        pool.shutdown(); // Disable new tasks from being submitted
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

        List<WikiLinksMention> mentions = getMentionsList(returnFutures);

        return mentions;
    }

    private List<WikiLinksMention> getMentionsList(List<Future<List<WikiLinksMention>>> returnFutures) throws InterruptedException, ExecutionException, TimeoutException {
        List<WikiLinksMention> mentions = new ArrayList<>();
        if(returnFutures != null) {
            for (Future<List<WikiLinksMention>> future : returnFutures) {
                mentions.addAll(future.get(10, TimeUnit.SECONDS));
            }
        }
        return mentions;
    }

    class ParseDataRunner implements Callable<List<WikiLinksMention>> {

        private List<Pair<String, String>> texts;

        public ParseDataRunner(List<Pair<String, String>> texts) {
            this.texts = texts;
        }


        @Override
        public List<WikiLinksMention> call() throws Exception {
            List<WikiLinksMention> extractedWikiMentions = new ArrayList<>();
            for(Pair<String, String> pair : this.texts) {
                extractedWikiMentions.addAll(WikilinksExtractor.extractFromFile(pair.getKey(), pair.getValue()));
            }

            return extractedWikiMentions;
        }
    }
}
