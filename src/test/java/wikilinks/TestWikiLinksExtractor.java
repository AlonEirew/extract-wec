package wikilinks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import data.WikiLinksMention;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class TestWikiLinksExtractor {

    private Gson gson = new Gson();

    @Test
    public void testExtract() {
        Map<String, List<WikiLinksMention>> finalResults = new HashMap<>();
        String pageText = getText();
        final List<WikiLinksMention> extractMentions = WikiLinksExtractor.extractFromFile("kit_kat", pageText);
        for (WikiLinksMention mention : extractMentions) {
            if(finalResults.containsKey(mention.getCorefChain())) {
                finalResults.get(mention.getCorefChain()).add(mention);
            } else {
                finalResults.put(mention.getCorefChain().getCorefValue(), new ArrayList<>());
                finalResults.get(mention.getCorefChain()).add(mention);
            }
        }

        System.out.println(gson.toJson(finalResults));
    }

    @Test
    public void testExtractTypes() {
        String pageText = getText();
        final Set<String> extractMentions = WikiLinksExtractor.extractTypes(pageText);
        System.out.println();
    }

    @Test
    public void testIsPerson() {
        String pageText = getText();
        boolean ret = WikiLinksExtractor.isPersonPage(pageText);
        System.out.println();
    }

    @Test
    public void testGetPageText() throws IOException {
        CreateWikiLinks wikiLinks = new CreateWikiLinks(null);
        final String alan_turing = wikiLinks.getPageText("Alan Turing");
        Assert.assertTrue(WikiLinksExtractor.isPersonPage(alan_turing));
        Assert.assertTrue(WikiLinksExtractor.extractTypes(alan_turing).isEmpty());

        final String sep_11 = wikiLinks.getPageText("September 11 attacks");
        Assert.assertFalse(WikiLinksExtractor.isPersonPage(sep_11));
        Assert.assertTrue(!WikiLinksExtractor.extractTypes(sep_11).isEmpty());
    }

    @Test
    public void testPersonOrEventFilter() {
        PersonOrEventFilter filter = new PersonOrEventFilter(new CreateWikiLinks(null));
        final WikiLinksMention input = new WikiLinksMention();
        input.setCorefChain("Alan Turing");
        Assert.assertTrue(filter.isConditionMet(input));

        input.setCorefChain("September 11 attacks");
        Assert.assertTrue(filter.isConditionMet(input));

        input.setCorefChain("Kit Kat");
        Assert.assertFalse(filter.isConditionMet(input));
    }

    private String getText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("september_11_attacks.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getAlenTuringText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("alan_turing.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }
}
