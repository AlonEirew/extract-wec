package wikilinks;

import data.RawElasticResult;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import persistence.ElasticQueryApi;
import utils.ExecutorServiceFactory;
import workers.IWorkerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CreateWikiLinks {

    private final IWorkerFactory workerFactory;
    private final ElasticQueryApi elasticApi;

    public CreateWikiLinks(ElasticQueryApi elasticApi, IWorkerFactory workerFactory) {
        this.elasticApi = elasticApi;
        this.workerFactory = workerFactory;
    }

    public void readAllWikiPagesAndProcess(int totalAmountToExtract) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        System.out.println("Strating process, Reading all documents from wikipedia (elastic)");

        List<Future<?>> allTasks = new ArrayList<>();

        long totalDocsCount = this.elasticApi.getTotalDocsCount();

        final Scroll scroll = new Scroll(TimeValue.timeValueHours(5L));
        SearchResponse searchResponse = this.elasticApi.createElasticSearchResponse(scroll);

        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        int count = 0;
        while (searchHits != null && searchHits.length > 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = elasticApi.getSearchScroll(scrollRequest);
            scrollId = searchResponse.getScrollId();

            List<RawElasticResult> rawElasticResults = this.elasticApi.getNextScrollResults(searchHits);
            allTasks.add(ExecutorServiceFactory.submit(this.workerFactory.createNewWorker(rawElasticResults)));
            System.out.println((totalDocsCount - count) + " documents to go");

            if(count >= totalAmountToExtract) {
                break;
            }

            count += searchHits.length;
            searchHits = searchResponse.getHits().getHits();
        }


        elasticApi.closeScroll(scrollId);
        System.out.println("Handling last mentions if exists");

        for (Future<?> future : allTasks) {
            future.get(1000, TimeUnit.SECONDS);
        }

        this.workerFactory.finalizeIfNeeded();
    }
}
