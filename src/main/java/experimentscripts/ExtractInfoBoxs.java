package experimentscripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import persistence.ElasticQueryApi;
import utils.ExecutorServiceFactory;
import wikilinks.CreateWikiLinks;
import workers.ReadInfoBoxWorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ExtractInfoBoxs {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String property = System.getProperty("user.dir");
        System.out.println("Working directory=" + property);

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        ReadInfoBoxWorkerFactory readInfoBoxWorkerFactory = new ReadInfoBoxWorkerFactory();
        try (ElasticQueryApi elasticApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")))) {

            ExecutorServiceFactory.initExecutorService(Integer.parseInt(config.get("pool_size")));

            CreateWikiLinks createWikiLinks = new CreateWikiLinks(elasticApi, readInfoBoxWorkerFactory);
            createWikiLinks.readAllWikiPagesAndProcess(Integer.parseInt(config.get("total_amount_to_extract")));

            ExecutorServiceFactory.closeService();
        } catch (Exception ex) {
            ex.printStackTrace();
            ExecutorServiceFactory.closeService();
        }

        final Map<String, Set<String>> infoBoxes = readInfoBoxWorkerFactory.getInfoBoxes();

        List<Map.Entry<String, Integer>> sortedInfoBoxesBySize = new ArrayList<>();
        for (Map.Entry<String, Set<String>> ent : infoBoxes.entrySet()) {
            AbstractMap.SimpleEntry<String, Integer> newEnt = new AbstractMap.SimpleEntry<>(ent.getKey(), ent.getValue().size());
            sortedInfoBoxesBySize.add(newEnt);
        }

        sortedInfoBoxesBySize.sort(Map.Entry.comparingByValue(Collections.reverseOrder()));

            System.out.println(infoBoxes.size());
//        System.out.println(gson.toJson(infoBoxes));

            FileUtils.write(new File(property + "/infoboxesBySize.json"), gson.toJson(sortedInfoBoxesBySize), "UTF-8");

    }
}
