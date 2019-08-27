package wikilinks;

import com.google.gson.Gson;
import data.WikiLinksCoref;
import data.WikiLinksMention;
import org.apache.commons.io.FileUtils;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import utils.ExecutorServiceFactory;
import workers.ParseAndExtractWorkersFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class WikiToWikiLinksMain {

    private static Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        final String property = System.getProperty("user.dir");
        System.out.println("Working directory=" + property);

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        ExecutorServiceFactory.initExecutorService(Integer.parseInt(config.get("pool_size")));

        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(config.get("sql_connection_url")));

        try (ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")))) {
            CreateWikiLinks createWikiLinks = new CreateWikiLinks(elasticApi, new ParseAndExtractWorkersFactory(sqlApi, elasticApi));

            long start = System.currentTimeMillis();

            if (!createSQLWikiLinksTables(sqlApi)) {
                System.out.println("Failed to create Database and tables, finishing process");
                return;
            }

            createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));
            sqlApi.persistAllCorefs();

            ExecutorServiceFactory.closeService();

            long end = System.currentTimeMillis();
            System.out.println("Process Done, took-" + (end - start) + "ms to run");
        } catch (Exception ex) {
            ex.printStackTrace();
            ExecutorServiceFactory.closeService();
        }
    }

    private static boolean createSQLWikiLinksTables(SQLQueryApi sqlApi) throws SQLException {
        System.out.println("Creating SQL Tables");
        return sqlApi.createTable(new WikiLinksMention()) &&
                sqlApi.createTable(WikiLinksCoref.getAndSetIfNotExistCorefChain("####TEMP####"));
    }
}
