package wec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.util.Pair;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {
    private static Gson gson = new Gson();

    public static List<Pair<String, String>> getTextAndTitle(String fileName) {
        InputStream inputStreamNlp = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream(fileName);
        JsonArray inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonArray.class);

        List<Pair<String, String>> retTexts = new ArrayList<>();
        for(JsonElement jsonObj : inputJsonNlp) {
            Pair<String, String> pair = new Pair<>(jsonObj.getAsJsonObject().get("title").getAsString(),
                    jsonObj.getAsJsonObject().get("text").getAsString());
            retTexts.add(pair);
        }

        return retTexts;
    }

    public static List<JsonObject> getTextTitleAndExpected(String fileName) {
        InputStream inputStreamNlp = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream(fileName);
        JsonArray inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonArray.class);

        List<JsonObject> retTexts = new ArrayList<>();
        for(JsonElement jsonObj : inputJsonNlp) {
            final JsonObject asJsonObject = jsonObj.getAsJsonObject();
            retTexts.add(asJsonObject);
        }

        return retTexts;
    }

    public static String getText(String fileNme) {
        InputStream inputStreamNlp = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream(fileNme);
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }
}
