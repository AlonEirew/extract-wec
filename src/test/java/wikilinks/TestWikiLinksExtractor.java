package wikilinks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.RawElasticResult;
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
        String pageText = get911Text();
        final List<WikiLinksMention> extractMentions = WikiLinksExtractor.extractFromFile("na", pageText);
        for (WikiLinksMention mention : extractMentions) {
            if(finalResults.containsKey(mention.getCorefChain())) {
                finalResults.get(mention.getCorefChain()).add(mention);
            } else {
                finalResults.put(mention.getCorefChain().getCorefValue(), new ArrayList<>());
                finalResults.get(mention.getCorefChain().getCorefValue()).add(mention);
            }
        }

        System.out.println(gson.toJson(finalResults));
    }

    @Test
    public void testExtractTypes() {
        String pageText = get911Text();
        final Set<String> extractMentions = WikiLinksExtractor.extractTypes(pageText);
        System.out.println();
    }

    @Test
    public void testIsPerson() {
        String pageText = getAlenTuringText();
        boolean ret = WikiLinksExtractor.isPerson(pageText);
        Assert.assertTrue(ret);
    }

    @Test
    public void testHasDateAndLocation() {
        String pageText = getTmp();
        String infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        boolean ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
        Assert.assertFalse(ret);

        pageText = getCharlieHabdoText();
        infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
        Assert.assertTrue(ret);

        pageText = get911Text();
        infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
        Assert.assertTrue(ret);

        pageText = getWeddingText();
        infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
        Assert.assertTrue(ret);
        System.out.println();

        pageText = getAlenTuringText();
        infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
        Assert.assertFalse(ret);

        pageText = getKitKatText();
        infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
        Assert.assertFalse(ret);
    }

    @Test
    public void testGetAllPagesTexts() throws IOException {
        CreateWikiLinks wikiLinks = new CreateWikiLinks(null);
        Set<String> pagesList = new HashSet<>();
        pagesList.add("Alan Turing");
        pagesList.add("September 11 attacks");
        final Map<String, String> allPagesText = wikiLinks.getAllPagesTitleAndText(pagesList);

        final String alan_turing = WikiLinksExtractor.extractPageInfoBox(allPagesText.get("Alan Turing"));
        Assert.assertTrue(WikiLinksExtractor.isPerson(alan_turing));
        Assert.assertTrue(WikiLinksExtractor.extractTypes(alan_turing).isEmpty());

        final String sep_11 = WikiLinksExtractor.extractPageInfoBox(allPagesText.get("September 11 attacks"));
        Assert.assertFalse(WikiLinksExtractor.isPerson(sep_11));
        Assert.assertTrue(!WikiLinksExtractor.extractTypes(sep_11).isEmpty());
    }

    @Test
    public void testGetPageText() throws IOException {
        CreateWikiLinks wikiLinks = new CreateWikiLinks(null);
        final String alan_turing = wikiLinks.getPageText("Alan Turing");
        final String infoBox = WikiLinksExtractor.extractPageInfoBox(alan_turing);
        Assert.assertTrue(WikiLinksExtractor.isPerson(infoBox));
        Assert.assertTrue(WikiLinksExtractor.extractTypes(infoBox).isEmpty());

        final String sep_11 = wikiLinks.getPageText("September 11 attacks");
        Assert.assertFalse(WikiLinksExtractor.isPerson(sep_11));
        Assert.assertTrue(!WikiLinksExtractor.extractTypes(sep_11).isEmpty());
    }

    @Test
    public void testIsElection() {
        String pageText = getElection1();
        String infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        boolean ret = WikiLinksExtractor.isElection(infoBox);
        Assert.assertTrue(ret);
    }

    @Test
    public void testAccident() {
        String pageText = getPilotErrorText();
        String infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        boolean ret = WikiLinksExtractor.isAccident(infoBox);
        Assert.assertTrue(ret);
    }

    @Test
    public void testIsDisaster() {
        String infoBox = WikiLinksExtractor.extractPageInfoBox(getEarthquake1Text());
        boolean ret = WikiLinksExtractor.isDisaster(infoBox);
        Assert.assertTrue(ret);

        infoBox = WikiLinksExtractor.extractPageInfoBox(get911Text());
        ret = WikiLinksExtractor.isDisaster(infoBox);
        Assert.assertFalse(ret);
    }

    @Test
    public void testPersonOrEventFilter() {
        PersonOrEventFilter filter = new PersonOrEventFilter();
        RawElasticResult input = new RawElasticResult("Alan Turing", WikiLinksExtractor.extractPageInfoBox(getAlenTuringText()));
        Assert.assertFalse(filter.isConditionMet(input));

        input = new RawElasticResult("September 11 attacks", WikiLinksExtractor.extractPageInfoBox(get911Text()));
        Assert.assertFalse(filter.isConditionMet(input));

        input = new RawElasticResult("Charlie Hebdo", WikiLinksExtractor.extractPageInfoBox(getCharlieHabdoText()));
        Assert.assertFalse(filter.isConditionMet(input));

        input = new RawElasticResult("Pilot Error", WikiLinksExtractor.extractPageInfoBox(getPilotErrorText()));
        Assert.assertFalse(filter.isConditionMet(input));

        input = new RawElasticResult("Tsunami", WikiLinksExtractor.extractPageInfoBox(getTsunamiText()));
        Assert.assertFalse(filter.isConditionMet(input));

        input = new RawElasticResult("Kit Kat", WikiLinksExtractor.extractPageInfoBox(getKitKatText()));
        Assert.assertTrue(filter.isConditionMet(input));
    }

    private String getEarthquake1Text() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("earthquake1.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getTsunamiText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("tsunami.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String get911Text() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("september_11_attacks.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getAlenTuringText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("alan_turing.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getCharlieHabdoText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("charlie_hebdo_shooting.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getWeddingText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("wedding.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getKitKatText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("kit_kat.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getElection1() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("election_1.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getPilotErrorText() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("pilot_error.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getTmp() {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream("tmp.json");
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }
}
