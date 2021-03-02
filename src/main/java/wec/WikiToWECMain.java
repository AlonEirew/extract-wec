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
import wec.filters.InfoboxFilter;
import wec.persistence.*;
import wec.utils.ExecutorServiceFactory;
import wec.workers.ParseAndExtractMentionsWorker;
import wec.workers.WorkerFactory;

import javax.persistence.EntityManager;
import java.io.IOException;

@SpringBootApplication
public class WikiToWECMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiToWECMain.class);

    @Autowired
    private ExtractWECToJson wecToJson;

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        LOGGER.info("WikiToWECMain process started!");
        SpringApplication.run(WikiToWECMain.class, args).close();
        long end = System.currentTimeMillis();
        LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
    }

    @Bean
    public CommandLineRunner runner(Environment environment, MentionsRepository repository, CorefRepository corefRepository,
                                    ContextRepository contextRepository, EntityManager entityManager) {
        return (args) -> {
            Configuration.initConfiguration(environment);
            WECResources.setEntityManager(entityManager);
            WECResources.setMentionsRepository(repository);
            WECResources.setCorefRepository(corefRepository);
            WECResources.setContextRepository(contextRepository);

            if(args.length > 0) {
                if (args[0].equalsIgnoreCase("wecdb")) {
                    LOGGER.info("Running wec unfiltered generation into");
                    runWecDb();
                } else if(args[0].equalsIgnoreCase("wecjson")) {
                    LOGGER.info("Generating WEC-Lang json");
                    runWecJson();
                } else {
                    LOGGER.info("Nothing happened, argument not applicable");
                }
            } else {
                LOGGER.info("Nothing happened, no argument provided");
            }
        };
    }

    private void runWecDb() {
        WECResources.setElasticApi(new ElasticQueryApi());
        final int pool_size = Configuration.getConfiguration().getPoolSize();
        if (pool_size > 0) {
            ExecutorServiceFactory.initExecutorService(pool_size);
        } else {
            ExecutorServiceFactory.initExecutorService();
        }

        WorkerFactory<InfoboxFilter> workerFactory = new WorkerFactory<>(
                ParseAndExtractMentionsWorker.class, InfoboxFilter.class, new InfoboxFilter(Configuration.getConfiguration().getInfoboxConfiguration()));

        try (ExtractWECToDB extractWECToDB = new ExtractWECToDB(workerFactory)) {
            extractWECToDB.readAllWikiPagesAndProcess(Configuration.getConfiguration().getTotalAmountToExtract());
        } catch (Exception ex) {
            LOGGER.error("Could not start process", ex);
        } finally {
            ExecutorServiceFactory.closeService();
            WECResources.closeAllResources();
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
}
