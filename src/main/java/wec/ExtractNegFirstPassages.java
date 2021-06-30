package wec;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import wec.config.Configuration;
import wec.data.WECJsonObj;
import wec.filters.ByCorefFilter;
import wec.persistence.DBRepository;
import wec.persistence.ElasticQueryApi;
import wec.persistence.WECResources;
import wec.utils.ExecutorServiceFactory;
import wec.workers.ExtractSearchNegativeExamplesWorker;
import wec.workers.WorkerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
public class ExtractNegFirstPassages {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractNegFirstPassages.class);

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        LOGGER.info("WikiToWECMain process started!");
        SpringApplication.run(ExtractNegFirstPassages.class, args).close();
        long end = System.currentTimeMillis();
        LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
    }

    @Bean
    public CommandLineRunner runner(Environment environment) {
        return (args) -> {
            Configuration.initConfiguration(environment);
            WECResources.setElasticApi(new ElasticQueryApi());

            List<WECJsonObj> allMentions = new ArrayList<>();
            allMentions.addAll(Objects.requireNonNull(readWECJsonFile(new File("input/Dev_Event_gold_mentions_validated.json"))));
            allMentions.addAll(Objects.requireNonNull(readWECJsonFile(new File("input/Test_Event_gold_mentions_validated.json"))));
            Set<String> corefLinks = allMentions.stream().map(WECJsonObj::getCoref_link).collect(Collectors.toSet());
            ByCorefFilter.setCorefToFilter(corefLinks);

            Files.deleteIfExists(Path.of("input/Negative_First_Passages.txt"));
            WorkerFactory workerFactory = new WorkerFactory(ExtractSearchNegativeExamplesWorker.class);
            ExtractWECToDB extractWECToDB = new ExtractWECToDB(workerFactory);

            try {
                extractWECToDB.readAllWikiPagesAndProcess();
            } catch (Exception ex) {
                LOGGER.error("Could not start process", ex);
            } finally {
                WECResources.closeAllResources();
                ExtractSearchNegativeExamplesWorker.close();
            }

            LOGGER.info("Total first passages extracted=" + ExtractSearchNegativeExamplesWorker.getTotalPassages());
            LOGGER.info("Process Done!");
        };
    }

    public static List<WECJsonObj> readWECJsonFile(File wecInput) {
        try (InputStream in = new FileInputStream(wecInput)) {
            Type MENTION_TYPE = new TypeToken<List<WECJsonObj>>() {
            }.getType();
            JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return Configuration.GSON.fromJson(reader, MENTION_TYPE);
        } catch (IOException ex) {
            LOGGER.error("Failed to read mention file", ex);
            return null;
        }
    }
}
