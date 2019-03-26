package wikilinks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestWikiLinksExtractor {

    private Gson gson = new Gson();

    @Test
    public void testExtract() {
        String[] textLines = getText().split("\n");
        Map<String, List<WikiLinksMention>> finalResults = new HashMap<>();
        for (String line : textLines) {
            final List<WikiLinksMention> extractMentions = WikiLinksExtractor.extractFromLine("", line);
            for(WikiLinksMention mention : extractMentions) {
                if(finalResults.containsKey(mention.getCorefChain())) {
                    finalResults.get(mention.getCorefChain()).add(mention);
                } else {
                    finalResults.put(mention.getCorefChain().getCorefValue(), new ArrayList<>());
                    finalResults.get(mention.getCorefChain()).add(mention);
                }
            }
        }

        System.out.println(gson.toJson(finalResults));
    }

    private String getText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("alan_turing.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }
}
