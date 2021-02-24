package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class WECConfigurations {
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Configuration config;
    private static InfoboxConfiguration infoboxConf;

    static {
        InputStream configFile = Objects.requireNonNull(WECConfigurations.class.getClassLoader().getResourceAsStream("config.json"));
        config = GSON.fromJson(new InputStreamReader(configFile), Configuration.class);
        InputStream infoConfigFile = Objects.requireNonNull(WECConfigurations.class.getClassLoader()
                .getResourceAsStream(config.getInfoboxConfiguration()));
        infoboxConf = GSON.fromJson(new InputStreamReader(infoConfigFile), InfoboxConfiguration.class);
    }

    public static Configuration getConfig() {
        return config;
    }

    public static InfoboxConfiguration getInfoboxConf() {
        return infoboxConf;
    }

}
