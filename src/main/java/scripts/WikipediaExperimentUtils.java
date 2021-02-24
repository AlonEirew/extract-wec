package scripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import config.WECConfigurations;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import persistence.WECResources;
import utils.ExecutorServiceFactory;
import wec.CreateWEC;
import workers.IWorkerFactory;

import java.io.IOException;

public class WikipediaExperimentUtils {
    private final static Logger LOGGER = LogManager.getLogger(WikipediaExperimentUtils.class);

    public void runWikipediaExperiment(IWorkerFactory workerFactory) throws IOException {
        long start = System.currentTimeMillis();
        ExecutorServiceFactory.initExecutorService(Integer.parseInt(WECConfigurations.getConfig().getPoolSize()));
        WECResources.setElasticApi(new ElasticQueryApi(WECConfigurations.getConfig()));
        WECResources.setSqlApi(new SQLQueryApi(new SQLiteConnections(WECConfigurations.getConfig().getSqlConnectionUrl())));

        try {
            CreateWEC createWEC = new CreateWEC(workerFactory);
            createWEC.readAllWikiPagesAndProcess(WECConfigurations.getConfig().getTotalAmountToExtract());
        } finally {
            ExecutorServiceFactory.closeService();
            WECResources.closeAllResources();
            long end = System.currentTimeMillis();
            LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
        }
    }
}
