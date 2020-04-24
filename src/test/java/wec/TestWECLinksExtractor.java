package wec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.CorefSubType;
import data.RawElasticResult;
import data.WECMention;
import data.WikiNewsMention;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import persistence.ElasticQueryApi;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        List<AbstractMap.SimpleEntry<String, String>> pageTexts = TestUtils.getTextAndTitle("wiki_links/tmp.json");
        for(AbstractMap.SimpleEntry<String, String> text : pageTexts) {
            final List<WikiNewsMention> wikiNewsMentions = WECLinksExtractor.extractFromWikiNews(text.getKey(), text.getValue());
            System.out.println();
        }
    }

    @Test
    public void testExtractTypes() {
        List<AbstractMap.SimpleEntry<String, String>> pageTexts = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : pageTexts) {
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

        List<AbstractMap.SimpleEntry<String, String>> pageTexts = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : pageTexts) {
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

        final List<AbstractMap.SimpleEntry<String, String>> peopleText = getPeopleText();
        for(AbstractMap.SimpleEntry<String, String> text : peopleText) {
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
        Assert.assertNotSame(WECLinksExtractor.isPerson(infoBox), CorefSubType.NA);
        Assert.assertTrue(WECLinksExtractor.extractTypes(infoBox).isEmpty());

        final String sep_11 = elasticQueryApi.getPageText("September 11 attacks");
        Assert.assertSame(WECLinksExtractor.isPerson(sep_11), CorefSubType.NA);
        Assert.assertTrue(!WECLinksExtractor.extractTypes(sep_11).isEmpty());
    }

    @Test
    public void testIsPerson() {
        final List<AbstractMap.SimpleEntry<String, String>> peopleText = getPeopleText();
        for(AbstractMap.SimpleEntry<String, String> text : peopleText) {
            CorefSubType ret = WECLinksExtractor.isPerson(text.getValue());
            Assert.assertNotSame(ret, CorefSubType.NA);
        }
    }

    @Test
    public void testIsElection() {
        List<AbstractMap.SimpleEntry<String, String>> pageText = getElectionText();
        for(AbstractMap.SimpleEntry<String, String> text : pageText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isElection(infoBox, text.getKey());
            Assert.assertNotSame(ret, CorefSubType.NA);
        }

        List<AbstractMap.SimpleEntry<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getAccidentText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getPeopleText());

         for(AbstractMap.SimpleEntry<String, String> text : other) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isElection(infoBox, text.getKey());
            Assert.assertSame(ret, CorefSubType.NA);
        }
    }

    @Test
    public void testAccident() {
        List<AbstractMap.SimpleEntry<String, String>> pageTexts = getAccidentText();
        for(AbstractMap.SimpleEntry<String, String> text : pageTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isAccident(infoBox);
            Assert.assertNotSame(ret, CorefSubType.NA);
        }

        List<AbstractMap.SimpleEntry<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getPeopleText());

        for(AbstractMap.SimpleEntry<String, String> text : other) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isAccident(infoBox);
            Assert.assertSame(ret, CorefSubType.NA);
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

        final List<AbstractMap.SimpleEntry<String, String>> sportText = getSportText();
        for(AbstractMap.SimpleEntry<String, String> text : sportText) {
            CorefSubType ret = WECLinksExtractor.isSportEvent(WECLinksExtractor.extractPageInfoBox(text.getValue()), text.getKey());
            Assert.assertNotSame(text.getKey(), ret, CorefSubType.NA);
        }

        List<AbstractMap.SimpleEntry<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getPeopleText());

        for(AbstractMap.SimpleEntry<String, String> text : other) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isSportEvent(infoBox, text.getKey());
            Assert.assertSame(text.getKey(), ret, CorefSubType.NA);
        }
    }

    @Test
    public void testIsAward() {

        final List<AbstractMap.SimpleEntry<String, String>> awardPair = getAwards();
        for(AbstractMap.SimpleEntry<String, String> pair : awardPair) {
            CorefSubType ret = WECLinksExtractor.isAwardEvent(WECLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertNotSame(ret, CorefSubType.NA);
        }

        List<AbstractMap.SimpleEntry<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getPeopleText());

        for(AbstractMap.SimpleEntry<String, String> pair : other) {
            CorefSubType ret = WECLinksExtractor.isAwardEvent(WECLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertSame(ret, CorefSubType.NA);
        }
    }

    @Test
    public void testIsConcreteGeneralEvent() {

        final List<AbstractMap.SimpleEntry<String, String>> newsPair = getConcreteGeneralTexts();
        for(AbstractMap.SimpleEntry<String, String> pair : newsPair) {
            CorefSubType ret = WECLinksExtractor.isConcreteGeneralEvent(WECLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertNotSame(ret, CorefSubType.NA);
        }

        List<AbstractMap.SimpleEntry<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(AbstractMap.SimpleEntry<String, String> pair : other) {
            CorefSubType ret = WECLinksExtractor.isConcreteGeneralEvent(WECLinksExtractor.extractPageInfoBox(pair.getValue()),
                    pair.getKey());

            Assert.assertSame(ret, CorefSubType.NA);
        }
    }

    @Test
    public void testIsDisaster() {
        final List<AbstractMap.SimpleEntry<String, String>> disasterText = getDisasterText();
        for(AbstractMap.SimpleEntry<String, String> text : disasterText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isDisaster(infoBox);
            Assert.assertNotSame(text.getKey(), ret, CorefSubType.NA);
        }

        List<AbstractMap.SimpleEntry<String, String>> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(AbstractMap.SimpleEntry<String, String> text : other) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isDisaster(infoBox);
            Assert.assertSame(text.getKey(), ret, CorefSubType.NA);
        }
    }

    @Test
    public void testIsCivilAttack() {
        final List<AbstractMap.SimpleEntry<String, String>> civilAttack = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : civilAttack) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isCivilAttack(infoBox);
            Assert.assertNotSame(text.getKey(), ret, CorefSubType.NA);
        }

        List<AbstractMap.SimpleEntry<String, String>> other = new ArrayList<>();
        other.addAll(getDisasterText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(AbstractMap.SimpleEntry<String, String> text : other) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = WECLinksExtractor.isCivilAttack(infoBox);
            Assert.assertSame(ret, CorefSubType.NA);
        }
    }

    @Test
    public void testReject() {
        final List<AbstractMap.SimpleEntry<String, String>> rejectTexts = getRejectTexts();
        for(AbstractMap.SimpleEntry<String, String> pair : rejectTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(pair.getValue());

            CorefSubType ret = WECLinksExtractor.isDisaster(infoBox);
            Assert.assertSame(pair.getKey(), ret, CorefSubType.NA);

//            ret = WECLinksExtractor.isAwardEvent(infoBox, pair.getKey());
//            Assert.assertSame(pair.getKey(), ret, CorefSubType.NA);
//
//            ret = WECLinksExtractor.isConcreteGeneralEvent(infoBox, pair.getKey());
//            Assert.assertSame(pair.getKey(), ret, CorefSubType.NA);
//
//            ret = WECLinksExtractor.isAccident(infoBox);
//            Assert.assertSame(pair.getKey(), ret, CorefSubType.NA);
//
//            ret = WECLinksExtractor.isSportEvent(infoBox, pair.getKey());
//            Assert.assertSame(pair.getKey(), ret, CorefSubType.NA);

            ret = WECLinksExtractor.isCivilAttack(infoBox);
            Assert.assertSame(pair.getKey(), ret, CorefSubType.NA);

            ret = WECLinksExtractor.isElection(infoBox, pair.getKey());
            Assert.assertSame(pair.getKey(), ret, CorefSubType.NA);
        }
    }

    @Test
    public void testPersonOrEventFilter() {
        PersonOrEventFilter filter = new PersonOrEventFilter();


        final List<AbstractMap.SimpleEntry<String, String>> peopleText = getPeopleText();
        for(AbstractMap.SimpleEntry<String, String> text : peopleText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<AbstractMap.SimpleEntry<String, String>> stringList = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : stringList) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<AbstractMap.SimpleEntry<String, String>> accidentText = getAccidentText();
        for(AbstractMap.SimpleEntry<String, String> text : accidentText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(filter.isConditionMet(input));
        }

        final List<AbstractMap.SimpleEntry<String, String>> disasterText = getDisasterText();
        for(AbstractMap.SimpleEntry<String, String> text : disasterText) {
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

    private List<AbstractMap.SimpleEntry<String, String>> getSportText() {
        return TestUtils.getTextAndTitle("wiki_links/sport.json");
    }

    private List<AbstractMap.SimpleEntry<String, String>> getDisasterText() {
        return TestUtils.getTextAndTitle("wiki_links/disaster.json");
    }

    private List<AbstractMap.SimpleEntry<String, String>> getCivilAttack() {
        return TestUtils.getTextAndTitle("wiki_links/civil_attack.json");
    }

    private List<AbstractMap.SimpleEntry<String, String>> getPeopleText() {
        return TestUtils.getTextAndTitle("wiki_links/people.json");
    }

    private String getWeddingText() {
        return TestUtils.getText("wiki_links/wedding.json");
    }

    private List<AbstractMap.SimpleEntry<String, String>> getElectionText() {
        return TestUtils.getTextAndTitle("wiki_links/election.json");
    }

    private List<AbstractMap.SimpleEntry<String, String>> getAccidentText() {
        return TestUtils.getTextAndTitle("wiki_links/accident.json");
    }

    private List<AbstractMap.SimpleEntry<String, String>> getAwards() {
        return TestUtils.getTextAndTitle("wiki_links/award.json");
    }

    private List<AbstractMap.SimpleEntry<String, String>> getConcreteGeneralTexts() {
        return TestUtils.getTextAndTitle("wiki_links/concrete_general.json");
    }

    private List<JsonObject> getExtractFromPage() {
        return TestUtils.getTextTitleAndExpected("wiki_links/extractfrompage.json");
    }

    private List<AbstractMap.SimpleEntry<String, String>> getRejectTexts() {
        return TestUtils.getTextAndTitle("wiki_links/reject.json");
    }

    private String getInfoBoxs() {
        return TestUtils.getText("wiki_links/many_infobox.json");
    }
}
