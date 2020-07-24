package wec;

import com.google.gson.Gson;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WikiToWECMain {
    private final static Logger LOGGER = LogManager.getLogger(WikiToWECMain.class);

    private final static Gson GSON = new Gson();

    public static void main(String[] args) throws IOException {
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Configuration config = GSON.fromJson(new FileReader(property + "/config.json"), Configuration.class);

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
                    getPersonOrEventFilter(config.getUseExtractors())));

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

    public static PersonOrEventFilter getPersonOrEventFilter(List<String> extractorClasses) throws ClassNotFoundException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        List<AInfoboxExtractor> extractors = new ArrayList<>();

        for(String className : extractorClasses) {
            Constructor<?>[] constructors = Class.forName(className).getConstructors();
            AInfoboxExtractor extractor = (AInfoboxExtractor) constructors[0].newInstance();
            extractors.add(extractor);
        }

        return new PersonOrEventFilter(extractors);
    }
}
