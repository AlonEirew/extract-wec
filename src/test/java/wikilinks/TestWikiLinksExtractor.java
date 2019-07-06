package wikilinks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import data.RawElasticResult;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import data.WikiLinksMention;
import persistence.ElasticQueryApi;
import workers.ReadInfoBoxWorker;

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
        List<String> pageTexts = getCivilAttack();
        for(String text : pageTexts) {
            final Set<String> extractMentions = WikiLinksExtractor.extractTypes(text);
            System.out.println();
        }
    }

    @Test
    public void testInfoBoxExtract() {
        String pageText = getInfoBoxs();
        ReadInfoBoxWorker worker = new ReadInfoBoxWorker();
        final String infoBoxs = worker.extractPageInfoBox(pageText);
        System.out.println(infoBoxs);
    }

    @Test
    public void testHasDateAndLocation() {

        List<String> pageTexts = getCivilAttack();
        for(String text : pageTexts) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text);
            boolean ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
            Assert.assertTrue(ret);
            break;
        }

        String pageText = getWeddingText();
        String infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        boolean ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
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
    public void testIsPerson() {
        String pageText = getAlenTuringText();
        boolean ret = WikiLinksExtractor.isPerson(pageText);
        Assert.assertTrue(ret);
    }

    @Test
    public void testIsElection() {
        List<String> pageText = getElectionText();
        for(String text : pageText) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text);
            boolean ret = WikiLinksExtractor.isElection(infoBox);
            Assert.assertTrue(ret);
        }

        List<String> other = getAccidentText();
        other.addAll(getCivilAttack());
        other.addAll(getDisasterText());
        other.addAll(getSportText());
//        other.addAll(getElectionText());

        for(String text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text);
            boolean ret = WikiLinksExtractor.isElection(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testAccident() {
        List<String> pageTexts = getAccidentText();
        for(String text : pageTexts) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text);
            boolean ret = WikiLinksExtractor.isAccident(infoBox);
            Assert.assertTrue(ret);
        }

        List<String> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getDisasterText());
        other.addAll(getSportText());
        other.addAll(getElectionText());

        for(String text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text);
            boolean ret = WikiLinksExtractor.isAccident(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testSmallCompany() {
        String infoBox = WikiLinksExtractor.extractPageInfoBox(getSmallCompanyText());
        boolean ret = WikiLinksExtractor.isSmallCompanyEvent(infoBox);
        Assert.assertTrue(ret);
    }

    @Test
    public void testSport() {

        final List<String> sportText = getSportText();
        for(String text : sportText) {
            boolean ret = WikiLinksExtractor.isSportEvent(WikiLinksExtractor.extractPageInfoBox(text));
            Assert.assertTrue(ret);
        }

        List<String> other = getAccidentText();
        other.addAll(getCivilAttack());
        other.addAll(getDisasterText());
//        other.addAll(getSportText());
        other.addAll(getElectionText());

        for(String text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text);
            boolean ret = WikiLinksExtractor.isSportEvent(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testIsAward() {

        final List<Pair<String, String>> awardPair = getAwards();
        for(Pair<String, String> pair : awardPair) {
            boolean ret = WikiLinksExtractor.isAwardEvent(WikiLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertTrue(ret);
        }

        final List<Pair<String, String>> newsPair = getConcreteGeneralTexts();
        for(Pair<String, String> pair : newsPair) {
            boolean ret = WikiLinksExtractor.isAwardEvent(WikiLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testIsConcreteGeneralEvent() {

        final List<Pair<String, String>> newsPair = getConcreteGeneralTexts();
        for(Pair<String, String> pair : newsPair) {
            boolean ret = WikiLinksExtractor.isConcreteGeneralEvent(WikiLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertTrue(ret);
        }

        final List<Pair<String, String>> awardPair = getAwards();
        for(Pair<String, String> pair : awardPair) {
            boolean ret = WikiLinksExtractor.isConcreteGeneralEvent(WikiLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testIsDisaster() {
        final List<String> disasterText = getDisasterText();
        for(String text : disasterText) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text);
            boolean ret = WikiLinksExtractor.isDisaster(infoBox);
            Assert.assertTrue(ret);
        }

        List<String> other = new ArrayList<>();
        other.addAll(getCivilAttack());
//        other.addAll(getDisasterText());
        other.addAll(getSportText());
        other.addAll(getElectionText());

        for(String text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text);
            boolean ret = WikiLinksExtractor.isDisaster(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testPersonOrEventFilter() {
        PersonOrEventFilter filter = new PersonOrEventFilter();
        RawElasticResult input = new RawElasticResult("Alan Turing", WikiLinksExtractor.extractPageInfoBox(getAlenTuringText()));
        Assert.assertFalse(filter.isConditionMet(input));

        final List<String> stringList = getCivilAttack();
        for(String text : stringList) {
            input = new RawElasticResult("September 11 attacks", WikiLinksExtractor.extractPageInfoBox(text));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<String> accidentText = getAccidentText();
        for(String text : accidentText) {
            input = new RawElasticResult("Pilot Error", WikiLinksExtractor.extractPageInfoBox(text));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<String> disasterText = getDisasterText();
        for(String text : disasterText) {
            input = new RawElasticResult("Tsunami", WikiLinksExtractor.extractPageInfoBox(text));
            Assert.assertFalse(filter.isConditionMet(input));
        }

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

    private List<String> getTexts(String fileNme) {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream(fileNme);
        JsonArray inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonArray.class);

        List<String> retTexts = new ArrayList<>();
        for(JsonElement jsonObj : inputJsonNlp) {
            retTexts.add(jsonObj.getAsJsonObject().get("text").getAsString());
        }

        return retTexts;
    }

    private List<Pair<String, String>> getTextAndTitle(String fileName) {
        InputStream inputStreamNlp = TestWikiLinksExtractor.class.getClassLoader().getResourceAsStream(fileName);
        JsonArray inputJsonNlp = gson.fromJson(new InputStreamReader(inputStreamNlp), JsonArray.class);

        List<Pair<String, String>> retTexts = new ArrayList<>();
        for(JsonElement jsonObj : inputJsonNlp) {
            Pair<String, String> pair = new Pair<>(jsonObj.getAsJsonObject().get("title").getAsString(),
                    jsonObj.getAsJsonObject().get("text").getAsString());
            retTexts.add(pair);
        }

        return retTexts;
    }

    private String getSmallCompanyText() {
        return getText("mobileye.json");
    }

    private List<String> getSportText() {
        return getTexts("sport.json");
    }

    private List<String> getDisasterText() {
        return getTexts("disaster.json");
    }

    private List<String> getCivilAttack() {
        return getTexts("civil_attack.json");
    }

    private String getAlenTuringText() {
        return getText("alan_turing.json");
    }

    private String getWeddingText() {
        return getText("wedding.json");
    }

    private String getKitKatText() {
        return getText("kit_kat.json");
    }

    private List<String> getElectionText() {
        return getTexts("election.json");
    }

    private List<String> getAccidentText() {
        return getTexts("accident.json");
    }

    private String getFilmList() {
        return getText("list_of_films.json");
    }

    private List<Pair<String, String>> getAwards() {
        return getTextAndTitle("award.json");
    }

    private List<Pair<String, String>> getConcreteGeneralTexts() {
        return getTextAndTitle("concrete_general.json");
    }

    private String getPresidentsList() {
        return getText("list_of_presidents.json");
    }

    private String getInfoBoxs() {
        return getText("many_infobox.json");
    }
}
