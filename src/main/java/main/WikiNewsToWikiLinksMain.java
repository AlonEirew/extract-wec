package main;

import com.google.gson.Gson;
import data.WECCoref;
import data.WikiNewsMention;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import wec.CreateWEC;
import workers.WikiNewsWorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WikiNewsToWikiLinksMain {
    private final static Logger LOGGER = LogManager.getLogger(WikiNewsToWikiLinksMain.class);
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

            SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(config.get("sql_connection_url")));

            sqlApi.createTable(new WikiNewsMention());
            final List<WECCoref> WECCorefMap = sqlApi.readTable(WECCoref.TABLE_COREF,
                    new WECCoref("NA"));

            final Map<String, WECCoref> corefMap = new HashMap<>();
            for(WECCoref coref : WECCorefMap) {
                corefMap.put(coref.getCorefValue(), coref);
            }

            CreateWEC createWEC = new CreateWEC(new WikiNewsWorkerFactory(corefMap));

            createWEC.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }
}
