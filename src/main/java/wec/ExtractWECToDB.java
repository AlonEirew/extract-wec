package wec;

import wec.config.Configuration;
import wec.data.RawElasticResult;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wec.data.WECCoref;
import wec.persistence.ElasticQueryApi;
import wec.persistence.WECResources;
import wec.utils.ExecutorServiceFactory;
import wec.workers.IWorkerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ExtractWECToDB {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractWECToDB.class);

    private final IWorkerFactory workerFactory;

    public ExtractWECToDB(IWorkerFactory workerFactory) {
        this.workerFactory = workerFactory;
    }

    public void readAllWikiPagesAndProcess() throws IOException {
        int totalAmountToExtract = Configuration.getConfiguration().getTotalAmountToExtract();
        ElasticQueryApi elasticApi = WECResources.getElasticApi();
        final int pool_size = Configuration.getConfiguration().getPoolSize();
        ExecutorService executorService = ExecutorServiceFactory.getExecutorService(pool_size);
        LOGGER.info("Starting process, preparing to read " + totalAmountToExtract + " documents from wikipedia (elastic)");

        long totalDocsCount = elasticApi.getTotalDocsCount();
        final Scroll scroll = new Scroll(TimeValue.timeValueHours(5L));
        SearchResponse searchResponse = elasticApi.createElasticSearchResponse(scroll);

        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        int count = 0;
        while (searchHits != null && searchHits.length > 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = elasticApi.getSearchScroll(scrollRequest);
            scrollId = searchResponse.getScrollId();

            List<RawElasticResult> rawElasticResults = elasticApi.getNextScrollResults(searchHits);
            executorService.execute(this.workerFactory.borrowNewWorker(rawElasticResults));
            LOGGER.info((totalDocsCount - count) + " documents to go");

            if(totalAmountToExtract > 0 && count >= totalAmountToExtract) {
                break;
            }

            count += searchHits.length;
            searchHits = searchResponse.getHits().getHits();
        }

        LOGGER.info("Handling last mentions if exists");
        ExecutorServiceFactory.closeService(executorService);
        this.workerFactory.close();
        elasticApi.closeScroll(scrollId);
    }

    public void close() {
        Collection<WECCoref> corefs = WECCoref.getGlobalCorefMap().values();
        WECResources.getDbRepository().saveCorefAndMentions(corefs);
    }
}
