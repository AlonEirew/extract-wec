package data;

public class Configuration {
    private String poolSize;
    private String elasticHost;
    private int elasticPort;
    private String elasticWikiIndex;
    private String infoboxConfiguration;
    private int multiRequestInterval;
    private int elasticSearchInterval;
    private int totalAmountToExtract;
    private String sqlConnectionUrl;

    public String getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(String poolSize) {
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
