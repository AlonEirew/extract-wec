package wec;

import java.util.List;

public class Configuration {
    private String poolSize;
    private String elasticHost;
    private String elasticPort;
    private String elasticWikiIndex;
    private String elasticWikinewsIndex;
    private String multiRequestInterval;
    private String elasticSearchInterval;
    private String totalAmountToExtract;
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

    public String getElasticPort() {
        return elasticPort;
    }

    public void setElasticPort(String elasticPort) {
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

    public String getMultiRequestInterval() {
        return multiRequestInterval;
    }

    public void setMultiRequestInterval(String multiRequestInterval) {
        this.multiRequestInterval = multiRequestInterval;
    }

    public String getElasticSearchInterval() {
        return elasticSearchInterval;
    }

    public void setElasticSearchInterval(String elasticSearchInterval) {
        this.elasticSearchInterval = elasticSearchInterval;
    }

    public String getTotalAmountToExtract() {
        return totalAmountToExtract;
    }

    public void setTotalAmountToExtract(String totalAmountToExtract) {
        this.totalAmountToExtract = totalAmountToExtract;
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
