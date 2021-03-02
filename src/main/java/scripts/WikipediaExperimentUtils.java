package scripts;

import wec.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wec.persistence.ElasticQueryApi;
import wec.persistence.WECResources;
import wec.utils.ExecutorServiceFactory;
import wec.CreateWEC;
import wec.workers.IWorkerFactory;

import java.io.IOException;

public class WikipediaExperimentUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaExperimentUtils.class);

    public void runWikipediaExperiment(IWorkerFactory workerFactory) throws IOException {
        long start = System.currentTimeMillis();
        ExecutorServiceFactory.initExecutorService(Configuration.getConfiguration().getPoolSize());
        WECResources.setElasticApi(new ElasticQueryApi());

        try {
            CreateWEC createWEC = new CreateWEC(workerFactory);
            createWEC.readAllWikiPagesAndProcess(Configuration.getConfiguration().getTotalAmountToExtract());
        } finally {
            ExecutorServiceFactory.closeService();
            WECResources.closeAllResources();
            long end = System.currentTimeMillis();
            LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
        }
    }
}
