package wikinews;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import persistence.ElasticQueryApi;
import utils.ExecutorServiceFactory;
import wikilinks.CreateWikiLinks;
import workers.WikiNewsRedirectCounterWorker;
import workers.WikiNewsRedirectCounterWorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class WikiNewsRedirecectCount {

    private static Gson gson = new Gson();

    public static void main(String[] args) throws IOException {

        final String property = System.getProperty("user.dir");
        System.out.println("Working directory=" + property);

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        try (ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wikinews_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")))) {

            CreateWikiLinks createWikiLinks = new CreateWikiLinks(elasticApi,
                    new WikiNewsRedirectCounterWorkerFactory());

            createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));

            System.out.println("Total redirect pages=" + WikiNewsRedirectCounterWorker.getCounter());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}