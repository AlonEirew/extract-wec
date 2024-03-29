package wec.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.core.env.Environment;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class Configuration {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final Gson GSONPretty = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Configuration configuration;

    private final int poolSize;
    private final String elasticHost;
    private final int elasticPort;
    private final String elasticWikiIndex;
    private final String infoboxConfigurationFile;
    private final int multiRequestInterval;
    private final int elasticSearchInterval;
    private final int totalAmountToExtract;
    private final String jsonOutputDir;
    private final String jsonOutputFile;
    private final int lexicalThresh;
    private final InfoboxConfiguration infoboxConfiguration;

    private Configuration(Environment environment) {
        this.poolSize = Integer.parseInt(Objects.requireNonNull(environment.getProperty("main.poolSize",
                String.valueOf(Runtime.getRuntime().availableProcessors()))));
        this.elasticHost = environment.getProperty("main.elasticHost");
        this.elasticPort = Integer.parseInt(Objects.requireNonNull(environment.getProperty("main.elasticPort")));
        this.elasticWikiIndex = environment.getProperty("main.elasticWikiIndex");
        this.infoboxConfigurationFile = environment.getProperty("main.infoboxConfiguration");
        this.multiRequestInterval = Integer.parseInt(Objects.requireNonNull(environment.getProperty("main.multiRequestInterval", "100")));
        this.elasticSearchInterval = Integer.parseInt(Objects.requireNonNull(environment.getProperty("main.elasticSearchInterval", "100")));
        this.totalAmountToExtract = Integer.parseInt(Objects.requireNonNull(environment.getProperty("main.totalAmountToExtract", "-1")));
        this.jsonOutputDir = environment.getProperty("main.outputDir", "output");
        this.jsonOutputFile = environment.getProperty("main.outputFile", "GenWEC.json");
        this.lexicalThresh = Integer.parseInt(Objects.requireNonNull(environment.getProperty("main.lexicalThresh", "4")));

        InputStream inputStreamConfigFile = Objects.requireNonNull(Configuration.class.getClassLoader()
                .getResourceAsStream(this.infoboxConfigurationFile));
        this.infoboxConfiguration = Configuration.GSON.fromJson(new InputStreamReader(inputStreamConfigFile), InfoboxConfiguration.class);
    }

    public static void initConfiguration(Environment environment) {
        if (configuration == null) {
            configuration = new Configuration(environment);
        }
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public String getElasticHost() {
        return elasticHost;
    }

    public int getElasticPort() {
        return elasticPort;
    }

    public String getElasticWikiIndex() {
        return elasticWikiIndex;
    }

    public int getMultiRequestInterval() {
        return multiRequestInterval;
    }

    public int getElasticSearchInterval() {
        return elasticSearchInterval;
    }

    public String getInfoboxConfigurationFile() {
        return infoboxConfigurationFile;
    }

    public int getTotalAmountToExtract() {
        return totalAmountToExtract;
    }

    public InfoboxConfiguration getInfoboxConfiguration() {
        return infoboxConfiguration;
    }

    public String getJsonOutputDir() {
        return jsonOutputDir;
    }

    public String getJsonOutputFile() {
        return jsonOutputFile;
    }

    public int getLexicalThresh() {
        return lexicalThresh;
    }
}
