package wec;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import wec.config.WECConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import wec.data.WECContext;
import wec.data.WECCoref;
import wec.data.WECMention;
import wec.persistence.ElasticQueryApi;
import wec.persistence.WecRepository;
import wec.persistence.WECResources;
import wec.utils.ExecutorServiceFactory;
import wec.filters.InfoboxFilter;
import wec.workers.ParseAndExtractMentionsWorker;
import wec.workers.WorkerFactory;

import java.io.IOException;

@SpringBootApplication
public class WikiToWECMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiToWECMain.class);

    public static void main(String[] args) throws IOException {
        LOGGER.info("WikiToWECMain process started!");
        SpringApplication.run(WikiToWECMain.class, args);
    }

    @Bean
    public CommandLineRunner runner(WecRepository repository) {
        return (args) -> {
            LOGGER.info("Runner start...");
            WECResources.setElasticApi(new ElasticQueryApi(WECConfigurations.getConfig()));
            WECResources.setWECRepository(repository);
            final int pool_size = WECConfigurations.getConfig().getPoolSize();
            if (pool_size > 0) {
                ExecutorServiceFactory.initExecutorService(pool_size);
            } else {
                ExecutorServiceFactory.initExecutorService();
            }

            long start = System.currentTimeMillis();
            WorkerFactory<InfoboxFilter> workerFactory = new WorkerFactory<>(
                    ParseAndExtractMentionsWorker.class, InfoboxFilter.class, new InfoboxFilter(WECConfigurations.getInfoboxConf()));

            try (CreateWEC createWEC = new CreateWEC(workerFactory)) {
                createWEC.readAllWikiPagesAndProcess(WECConfigurations.getConfig().getTotalAmountToExtract());
            } catch (Exception ex) {
                LOGGER.error("Could not start process", ex);
            } finally {
                ExecutorServiceFactory.closeService();
                WECResources.closeAllResources();
                long end = System.currentTimeMillis();
                LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
            }
        };
    }
}
