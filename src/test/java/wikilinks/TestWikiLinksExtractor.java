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
        List<Pair<String, String>> pageTexts = getCivilAttack();
        for(Pair<String, String> text : pageTexts) {
            final Set<String> extractMentions = WikiLinksExtractor.extractTypes(text.getValue());
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

        List<Pair<String, String>> pageTexts = getCivilAttack();
        for(Pair<String, String> text : pageTexts) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
            Assert.assertTrue(ret);
            break;
        }

        String pageText = getWeddingText();
        String infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
        boolean ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
        Assert.assertTrue(ret);
        System.out.println();

        final List<Pair<String, String>> peopleText = getPeopleText();
        for(Pair<String, String> text : peopleText) {
            infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            ret = WikiLinksExtractor.hasDateAndLocation(infoBox);
            Assert.assertFalse(ret);
        }
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
        final List<Pair<String, String>> peopleText = getPeopleText();
        for(Pair<String, String> text : peopleText) {
            boolean ret = WikiLinksExtractor.isPerson(text.getValue());
            Assert.assertTrue(ret);
        }
    }

    @Test
    public void testIsElection() {
        List<Pair<String, String>> pageText = getElectionText();
        for(Pair<String, String> text : pageText) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isElection(infoBox, text.getKey());
            Assert.assertTrue(ret);
        }

        List<Pair<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getAccidentText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getPeopleText());

         for(Pair<String, String> text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isElection(infoBox, text.getKey());
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testAccident() {
        List<Pair<String, String>> pageTexts = getAccidentText();
        for(Pair<String, String> text : pageTexts) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isAccident(infoBox);
            Assert.assertTrue(ret);
        }

        List<Pair<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getPeopleText());

        for(Pair<String, String> text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
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
    public void testIsSport() {

        final List<Pair<String, String>> sportText = getSportText();
        for(Pair<String, String> text : sportText) {
            boolean ret = WikiLinksExtractor.isSportEvent(WikiLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertTrue(text.getKey(), ret);
        }

        List<Pair<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getPeopleText());

        for(Pair<String, String> text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isSportEvent(infoBox);
            Assert.assertFalse(text.getKey(), ret);
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

        List<Pair<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getPeopleText());

        for(Pair<String, String> pair : other) {
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

        List<Pair<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(Pair<String, String> pair : other) {
            boolean ret = WikiLinksExtractor.isConcreteGeneralEvent(WikiLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testIsDisaster() {
        final List<Pair<String, String>> disasterText = getDisasterText();
        for(Pair<String, String> text : disasterText) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isDisaster(infoBox);
            Assert.assertTrue(ret);
        }

        List<Pair<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(Pair<String, String> text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isDisaster(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void isCivilAttack() {
        final List<Pair<String, String>> civilAttack = getCivilAttack();
        for(Pair<String, String> text : civilAttack) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isCivilAttack(infoBox);
            Assert.assertTrue(ret);
        }

        List<Pair<String, String>> other = new ArrayList<>();
        other.addAll(getDisasterText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(Pair<String, String> text : other) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isCivilAttack(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testReject() {
        final List<Pair<String, String>> rejectTexts = getRejectTexts();
        for(Pair<String, String> pair : rejectTexts) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(pair.getValue());

            boolean ret = WikiLinksExtractor.isDisaster(infoBox);
            Assert.assertFalse(pair.getKey(), ret);

            ret = WikiLinksExtractor.isAwardEvent(infoBox, pair.getKey());
            Assert.assertFalse(pair.getKey(), ret);

            ret = WikiLinksExtractor.isConcreteGeneralEvent(infoBox, pair.getKey());
            Assert.assertFalse(pair.getKey(), ret);

            ret = WikiLinksExtractor.isAccident(infoBox);
            Assert.assertFalse(pair.getKey(), ret);

            ret = WikiLinksExtractor.isSportEvent(infoBox);
            Assert.assertFalse(pair.getKey(), ret);

            ret = WikiLinksExtractor.isCivilAttack(infoBox);
            Assert.assertFalse(pair.getKey(), ret);

            ret = WikiLinksExtractor.isElection(infoBox, pair.getKey());
            Assert.assertFalse(pair.getKey(), ret);
        }
    }

    @Test
    public void testPersonOrEventFilter() {
        PersonOrEventFilter filter = new PersonOrEventFilter();


        final List<Pair<String, String>> peopleText = getPeopleText();
        for(Pair<String, String> text : peopleText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WikiLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<Pair<String, String>> stringList = getCivilAttack();
        for(Pair<String, String> text : stringList) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WikiLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<Pair<String, String>> accidentText = getAccidentText();
        for(Pair<String, String> text : accidentText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WikiLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<Pair<String, String>> disasterText = getDisasterText();
        for(Pair<String, String> text : disasterText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WikiLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }
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

    private List<Pair<String, String>> getSportText() {
        return getTextAndTitle("sport.json");
    }

    private List<Pair<String, String>> getDisasterText() {
        return getTextAndTitle("disaster.json");
    }

    private List<Pair<String, String>> getCivilAttack() {
        return getTextAndTitle("civil_attack.json");
    }

    private List<Pair<String, String>> getPeopleText() {
        return getTextAndTitle("people.json");
    }

    private String getWeddingText() {
        return getText("wedding.json");
    }

    private List<Pair<String, String>> getElectionText() {
        return getTextAndTitle("election.json");
    }

    private List<Pair<String, String>> getAccidentText() {
        return getTextAndTitle("accident.json");
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

    private List<Pair<String, String>> getRejectTexts() {
        return getTextAndTitle("reject.json");
    }

    private String getInfoBoxs() {
        return getText("many_infobox.json");
    }
}
