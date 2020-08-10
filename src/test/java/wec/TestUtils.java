package wec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import data.RawElasticResult;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {
    private static final Gson GSON = new Gson();

    public static List<RawElasticResult> getTextAndTitle(String fileName) {
        InputStream inputStreamNlp = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream(fileName);
        assert inputStreamNlp != null;
        JsonArray inputJsonNlp = GSON.fromJson(new InputStreamReader(inputStreamNlp), JsonArray.class);

        List<RawElasticResult> retTexts = new ArrayList<>();
        for(JsonElement jsonObj : inputJsonNlp) {
            String title = jsonObj.getAsJsonObject().get("title").getAsString();
            String text = jsonObj.getAsJsonObject().get("text").getAsString();
            String infobox = jsonObj.getAsJsonObject().get("infobox").getAsString();

            retTexts.add(new RawElasticResult(title, text, infobox));
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

    public static RawElasticResult getText(String fileNme) {
        InputStream inputStreamNlp = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream(fileNme);
        JsonObject inputJsonNlp = GSON.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        String text = inputJsonNlp.get("text").getAsString();
        String title = inputJsonNlp.get("title").getAsString();
        String infobox = inputJsonNlp.get("infobox").getAsString();
        return new RawElasticResult(title, text, infobox);
    }
}
