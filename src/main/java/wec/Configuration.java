package wec;

import java.util.List;

public class Configuration {
    private String poolSize;
    private String elasticHost;
    private int elasticPort;
    private String elasticWikiIndex;
    private String elasticWikinewsIndex;
    private int multiRequestInterval;
    private int elasticSearchInterval;
    private int totalAmountToExtract;
    private String sqlConnectionUrl;
    private List<String> useExtractors;

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

    public String getElasticWikinewsIndex() {
        return elasticWikinewsIndex;
    }

    public void setElasticWikinewsIndex(String elasticWikinewsIndex) {
        this.elasticWikinewsIndex = elasticWikinewsIndex;
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

    public List<String> getUseExtractors() {
        return useExtractors;
    }

    public void setUseExtractors(List<String> useExtractors) {
        this.useExtractors = useExtractors;
    }
}
