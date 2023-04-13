package wec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import wec.config.Configuration;
import wec.persistence.DBRepository;
import wec.persistence.ElasticQueryApi;
import wec.persistence.WECResources;
import wec.workers.ParseAndExtractMentionsWorker;
import wec.workers.WorkerFactory;

import java.io.IOException;
import java.util.Arrays;

@SpringBootApplication
public class WikiToWECMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiToWECMain.class);

    @Autowired
    private ExtractWECToJson wecToJson;

    @Autowired
    private ExtractInfoboxs extractInfoboxs;

    @Autowired
    private ExtractNegFirstPassages extractNegFirstPassages;

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        LOGGER.info("WikiToWECMain process started!");
        LOGGER.info("Running with args: " + Arrays.toString(args));
        SpringApplication.run(WikiToWECMain.class, args).close();
        long end = System.currentTimeMillis();
        LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
    }

    @Bean
    public CommandLineRunner runner(Environment environment, DBRepository dbRepository) {
        return (args) -> {
            Configuration.initConfiguration(environment);
            WECResources.setDbRepository(dbRepository);

            if(args.length > 0) {
                if (args[0].equalsIgnoreCase("wecdb")) {
                    LOGGER.info("Running wec unfiltered generation into");
                    runWecDb();
                } else if(args[0].equalsIgnoreCase("wecjson")) {
                    LOGGER.info("Generating WEC-Lang json");
                    runWecJson();
                } else if(args[0].equalsIgnoreCase("infobox")) {
                    LOGGER.info("Running infobox script");
                    runInfoboxScript(args);
                } else if(args[0].equalsIgnoreCase("first")){
                    LOGGER.info("Running first passage script");
                    runFirstPassageScript();
                }
                else {
                    LOGGER.info("Nothing happened, argument not applicable");
                }
            } else {
                LOGGER.info("Nothing happened, no argument provided");
            }
        };
    }

    private void runWecDb() {
        WECResources.setElasticApi(new ElasticQueryApi());
        WorkerFactory workerFactory = new WorkerFactory(ParseAndExtractMentionsWorker.class);

        ExtractWECToDB extractWECToDB = new ExtractWECToDB(workerFactory);
        try {
            extractWECToDB.readAllWikiPagesAndProcess();
        } catch (Exception ex) {
            LOGGER.error("Could not start process", ex);
        } finally {
            WECResources.closeAllResources();
            extractWECToDB.close();
            workerFactory.close();
        }
    }

    private void runWecJson() {
        LOGGER.info("Starting process to generate WEC dataset json");
        try {
            this.wecToJson.generateJson();
        } catch (IOException e) {
            LOGGER.error("Failed to generate Json!", e);
        } finally {
            WECResources.closeAllResources();
        }
    }

    private void runInfoboxScript(String[] args) {
        LOGGER.info("Starting process to extract the infobox of each article");
        try {
            this.extractInfoboxs.runInfoboxScript(args);
        } catch (IOException e) {
            LOGGER.error("Failed to generate Json!", e);
        } finally {
            WECResources.closeAllResources();
        }
    }

    private void runFirstPassageScript() {
        LOGGER.info("Starting process to extract the first passage of each article");
        try {
            this.extractNegFirstPassages.runFirstPassageScript();
        } catch (IOException e) {
            LOGGER.error("Failed to generate Json!", e);
        } finally {
            WECResources.closeAllResources();
        }
    }
}
