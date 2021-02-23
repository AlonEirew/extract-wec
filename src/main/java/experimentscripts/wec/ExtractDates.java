package experimentscripts.wec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import config.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.WECResources;
import utils.ExecutorServiceFactory;
import wec.CreateWEC;
import workers.ReadDateWorker;
import workers.WorkersFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExtractDates {
    private final static Logger LOGGER = LogManager.getLogger(ExtractDates.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException,
            TimeoutException, ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        long start = System.currentTimeMillis();
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Configuration config = GSON.fromJson(new FileReader(property + "/config.json"), Configuration.class);

        ExecutorServiceFactory.initExecutorService();
        WECResources.setElasticApi(new ElasticQueryApi(config));

        try {
            WorkersFactory<List<String>> readDateWorkerFactory = new WorkersFactory(ReadDateWorker.class,
                    Collections.<String>emptyList().getClass(), new ArrayList<>());

            CreateWEC createWEC = new CreateWEC(readDateWorkerFactory);
            createWEC.readAllWikiPagesAndProcess(config.getTotalAmountToExtract());
            final List<String> datesSchemas = readDateWorkerFactory.getResource();

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
