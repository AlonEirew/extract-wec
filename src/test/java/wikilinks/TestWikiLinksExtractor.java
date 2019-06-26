package wikilinks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.RawElasticResult;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import data.WikiLinksMention;
import persistence.ElasticQueryApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TestWikiLinksExtractor {

    private Gson gson = new Gson();

    @Test
    public void testExtract() {
        Map<String, List<WikiLinksMention>> finalResults = new HashMap<>();
        String pageText = getFilmList();
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
    public void testGetAllPagesTexts() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Map<String, String> config = getConfigFile();

        ElasticQueryApi elasticQueryApi = new ElasticQueryApi(config);
        Set<String> pagesList = new HashSet<>();
        pagesList.add("Alan Turing");
        pagesList.add("September 11 attacks");
        final Map<String, String> allPagesText = elasticQueryApi.getAllWikiPagesTitleAndText(pagesList);

        final String alan_turing = WikiLinksExtractor.extractPageInfoBox(allPagesText.get("Alan Turing"));
        Assert.assertTrue(WikiLinksExtractor.isPerson(alan_turing));
        Assert.assertTrue(WikiLinksExtractor.extractTypes(alan_turing).isEmpty());

        final String sep_11 = WikiLinksExtractor.extractPageInfoBox(allPagesText.get("September 11 attacks"));
        Assert.assertFalse(WikiLinksExtractor.isPerson(sep_11));
        Assert.assertTrue(!WikiLinksExtractor.extractTypes(sep_11).isEmpty());
    }

    @Test
    public void testGetPageText() throws IOException {
        Map<String, String> config = getConfigFile();

        ElasticQueryApi elasticQueryApi = new ElasticQueryApi(config);
        final String alan_turing = elasticQueryApi.getPageText("Alan Turing");
        final String infoBox = WikiLinksExtractor.extractPageInfoBox(alan_turing);
        Assert.assertTrue(WikiLinksExtractor.isPerson(infoBox));
        Assert.assertTrue(WikiLinksExtractor.extractTypes(infoBox).isEmpty());

        final String sep_11 = elasticQueryApi.getPageText("September 11 attacks");
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
    public void testSmallCompany() {
        String infoBox = WikiLinksExtractor.extractPageInfoBox(getSmallCompanyText());
        boolean ret = WikiLinksExtractor.isSmallCompanyEvent(infoBox);
        Assert.assertTrue(ret);
    }

    @Test
    public void testSport() {
        boolean ret = WikiLinksExtractor.isSportEvent(WikiLinksExtractor.extractPageInfoBox(getSportDraftText()));
        Assert.assertTrue(ret);

        ret = WikiLinksExtractor.isSportEvent(WikiLinksExtractor.extractPageInfoBox(getSportMatchText()));
        Assert.assertTrue(ret);

        ret = WikiLinksExtractor.isSportEvent(WikiLinksExtractor.extractPageInfoBox(getChampText()));
        Assert.assertTrue(ret);

        ret = WikiLinksExtractor.isSportEvent(WikiLinksExtractor.extractPageInfoBox(get911Text()));
        Assert.assertFalse(ret);
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

    private Map<String, String> getConfigFile() throws IOException {
        final String property = System.getProperty("user.dir");

        Map<String, String> config = gson.fromJson(FileUtils.readFileToString(
                new File(property + "/config.json"), "UTF-8"),
                Map.class);

        return config;
    }

    private String getText(String fileNme) {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream(fileNme);
        JsonObject inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonObject.class);
        return inputJsonNlp.get("text").getAsString();
    }

    private String getSmallCompanyText() {
        return getText("mobileye.json");
    }

    private String getChampText() {
        return getText("championships.json");
    }

    private String getSportMatchText() {
        return getText("sport_match.json");
    }

    private String getSportDraftText() {
        return getText("sport_draft.json");
    }

    private String getEarthquake1Text() {
        return getText("earthquake1.json");
    }

    private String getTsunamiText() {
        return getText("tsunami.json");
    }

    private String get911Text() {
        return getText("september_11_attacks.json");
    }

    private String getAlenTuringText() {
        return getText("alan_turing.json");
    }

    private String getCharlieHabdoText() {
        return getText("charlie_hebdo_shooting.json");
    }

    private String getWeddingText() {
        return getText("wedding.json");
    }

    private String getKitKatText() {
        return getText("kit_kat.json");
    }

    private String getElection1() {
        return getText("election_1.json");
    }

    private String getPilotErrorText() {
        return getText("pilot_error.json");
    }

    private String getFilmList() {
        return getText("list_of_films.json");
    }

    private String getPresidentsList() {
        return getText("list_of_presidents.json");
    }

    private String getTmp() {
        return getText("tmp.json");
    }
}
