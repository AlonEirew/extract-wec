package wec.persistence;

import org.springframework.stereotype.Component;

@Component
public class WECResources {
    private static ElasticQueryApi sElasticApi;

    private static WecRepository sWECRepository;

    public static ElasticQueryApi getElasticApi() {
        return sElasticApi;
    }

    public static void setElasticApi(ElasticQueryApi elasticApi) {
        sElasticApi = elasticApi;
    }

    public static void setWECRepository(WecRepository sWECRepository) {
        WECResources.sWECRepository = sWECRepository;
    }

    public static WecRepository getWECRepository() {
        return sWECRepository;
    }

    public static void closeAllResources() {
        if(sElasticApi != null) {
            sElasticApi.close();
        }
    }
}
