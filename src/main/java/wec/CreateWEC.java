package wec;

import data.RawElasticResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

public class CreateWEC {
    private final static Logger LOGGER = LogManager.getLogger(CreateWEC.class);

    private final IWorkerFactory workerFactory;
    private final ElasticQueryApi elasticApi;

    public CreateWEC(ElasticQueryApi elasticApi, IWorkerFactory workerFactory) {
        this.elasticApi = elasticApi;
        this.workerFactory = workerFactory;
    }

    public void readAllWikiPagesAndProcess(int totalAmountToExtract) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        LOGGER.info("Strating process, Reading all documents from wikipedia (elastic)");

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
            LOGGER.info((totalDocsCount - count) + " documents to go");

            if(count >= totalAmountToExtract) {
                break;
            }

            count += searchHits.length;
            searchHits = searchResponse.getHits().getHits();
        }

        LOGGER.info("Handling last mentions if exists");
        for (Future<?> future : allTasks) {
            future.get(10, TimeUnit.MINUTES);
        }

        this.workerFactory.finalizeIfNeeded();
        elasticApi.closeScroll(scrollId);
    }
}
