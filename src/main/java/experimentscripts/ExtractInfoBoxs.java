package experimentscripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import persistence.ElasticQueryApi;
import wikilinks.CreateWikiLinks;
import workers.ReadInfoBoxWorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ExtractInfoBoxs {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String property = System.getProperty("user.dir");
        System.out.println("Working directory=" + property);

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")));

        ReadInfoBoxWorkerFactory readInfoBoxWorkerFactory = new ReadInfoBoxWorkerFactory();
        CreateWikiLinks createWikiLinks = new CreateWikiLinks(elasticApi, readInfoBoxWorkerFactory);
        createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));
        final Map<String, String> infoBoxes = readInfoBoxWorkerFactory.getInfoBoxes();

        System.out.println(infoBoxes.size());
//        System.out.println(gson.toJson(infoBoxes));

        FileUtils.write(new File(property + "/infoboxes.json"), gson.toJson(infoBoxes), "UTF-8");
    }
}