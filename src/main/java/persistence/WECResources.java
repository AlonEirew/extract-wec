package persistence;

public class WECResources {
    private static SQLQueryApi sSqlApi;
    private static ElasticQueryApi sElasticApi;

    public static SQLQueryApi getSqlApi() {
        return sSqlApi;
    }

    public static void setSqlApi(SQLQueryApi sqlApi) {
        sSqlApi = sqlApi;
    }

    public static ElasticQueryApi getElasticApi() {
        return sElasticApi;
    }

    public static void setElasticApi(ElasticQueryApi elasticApi) {
        sElasticApi = elasticApi;
    }

    public static void closeAllResources() {
        if(sSqlApi != null) {
            sSqlApi.persistAllMentions();
            sSqlApi.persistAllCorefs();
        }

        if(sElasticApi != null) {
            sElasticApi.close();
        }
    }
}
