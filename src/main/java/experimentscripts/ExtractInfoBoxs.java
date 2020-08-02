package experimentscripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import data.Configuration;
import wec.CreateWEC;
import workers.ReadInfoBoxWorkerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ExtractInfoBoxs {
    private final static Logger LOGGER = LogManager.getLogger(ExtractInfoBoxs.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws IOException {
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Configuration config = GSON.fromJson(new FileReader(property + "/config.json"), Configuration.class);

        ReadInfoBoxWorkerFactory readInfoBoxWorkerFactory = new ReadInfoBoxWorkerFactory();
        try (ElasticQueryApi elasticApi = new ElasticQueryApi(config)) {

            CreateWEC createWEC = new CreateWEC(elasticApi, readInfoBoxWorkerFactory);
            createWEC.readAllWikiPagesAndProcess(config.getTotalAmountToExtract());
        } catch (Exception ex) {
            LOGGER.error(ex);
        }

        final Map<String, Set<String>> infoBoxes = readInfoBoxWorkerFactory.getInfoBoxes();

        List<Map.Entry<String, Integer>> sortedInfoBoxesBySize = new ArrayList<>();
        for (Map.Entry<String, Set<String>> ent : infoBoxes.entrySet()) {
            AbstractMap.SimpleEntry<String, Integer> newEnt = new AbstractMap.SimpleEntry<>(ent.getKey(), ent.getValue().size());
            sortedInfoBoxesBySize.add(newEnt);
        }

        sortedInfoBoxesBySize.sort(Map.Entry.comparingByValue(Collections.reverseOrder()));

            LOGGER.info(infoBoxes.size());
//        LOGGER.info(gson.toJson(infoBoxes));

            FileUtils.write(new File(property + "/infoboxesBySize.json"), GSON.toJson(sortedInfoBoxesBySize), "UTF-8");

    }
}
