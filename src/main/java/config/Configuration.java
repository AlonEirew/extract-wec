package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Configuration {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final Gson GSONPretty = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private int poolSize;
    private String elasticHost;
    private int elasticPort;
    private String elasticWikiIndex;
    private String infoboxConfiguration;
    private int multiRequestInterval;
    private int elasticSearchInterval;
    private int totalAmountToExtract;
    private String sqlConnectionUrl;

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public String getElasticHost() {
        return elasticHost;
    }

    public void setElasticHost(String elasticHost) {
        this.elasticHost = elasticHost;
    }

    public int getElasticPort() {
        return elasticPort;
    }

    public void setElasticPort(int elasticPort) {
        this.elasticPort = elasticPort;
    }

    public String getElasticWikiIndex() {
        return elasticWikiIndex;
    }

    public void setElasticWikiIndex(String elasticWikiIndex) {
        this.elasticWikiIndex = elasticWikiIndex;
    }

    public int getMultiRequestInterval() {
        return multiRequestInterval;
    }

    public void setMultiRequestInterval(int multiRequestInterval) {
        this.multiRequestInterval = multiRequestInterval;
    }

    public int getElasticSearchInterval() {
        return elasticSearchInterval;
    }

    public void setElasticSearchInterval(int elasticSearchInterval) {
        this.elasticSearchInterval = elasticSearchInterval;
    }

    public String getInfoboxConfiguration() {
        return infoboxConfiguration;
    }

    public void setInfoboxConfiguration(String infoboxConfiguration) {
        this.infoboxConfiguration = infoboxConfiguration;
    }

    public int getTotalAmountToExtract() {
        return totalAmountToExtract;
    }

    public void setTotalAmountToExtract(int totalAmountToExtract) {
        this.totalAmountToExtract = totalAmountToExtract;

        if(this.totalAmountToExtract == -1)
            this.totalAmountToExtract = Integer.MAX_VALUE;
    }

    public String getSqlConnectionUrl() {
        return sqlConnectionUrl;
    }

    public void setSqlConnectionUrl(String sqlConnectionUrl) {
        this.sqlConnectionUrl = sqlConnectionUrl;
    }

}
