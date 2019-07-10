package wikinews;

import com.google.gson.Gson;
import data.WikiLinksCoref;
import data.WikiNewsMention;
import org.apache.commons.io.FileUtils;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import utils.ExecutorServiceFactory;
import wikilinks.CreateWikiLinks;
import workers.WikiNewsWorkerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class WikiNewsToWikiLinksMain {
    private static Gson gson = new Gson();

    public static void main(String[] args) throws IOException, SQLException, InterruptedException, ExecutionException, TimeoutException {
        final String property = System.getProperty("user.dir");
        System.out.println("Working directory=" + property);

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        ExecutorServiceFactory.initExecutorService(Integer.parseInt(config.get("pool_size")));

        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(config.get("sql_connection_url")));
        ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wikinews_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")));

        sqlApi.createTable(new WikiNewsMention());

        final Map<String, WikiLinksCoref> wikiLinksCorefMap = sqlApi.readCorefTableToMap();

        CreateWikiLinks createWikiLinks = new CreateWikiLinks(sqlApi, elasticApi,
                new WikiNewsWorkerFactory(wikiLinksCorefMap, sqlApi));

        createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));


        ExecutorServiceFactory.closeService();
        elasticApi.closeElasticQueryApi();
    }
}
