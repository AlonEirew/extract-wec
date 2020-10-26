package experimentscripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import utils.ExecutorServiceFactory;
import data.Configuration;
import wec.CreateWEC;
import workers.ReadDateWorkerFactory;
import workers.WECResources;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExtractDates {
    private final static Logger LOGGER = LogManager.getLogger(ExtractDates.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        long start = System.currentTimeMillis();
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Configuration config = GSON.fromJson(new FileReader(property + "/config.json"), Configuration.class);

        ExecutorServiceFactory.initExecutorService();
        WECResources.setElasticApi(new ElasticQueryApi(config));

        try {
            ReadDateWorkerFactory readDateWorkerFactory = new ReadDateWorkerFactory();
            CreateWEC createWEC = new CreateWEC(readDateWorkerFactory);
            createWEC.readAllWikiPagesAndProcess(config.getTotalAmountToExtract());
            final List<String> datesSchemas = readDateWorkerFactory.getDatesSchemas();

            LOGGER.info(datesSchemas.size());
//        LOGGER.info(gson.toJson(infoBoxes));

            FileUtils.writeLines(new File(property + "/output/datesParse.txt"), datesSchemas, "\n");
        } finally {
            ExecutorServiceFactory.closeService();
            long end = System.currentTimeMillis();
            LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
        }
    }
}
