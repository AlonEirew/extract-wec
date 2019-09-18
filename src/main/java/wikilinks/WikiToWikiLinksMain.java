package wikilinks;

import com.google.gson.Gson;
import data.MentionContext;
import data.WikiLinksCoref;
import data.WikiLinksMention;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import utils.ExecutorServiceFactory;
import workers.ParseAndExtractWorkersFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

public class WikiToWikiLinksMain {
    private final static Logger LOGGER = LogManager.getLogger(WikiToWikiLinksMain.class);

    private final static Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Map<String, String> config = GSON.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        final int pool_size = Integer.parseInt(config.get("pool_size"));
        if(pool_size > 0) {
            ExecutorServiceFactory.initExecutorService(pool_size);
        } else {
            ExecutorServiceFactory.initExecutorService();
        }

        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(config.get("sql_connection_url")));
        long start = System.currentTimeMillis();
        ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")));
        try {
            CreateWikiLinks createWikiLinks = new CreateWikiLinks(elasticApi, new ParseAndExtractWorkersFactory(sqlApi, elasticApi));

            if (!createSQLWikiLinksTables(sqlApi)) {
                LOGGER.error("Failed to create Database and tables, finishing process");
                return;
            }

            createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));
        } catch (Exception ex) {
            LOGGER.error(ex);
        } finally {
            elasticApi.close();
            sqlApi.persistAllCorefs();
            sqlApi.persistAllContexts();
            ExecutorServiceFactory.closeService();

            long end = System.currentTimeMillis();
            LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
        }
    }

    private static boolean createSQLWikiLinksTables(SQLQueryApi sqlApi) throws SQLException {
        LOGGER.info("Creating SQL Tables");
        return sqlApi.createTable(new WikiLinksMention()) &&
                sqlApi.createTable(WikiLinksCoref.getAndSetIfNotExist("####TEMP####")) &&
                sqlApi.createTable(new MentionContext(new ArrayList<>()));
    }
}
