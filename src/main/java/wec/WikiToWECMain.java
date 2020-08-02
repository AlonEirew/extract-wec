package wec;

import com.google.gson.Gson;
import data.Configuration;
import data.InfoboxConfiguration;
import data.WECCoref;
import data.WECMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import utils.ExecutorServiceFactory;
import workers.ParseAndExtractWorkersFactory;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

public class WikiToWECMain {
    private final static Logger LOGGER = LogManager.getLogger(WikiToWECMain.class);

    private final static Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Configuration config = GSON.fromJson(new FileReader(property + "/config.json"), Configuration.class);
        InfoboxConfiguration infoboxConfiguration = GSON.fromJson(new FileReader(
                property + config.getInfoboxConfiguration()), InfoboxConfiguration.class);

        infoboxConfiguration.getInfoboxConfigs().get(10).getExtractor();

        final int pool_size = Integer.parseInt(config.getPoolSize());
        if(pool_size > 0) {
            ExecutorServiceFactory.initExecutorService(pool_size);
        } else {
            ExecutorServiceFactory.initExecutorService();
        }

        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(config.getSqlConnectionUrl()));
        long start = System.currentTimeMillis();
        try (ElasticQueryApi elasticApi = new ElasticQueryApi(config)) {
            CreateWEC createWEC = new CreateWEC(elasticApi, new ParseAndExtractWorkersFactory(sqlApi, elasticApi,
                    new InfoboxFilter(infoboxConfiguration)));

            if (!createSQLWikiLinksTables(sqlApi)) {
                LOGGER.error("Failed to create Database and tables, finishing process");
                return;
            }

            createWEC.readAllWikiPagesAndProcess(config.getTotalAmountToExtract());
        } catch (Exception ex) {
            LOGGER.error("Could not start process", ex);
        } finally {
            ExecutorServiceFactory.closeService();
            sqlApi.persistAllMentions();
            sqlApi.persistAllCorefs();

            long end = System.currentTimeMillis();
            LOGGER.info("Process Done, took-" + (end - start) + "ms to run");
        }
    }

    private static boolean createSQLWikiLinksTables(SQLQueryApi sqlApi) throws SQLException {
        LOGGER.info("Creating SQL Tables");
        return sqlApi.createTable(new WECMention()) &&
                sqlApi.createTable(WECCoref.getAndSetIfNotExist("####TEMP####"));
    }
}