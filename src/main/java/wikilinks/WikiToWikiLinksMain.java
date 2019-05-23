package wikilinks;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class WikiToWikiLinksMain {

    private static Gson gson = new Gson();

    public static void main(String[] args) throws IOException, SQLException {
        final String property = System.getProperty("user.dir");
        System.out.println("Working directory=" + property);

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        CreateWikiLinks createWikiLinks = new CreateWikiLinks(
                new SQLQueryApi(new SQLiteConnections(config.get("sql_connection_url"))), config);

        long start = System.currentTimeMillis();
        createWikiLinks.readAllAndPerisist();
        long end = System.currentTimeMillis();
        System.out.println("Process Done, took-" + (end - start) + "ms to run");
    }
}
