package experimentscripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
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
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Map<String, String> config = GSON.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(config.get("sql_connection_url")));
        ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")));

        ReadDateWorkerFactory readDateWorkerFactory = new ReadDateWorkerFactory();
        CreateWikiLinks createWikiLinks = new CreateWikiLinks(elasticApi, readDateWorkerFactory);
        createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));
        final List<String> datesSchemas = readDateWorkerFactory.getDatesSchemas();

        LOGGER.info(datesSchemas.size());
//        LOGGER.info(gson.toJson(infoBoxes));

        FileUtils.writeLines(new File(property + "/dates.json"), datesSchemas, "\n");
    }
}
