package scripts;

import wec.ExtractWECToDB;
import wec.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wec.persistence.ElasticQueryApi;
import wec.persistence.WECResources;
import wec.utils.ExecutorServiceFactory;
import wec.workers.IWorkerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class WikipediaExperimentUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaExperimentUtils.class);

    public void runWikipediaExperiment(IWorkerFactory workerFactory) throws IOException {
        long start = System.currentTimeMillis();
        WECResources.setElasticApi(new ElasticQueryApi());

        try {
            ExtractWECToDB extractWECToDB = new ExtractWECToDB(workerFactory);
            extractWECToDB.readAllWikiPagesAndProcess();
        } finally {
            WECResources.closeAllResources();
            long end = System.currentTimeMillis();
            LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
        }
    }
}
