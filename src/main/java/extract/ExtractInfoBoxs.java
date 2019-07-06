package extract;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import wikilinks.CreateWikiLinks;
import workers.ReadWorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ExtractInfoBoxs {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException {
        final String property = System.getProperty("user.dir");
        System.out.println("Working directory=" + property);

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(config.get("sql_connection_url")));
        ElasticQueryApi elasticApi = new ElasticQueryApi(config);

        ReadWorkerFactory readWorkerFactory = new ReadWorkerFactory();
        CreateWikiLinks createWikiLinks = new CreateWikiLinks(sqlApi, elasticApi, config, readWorkerFactory);
        createWikiLinks.readAllWikiPagesAndProcess();
        final Map<String, String> infoBoxes = readWorkerFactory.getInfoBoxes();

        System.out.println(infoBoxes.size());
//        System.out.println(gson.toJson(infoBoxes));

        FileUtils.write(new File(property + "/infoboxes.json"), gson.toJson(infoBoxes), "UTF-8");
    }
}
