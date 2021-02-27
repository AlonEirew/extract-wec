package config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class WECConfigurations {
    private static Configuration config;
    private static InfoboxConfiguration infoboxConf;

    static {
        InputStream configFile = Objects.requireNonNull(WECConfigurations.class.getClassLoader().getResourceAsStream("config.json"));
        config = Configuration.GSON.fromJson(new InputStreamReader(configFile), Configuration.class);
        InputStream infoConfigFile = Objects.requireNonNull(WECConfigurations.class.getClassLoader()
                .getResourceAsStream(config.getInfoboxConfiguration()));
        infoboxConf = Configuration.GSON.fromJson(new InputStreamReader(infoConfigFile), InfoboxConfiguration.class);
    }

    public static Configuration getConfig() {
        return config;
    }

    public static InfoboxConfiguration getInfoboxConf() {
        return infoboxConf;
    }

}
