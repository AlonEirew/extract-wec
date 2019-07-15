package wikilinks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.RawElasticResult;
import data.WikiLinksMention;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import persistence.ElasticQueryApi;
import utils.ExecutorServiceFactory;
import workers.ReadInfoBoxWorker;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TestWikiLinksExtractor {

    private Gson gson = new Gson();

    @Test
    public void testExtract() {

        final List<JsonObject> extractFromPage = getExtractFromPage();
        for(JsonObject jo : extractFromPage) {
            String pageText = jo.get("text").getAsString();
            String title = jo.get("title").getAsString();
            int expected = jo.get("expected").getAsInt();
            final List<WikiLinksMention> extractMentions1 = WikiLinksExtractor.extractFromWikipedia(title, pageText);
            Assert.assertEquals(title, expected, extractMentions1.size());
        }
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

        ExecutorServiceFactory.initExecutorService(Integer.parseInt(config.get("pool_size")));

        ElasticQueryApi elasticQueryApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")));

        Set<String> pagesList = new HashSet<>();
        pagesList.add("Alan Turing");
        pagesList.add("September 11 attacks");
        final Map<String, String> allPagesText = elasticQueryApi.getAllWikiPagesTitleAndText(pagesList);

        final String alan_turing = allPagesText.get("Alan Turing");
        Assert.assertTrue(WikiLinksExtractor.isPerson(alan_turing));
        Assert.assertTrue(WikiLinksExtractor.extractTypes(alan_turing).isEmpty());

        final String sep_11 = allPagesText.get("September 11 attacks");
        Assert.assertFalse(WikiLinksExtractor.isPerson(sep_11));
        Assert.assertTrue(!WikiLinksExtractor.extractTypes(sep_11).isEmpty());

        ExecutorServiceFactory.closeService();
    }

    @Test
    public void testGetPageText() throws IOException {
        Map<String, String> config = getConfigFile();

        ElasticQueryApi elasticQueryApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")));
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
            boolean ret = WikiLinksExtractor.isSportEvent(WikiLinksExtractor.extractPageInfoBox(text.getValue()), text.getKey());
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
            boolean ret = WikiLinksExtractor.isSportEvent(infoBox, text.getKey());
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
    public void testIsCivilAttack() {
        final List<Pair<String, String>> civilAttack = getCivilAttack();
        for(Pair<String, String> text : civilAttack) {
            String infoBox = WikiLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WikiLinksExtractor.isCivilAttack(infoBox);
            Assert.assertTrue(text.getKey(), ret);
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

            ret = WikiLinksExtractor.isSportEvent(infoBox, pair.getKey());
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

    private String getSmallCompanyText() {
        return TestUtils.getText("wiki_links/mobileye.json");
    }

    private List<Pair<String, String>> getSportText() {
        return TestUtils.getTextAndTitle("wiki_links/sport.json");
    }

    private List<Pair<String, String>> getDisasterText() {
        return TestUtils.getTextAndTitle("wiki_links/disaster.json");
    }

    private List<Pair<String, String>> getCivilAttack() {
        return TestUtils.getTextAndTitle("wiki_links/civil_attack.json");
    }

    private List<Pair<String, String>> getPeopleText() {
        return TestUtils.getTextAndTitle("wiki_links/people.json");
    }

    private String getWeddingText() {
        return TestUtils.getText("wiki_links/wedding.json");
    }

    private List<Pair<String, String>> getElectionText() {
        return TestUtils.getTextAndTitle("wiki_links/election.json");
    }

    private List<Pair<String, String>> getAccidentText() {
        return TestUtils.getTextAndTitle("wiki_links/accident.json");
    }

    private List<Pair<String, String>> getAwards() {
        return TestUtils.getTextAndTitle("wiki_links/award.json");
    }

    private List<Pair<String, String>> getConcreteGeneralTexts() {
        return TestUtils.getTextAndTitle("wiki_links/concrete_general.json");
    }

    private List<JsonObject> getExtractFromPage() {
        return TestUtils.getTextTitleAndExpected("wiki_links/extractfrompage.json");
    }

    private List<Pair<String, String>> getRejectTexts() {
        return TestUtils.getTextAndTitle("wiki_links/reject.json");
    }

    private String getInfoBoxs() {
        return TestUtils.getText("wiki_links/many_infobox.json");
    }
}
