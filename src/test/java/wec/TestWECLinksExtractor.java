package wec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.RawElasticResult;
import data.WECMention;
import data.WikiNewsMention;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import persistence.ElasticQueryApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestWECLinksExtractor {

    private Gson gson = new Gson();

    @Test
    public void testExtract() {

        final List<JsonObject> extractFromPage = getExtractFromPage();
        for(JsonObject jo : extractFromPage) {
            String pageText = jo.get("text").getAsString();
            String title = jo.get("title").getAsString();
            int expected = jo.get("expected").getAsInt();
            final List<WECMention> extractMentions1 = WECLinksExtractor.extractFromWikipedia(title, pageText);
            Assert.assertEquals(title, expected, extractMentions1.size());
        }
    }

    @Test
    public void testExtractTmp() {
        List<Pair<String, String>> pageTexts = TestUtils.getTextAndTitle("wiki_links/tmp.json");
        for(Pair<String, String> text : pageTexts) {
            final List<WikiNewsMention> wikiNewsMentions = WECLinksExtractor.extractFromWikiNews(text.getKey(), text.getValue());
            System.out.println();
        }
    }

    @Test
    public void testExtractTypes() {
        List<Pair<String, String>> pageTexts = getCivilAttack();
        for(Pair<String, String> text : pageTexts) {
            final Set<String> extractMentions = WECLinksExtractor.extractTypes(text.getValue());
            System.out.println();
        }
    }

    @Test
    public void testInfoBoxExtract() {
        String pageText = getInfoBoxs();
        final String infoBoxs = WECLinksExtractor.extractPageInfoBox(pageText);
        System.out.println(infoBoxs);
    }

    @Test
    public void testHasDateAndLocation() {

        List<Pair<String, String>> pageTexts = getCivilAttack();
        for(Pair<String, String> text : pageTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.hasDateAndLocation(infoBox);
            Assert.assertTrue(ret);
            break;
        }

        String pageText = getWeddingText();
        String infoBox = WECLinksExtractor.extractPageInfoBox(pageText);
        boolean ret = WECLinksExtractor.hasDateAndLocation(infoBox);
        Assert.assertTrue(ret);
        System.out.println();

        final List<Pair<String, String>> peopleText = getPeopleText();
        for(Pair<String, String> text : peopleText) {
            infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            ret = WECLinksExtractor.hasDateAndLocation(infoBox);
            Assert.assertFalse(ret);
        }
    }

//    @Test
//    public void testGetAllPagesTexts() throws IOException {
//        Map<String, String> config = getConfigFile();
//
//        ExecutorServiceFactory.initExecutorService(Integer.parseInt(config.get("pool_size")));
//
//        ElasticQueryApi elasticQueryApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
//                Integer.parseInt(config.get("elastic_search_interval")), config.get("elastic_host"),
//                Integer.parseInt(config.get("elastic_port")));
//
//        Set<String> pagesList = new HashSet<>();
//        pagesList.add("Alan Turing");
//        pagesList.add("September 11 attacks");
//        final Map<String, String> allPagesText = elasticQueryApi.getAllWikiPagesTitleAndTextAsync(pagesList);
//
//        final String alan_turing = allPagesText.get("Alan Turing");
//        Assert.assertTrue(WikiLinksExtractor.isPerson(alan_turing));
//        Assert.assertTrue(WikiLinksExtractor.extractTypes(alan_turing).isEmpty());
//
//        final String sep_11 = allPagesText.get("September 11 attacks");
//        Assert.assertFalse(WikiLinksExtractor.isPerson(sep_11));
//        Assert.assertTrue(!WikiLinksExtractor.extractTypes(sep_11).isEmpty());
//
//        ExecutorServiceFactory.closeService();
//    }

    @Test
    public void testGetPageText() throws IOException {
        Map<String, String> config = getConfigFile();

        ElasticQueryApi elasticQueryApi = new ElasticQueryApi(config.get("elastic_wiki_index"),
                Integer.parseInt(config.get("elastic_search_interval")),
                Integer.parseInt(config.get("multi_request_interval")),
                config.get("elastic_host"),
                Integer.parseInt(config.get("elastic_port")));
        final String alan_turing = elasticQueryApi.getPageText("Alan Turing");
        final String infoBox = WECLinksExtractor.extractPageInfoBox(alan_turing);
        Assert.assertTrue(WECLinksExtractor.isPerson(infoBox));
        Assert.assertTrue(WECLinksExtractor.extractTypes(infoBox).isEmpty());

        final String sep_11 = elasticQueryApi.getPageText("September 11 attacks");
        Assert.assertFalse(WECLinksExtractor.isPerson(sep_11));
        Assert.assertTrue(!WECLinksExtractor.extractTypes(sep_11).isEmpty());
    }

    @Test
    public void testIsPerson() {
        final List<Pair<String, String>> peopleText = getPeopleText();
        for(Pair<String, String> text : peopleText) {
            boolean ret = WECLinksExtractor.isPerson(text.getValue());
            Assert.assertTrue(ret);
        }
    }

    @Test
    public void testIsElection() {
        List<Pair<String, String>> pageText = getElectionText();
        for(Pair<String, String> text : pageText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isElection(infoBox, text.getKey());
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
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isElection(infoBox, text.getKey());
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testAccident() {
        List<Pair<String, String>> pageTexts = getAccidentText();
        for(Pair<String, String> text : pageTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isAccident(infoBox);
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
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isAccident(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testSmallCompany() {
        String infoBox = WECLinksExtractor.extractPageInfoBox(getSmallCompanyText());
        boolean ret = WECLinksExtractor.isSmallCompanyEvent(infoBox);
        Assert.assertTrue(ret);
    }

    @Test
    public void testIsSport() {

        final List<Pair<String, String>> sportText = getSportText();
        for(Pair<String, String> text : sportText) {
            boolean ret = WECLinksExtractor.isSportEvent(WECLinksExtractor.extractPageInfoBox(text.getValue()), text.getKey());
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
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isSportEvent(infoBox, text.getKey());
            Assert.assertFalse(text.getKey(), ret);
        }
    }

    @Test
    public void testIsAward() {

        final List<Pair<String, String>> awardPair = getAwards();
        for(Pair<String, String> pair : awardPair) {
            boolean ret = WECLinksExtractor.isAwardEvent(WECLinksExtractor.extractPageInfoBox(pair.getValue()),
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
            boolean ret = WECLinksExtractor.isAwardEvent(WECLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testIsConcreteGeneralEvent() {

        final List<Pair<String, String>> newsPair = getConcreteGeneralTexts();
        for(Pair<String, String> pair : newsPair) {
            boolean ret = WECLinksExtractor.isConcreteGeneralEvent(WECLinksExtractor.extractPageInfoBox(pair.getValue()),
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
            boolean ret = WECLinksExtractor.isConcreteGeneralEvent(WECLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testIsDisaster() {
        final List<Pair<String, String>> disasterText = getDisasterText();
        for(Pair<String, String> text : disasterText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isDisaster(infoBox);
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
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isDisaster(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testIsCivilAttack() {
        final List<Pair<String, String>> civilAttack = getCivilAttack();
        for(Pair<String, String> text : civilAttack) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isCivilAttack(infoBox);
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
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = WECLinksExtractor.isCivilAttack(infoBox);
            Assert.assertFalse(ret);
        }
    }

    @Test
    public void testReject() {
        final List<Pair<String, String>> rejectTexts = getRejectTexts();
        for(Pair<String, String> pair : rejectTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(pair.getValue());

            boolean ret = WECLinksExtractor.isDisaster(infoBox);
            Assert.assertFalse(pair.getKey(), ret);

            ret = WECLinksExtractor.isAwardEvent(infoBox, pair.getKey());
            Assert.assertFalse(pair.getKey(), ret);

            ret = WECLinksExtractor.isConcreteGeneralEvent(infoBox, pair.getKey());
            Assert.assertFalse(pair.getKey(), ret);

            ret = WECLinksExtractor.isAccident(infoBox);
            Assert.assertFalse(pair.getKey(), ret);

            ret = WECLinksExtractor.isSportEvent(infoBox, pair.getKey());
            Assert.assertFalse(pair.getKey(), ret);

            ret = WECLinksExtractor.isCivilAttack(infoBox);
            Assert.assertFalse(pair.getKey(), ret);

            ret = WECLinksExtractor.isElection(infoBox, pair.getKey());
            Assert.assertFalse(pair.getKey(), ret);
        }
    }

    @Test
    public void testPersonOrEventFilter() {
        PersonOrEventFilter filter = new PersonOrEventFilter();


        final List<Pair<String, String>> peopleText = getPeopleText();
        for(Pair<String, String> text : peopleText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<Pair<String, String>> stringList = getCivilAttack();
        for(Pair<String, String> text : stringList) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<Pair<String, String>> accidentText = getAccidentText();
        for(Pair<String, String> text : accidentText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<Pair<String, String>> disasterText = getDisasterText();
        for(Pair<String, String> text : disasterText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
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
