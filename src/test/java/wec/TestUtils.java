package wec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {
    private static final Gson GSON = new Gson();

    public static List<AbstractMap.SimpleEntry<String, String>> getTextAndTitle(String fileName) {
        InputStream inputStreamNlp = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream(fileName);
        assert inputStreamNlp != null;
        JsonArray inputJsonNlp = GSON.fromJson(new InputStreamReader(inputStreamNlp), JsonArray.class);

        List<AbstractMap.SimpleEntry<String, String>> retTexts = new ArrayList<>();
        for(JsonElement jsonObj : inputJsonNlp) {
            AbstractMap.SimpleEntry<String, String> pair = new AbstractMap.SimpleEntry<>(jsonObj.getAsJsonObject().get("title").getAsString(),
                    jsonObj.getAsJsonObject().get("text").getAsString());
            retTexts.add(pair);
        }

        return retTexts;
    }

    public static List<JsonObject> getTextTitleAndExpected(String fileName) {
        InputStream inputStreamNlp = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream(fileName);
        assert inputStreamNlp != null;
        JsonArray inputJsonNlp = GSON.fromJson(new InputStreamReader(inputStreamNlp), JsonArray.class);

        List<JsonObject> retTexts = new ArrayList<>();
        for(JsonElement jsonObj : inputJsonNlp) {
            final JsonObject asJsonObject = jsonObj.getAsJsonObject();
            retTexts.add(asJsonObject);
        }

        return retTexts;
    }

    public static String getText(String fileNme) {
        InputStream inputStreamNlp = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream(fileNme);
        JsonObject inputJsonNlp = GSON.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }
}
