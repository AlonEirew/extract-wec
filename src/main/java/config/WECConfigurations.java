package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Objects;

public class WECConfigurations {
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Configuration config;
    private static InfoboxConfiguration infoboxConf;

    static {
        String configFile = Objects.requireNonNull(WECConfigurations.class.getClassLoader().getResource("config.json")).getFile();
        try {
            config = GSON.fromJson(new FileReader(configFile), Configuration.class);
            String infoConfigFile = Objects.requireNonNull(WECConfigurations.class.getClassLoader().getResource(config.getInfoboxConfiguration())).getFile();
            infoboxConf = GSON.fromJson(new FileReader(infoConfigFile), InfoboxConfiguration.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Configuration getConfig() {
        return config;
    }

    public static InfoboxConfiguration getInfoboxConf() {
        return infoboxConf;
    }

}
