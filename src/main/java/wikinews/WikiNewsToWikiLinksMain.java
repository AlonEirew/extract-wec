package wikinews;

import com.google.gson.Gson;
import data.WikiNewsMention;
import org.apache.commons.io.FileUtils;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class WikiNewsToWikiLinksMain {
    private static Gson gson = new Gson();

    public static void main(String[] args) throws IOException, SQLException {
        final String property = System.getProperty("user.dir");
        System.out.println("Working directory=" + property);

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(config.get("sql_connection_url")));
        ElasticQueryApi elasticApi = new ElasticQueryApi(config);

        sqlApi.createTable(new WikiNewsMention());

        elasticApi.closeElasticQueryApi();
    }

    private static Map<Integer, String> readCorefChains() {
        return null;
    }
}
