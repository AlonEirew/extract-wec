package main;

import config.WECConfigurations;
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
import workers.WorkerFactory;

import java.io.IOException;
import java.sql.SQLException;

public class WikiToWECMain {
    private final static Logger LOGGER = LogManager.getLogger(WikiToWECMain.class);

    public static void main(String[] args) throws IOException {
        LOGGER.info("WikiToWECMain process started!");

        WECResources.setSqlApi(new SQLQueryApi(new SQLiteConnections(WECConfigurations.getConfig().getSqlConnectionUrl())));
        WECResources.setElasticApi(new ElasticQueryApi(WECConfigurations.getConfig()));

        final int pool_size = WECConfigurations.getConfig().getPoolSize();
        if(pool_size > 0) {
            ExecutorServiceFactory.initExecutorService(pool_size);
        } else {
            ExecutorServiceFactory.initExecutorService();
        }

        long start = System.currentTimeMillis();
        WorkerFactory<InfoboxFilter> workerFactory = new WorkerFactory<>(
                ParseAndExtractMentionsWorker.class, InfoboxFilter.class, new InfoboxFilter(WECConfigurations.getInfoboxConf()));

        try (CreateWEC createWEC = new CreateWEC(workerFactory)) {

            if (!createSQLWECTables()) {
                LOGGER.error("Failed to create Database and tables, finishing process");
                return;
            }

            createWEC.readAllWikiPagesAndProcess(WECConfigurations.getConfig().getTotalAmountToExtract());
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
