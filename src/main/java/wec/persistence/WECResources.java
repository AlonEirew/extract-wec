package wec.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WECResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(WECResources.class);

    private static ElasticQueryApi elasticApi;
    private static DBRepository dbRepository;

    public static ElasticQueryApi getElasticApi() {
        return elasticApi;
    }

    public static void setElasticApi(ElasticQueryApi elasticApi) {
        WECResources.elasticApi = elasticApi;
    }

    public static DBRepository getDbRepository() {
        return dbRepository;
    }

    public static void setDbRepository(DBRepository dbRepository) {
        WECResources.dbRepository = dbRepository;
    }

    public static void closeAllResources() {
        LOGGER.info("Closing all resources...");
        if(elasticApi != null) {
            elasticApi.close();
        }
    }
}
