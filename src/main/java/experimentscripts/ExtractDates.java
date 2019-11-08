package experimentscripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import utils.ExecutorServiceFactory;
import wikilinks.CreateWikiLinks;
import workers.ReadDateWorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExtractDates {
    private final static Logger LOGGER = LogManager.getLogger(ExtractDates.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        long start = System.currentTimeMillis();
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Map<String, String> config = GSON.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        ExecutorServiceFactory.initExecutorService();
        ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")),
                Integer.parseInt(config.get("multi_request_interval")),
                config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")));

        try {
            ReadDateWorkerFactory readDateWorkerFactory = new ReadDateWorkerFactory();
            CreateWikiLinks createWikiLinks = new CreateWikiLinks(elasticApi, readDateWorkerFactory);
            createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));
            final List<String> datesSchemas = readDateWorkerFactory.getDatesSchemas();

            LOGGER.info(datesSchemas.size());
//        LOGGER.info(gson.toJson(infoBoxes));

            FileUtils.writeLines(new File(property + "/output/datesParse.txt"), datesSchemas, "\n");
        } finally {
            elasticApi.close();
            ExecutorServiceFactory.closeService();
            long end = System.currentTimeMillis();
            LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
        }
    }
}
