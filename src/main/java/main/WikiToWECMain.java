package main;

import com.google.gson.Gson;
import config.Configuration;
import config.InfoboxConfiguration;
import data.WECCoref;
import data.WECMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import persistence.WECResources;
import utils.ExecutorServiceFactory;
import wec.CreateWEC;
import wec.InfoboxFilter;
import workers.ParseAndExtractMentionsWorker;
import workers.WorkersFactory;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

public class WikiToWECMain {
    private final static Logger LOGGER = LogManager.getLogger(WikiToWECMain.class);
    private final static Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {
        LOGGER.info("WikiToWECMain process started!");
        Configuration config = GSON.fromJson(new FileReader("config.json"), Configuration.class);
        InfoboxConfiguration infoboxConf = GSON.fromJson(new FileReader(
                config.getInfoboxConfiguration()), InfoboxConfiguration.class);

        WECResources.setSqlApi(new SQLQueryApi(new SQLiteConnections(config.getSqlConnectionUrl())));
        WECResources.setElasticApi(new ElasticQueryApi(config));

        final int pool_size = Integer.parseInt(config.getPoolSize());
        if(pool_size > 0) {
            ExecutorServiceFactory.initExecutorService(pool_size);
        } else {
            ExecutorServiceFactory.initExecutorService();
        }

        long start = System.currentTimeMillis();
        WorkersFactory<InfoboxFilter> workerFactory = new WorkersFactory<>(
                ParseAndExtractMentionsWorker.class, InfoboxFilter.class, new InfoboxFilter(infoboxConf));

        try (CreateWEC createWEC = new CreateWEC(workerFactory)) {

            if (!createSQLWECTables()) {
                LOGGER.error("Failed to create Database and tables, finishing process");
                return;
            }

            createWEC.readAllWikiPagesAndProcess(config.getTotalAmountToExtract());
        } catch (Exception ex) {
            LOGGER.error("Could not start process", ex);
        } finally {
            ExecutorServiceFactory.closeService();
            WECResources.closeAllResources();
            long end = System.currentTimeMillis();
            LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
        }
    }

    private static boolean createSQLWECTables() throws SQLException {
        LOGGER.info("Creating SQL Tables");
        SQLQueryApi sqlApi = WECResources.getSqlApi();
        return sqlApi.createTable(new WECMention()) &&
                sqlApi.createTable(WECCoref.getAndSetIfNotExist("####TEMP####"));
    }
}
