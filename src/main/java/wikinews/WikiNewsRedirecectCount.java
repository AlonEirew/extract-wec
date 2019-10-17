package wikinews;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import wikilinks.CreateWikiLinks;
import workers.WikiNewsRedirectCounterWorker;
import workers.WikiNewsRedirectCounterWorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class WikiNewsRedirecectCount {
    private final static Logger LOGGER = LogManager.getLogger(WikiNewsRedirecectCount.class);
    private final static Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {

        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Map<String, String> config = GSON.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        try (ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wikinews_index"),
                Integer.parseInt(config.get("elastic_search_interval")),
                Integer.parseInt(config.get("multi_request_interval")),
                config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")))) {

            CreateWikiLinks createWikiLinks = new CreateWikiLinks(elasticApi,
                    new WikiNewsRedirectCounterWorkerFactory());

            createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));

            LOGGER.info("Total redirect pages=" + WikiNewsRedirectCounterWorker.getCounter());
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }
}