package experimentscripts.wec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.WECResources;
import utils.ExecutorServiceFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class ExtractInfoboxs {
    private final static Logger LOGGER = LogManager.getLogger(ExtractInfoboxs.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws FileNotFoundException {
        long start = System.currentTimeMillis();
        final String property = System.getProperty("user.dir");
        LOGGER.info("Working directory=" + property);

        Configuration config = GSON.fromJson(new FileReader(property + "/config.json"), Configuration.class);

        ExecutorServiceFactory.initExecutorService();
        WECResources.setElasticApi(new ElasticQueryApi(config));


    }
}
