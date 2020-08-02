package wec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestWECLinksExtractor {

    private static final Gson GSON = new Gson();

    private InfoboxConfiguration infoboxConfiguration;

    @Before
    public void initTest() throws FileNotFoundException {
        String inputStreamNlp = Objects.requireNonNull(TestWECLinksExtractor.class.getClassLoader()
                .getResource("en_infobox_config.json")).getFile();

        infoboxConfiguration = GSON.fromJson(new FileReader(inputStreamNlp), InfoboxConfiguration.class);
    }

    @Test
    public void testExtract() {

        final List<JsonObject> extractFromPage = getExtractFromPage();
        for(JsonObject jo : extractFromPage) {
            String pageText = jo.get("text").getAsString();
            String title = jo.get("title").getAsString();
            int expected = jo.get("expected").getAsInt();
            RawElasticResult rawElasticResult = new RawElasticResult(title, pageText);
            final List<WECMention> extractMentions1 = WECLinksExtractor.extractFromWikipedia(rawElasticResult);
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
    public void testInfoBoxExtract() {
        String pageText = getInfoBoxs();
        final String infoBoxs = WECLinksExtractor.extractPageInfoBox(pageText);
        System.out.println(infoBoxs);
    }

    @Test
    public void testHasDateAndLocationExtractor() {
        DefaultInfoboxExtractor extractor = new DefaultInfoboxExtractor(null, null);

        List<AbstractMap.SimpleEntry<String, String>> pageTexts = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : pageTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = extractor.hasDateAndLocation(infoBox);
            Assert.assertTrue(text.getKey(), ret);
            break;
        }

        final List<AbstractMap.SimpleEntry<String, String>> peopleText = getPeopleText();
        for(AbstractMap.SimpleEntry<String, String> text : peopleText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = extractor.hasDateAndLocation(infoBox);
            Assert.assertFalse(text.getKey(), ret);
        }
    }

//    @Test // Removed test as it require running elastic search to pass
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
    public void testIsPerson() {
        String corefType = "PERSON";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor personExtractor = infoboxConfig.getExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> peopleText = getPeopleText();
        for(AbstractMap.SimpleEntry<String, String> text : peopleText) {
            String ret = personExtractor.extractMatchedInfobox(text.getValue(), "");
            Assert.assertNotSame(DefaultInfoboxExtractor.NA, ret);
        }
    }

    @Test
    public void testIsElection() {
        String corefType = "ELECTION_EVENT";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor electionExtractor = infoboxConfig.getExtractor();
        List<AbstractMap.SimpleEntry<String, String>> pageText = getElectionText();
        for(AbstractMap.SimpleEntry<String, String> text : pageText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            String ret = electionExtractor.extractMatchedInfobox(infoBox, text.getKey());
            Assert.assertNotSame(text.getKey(), DefaultInfoboxExtractor.NA, ret);
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
            String ret = electionExtractor.extractMatchedInfobox(infoBox, text.getKey());
            Assert.assertSame(text.getKey(), DefaultInfoboxExtractor.NA, ret);
        }
    }

    @Test
    public void testAccident() {
        String corefType = "ACCIDENT_EVENT";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor accidentExtractor = infoboxConfig.getExtractor();
        List<AbstractMap.SimpleEntry<String, String>> pageTexts = getAccidentText();
        for(AbstractMap.SimpleEntry<String, String> text : pageTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            String ret = accidentExtractor.extractMatchedInfobox(infoBox, "");
            Assert.assertNotSame(DefaultInfoboxExtractor.NA, ret);
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
            String ret = accidentExtractor.extractMatchedInfobox(infoBox, "");
            Assert.assertSame(DefaultInfoboxExtractor.NA, ret);
        }
    }

    @Test
    public void testSmallCompany() {
        String corefType = "COMPANY";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor companyExtractor = infoboxConfig.getExtractor();
        String smallCompanyText = getSmallCompanyText();
        String infoBox = WECLinksExtractor.extractPageInfoBox(smallCompanyText);
        String ret = companyExtractor.extractMatchedInfobox(infoBox, "");
        Assert.assertTrue(infoboxConfig.getInfoboxs().contains(ret));
    }

    @Test
    public void testIsSport() {
        String corefType = "SPORT_EVENT";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor sportExtractor = infoboxConfig.getExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> sportText = getSportText();
        for(AbstractMap.SimpleEntry<String, String> text : sportText) {
            String ret = sportExtractor.extractMatchedInfobox(WECLinksExtractor.extractPageInfoBox(text.getValue()), text.getKey());
            Assert.assertNotSame(text.getValue(), DefaultInfoboxExtractor.NA, ret);
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
            String ret = sportExtractor.extractMatchedInfobox(infoBox, text.getKey());
            Assert.assertSame(text.getKey(), DefaultInfoboxExtractor.NA, ret);
        }
    }

    @Test
    public void testIsAward() {
        String corefType = "AWARD_EVENT";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor awardExtractor = infoboxConfig.getExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> awardPair = getAwards();
        for(AbstractMap.SimpleEntry<String, String> pair : awardPair) {
            String ret = awardExtractor.extractMatchedInfobox(WECLinksExtractor.extractPageInfoBox(pair.getValue()), pair.getKey());
            Assert.assertNotSame(pair.getKey(), DefaultInfoboxExtractor.NA, ret);
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
            String ret = awardExtractor.extractMatchedInfobox(WECLinksExtractor.extractPageInfoBox(pair.getValue()), pair.getKey());
            Assert.assertSame(pair.getKey(), DefaultInfoboxExtractor.NA, ret);
        }
    }

    @Test
    public void testIsConcreteGeneralEvent() {
        String corefType = "GENERAL_EVENT";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor generalExtractor = infoboxConfig.getExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> newsPair = getConcreteGeneralTexts();
        for(AbstractMap.SimpleEntry<String, String> pair : newsPair) {
            String ret = generalExtractor.extractMatchedInfobox(WECLinksExtractor.extractPageInfoBox(pair.getValue()), pair.getKey());
            Assert.assertNotSame(pair.getKey(), DefaultInfoboxExtractor.NA, ret);
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
            String ret = generalExtractor.extractMatchedInfobox(WECLinksExtractor.extractPageInfoBox(pair.getValue()), pair.getKey());
            Assert.assertSame(pair.getKey(), DefaultInfoboxExtractor.NA, ret);
        }
    }

    @Test
    public void testIsDisaster() {
        String corefType = "DISASTER_EVENT";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor disasterExtractor = infoboxConfig.getExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> disasterText = getDisasterText();
        for(AbstractMap.SimpleEntry<String, String> text : disasterText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            String ret = disasterExtractor.extractMatchedInfobox(infoBox, "");
            Assert.assertNotSame(text.getKey(), DefaultInfoboxExtractor.NA, ret);
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
            String ret = disasterExtractor.extractMatchedInfobox(infoBox, "");
            Assert.assertSame(text.getKey(), DefaultInfoboxExtractor.NA, ret);
        }
    }

    @Test
    public void testIsCivilAttack() {
        String corefType = "ATTACK_EVENT";
        InfoboxConfiguration.InfoboxConfig infoboxConfig = getInfoboxConfig(infoboxConfiguration, corefType);
        assert infoboxConfig != null;
        DefaultInfoboxExtractor attackExtractor = infoboxConfig.getExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> civilAttack = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : civilAttack) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            String ret = attackExtractor.extractMatchedInfobox(infoBox, "");
            Assert.assertNotSame(text.getKey(), DefaultInfoboxExtractor.NA, ret);
        }

        List<AbstractMap.SimpleEntry<String, String>> other = new ArrayList<>();
        other.addAll(getTimeFullPages());
        other.addAll(getDisasterText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(AbstractMap.SimpleEntry<String, String> text : other) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            String ret = attackExtractor.extractMatchedInfobox(infoBox, "");
            Assert.assertSame(text.getKey(), DefaultInfoboxExtractor.NA, ret);
        }
    }

    private List<AbstractMap.SimpleEntry<String, String>> getTimeFullPages() {
        return TestUtils.getTextAndTitle("time/page_time_extract.json");
    }

    @Test
    public void testReject() {
        final List<AbstractMap.SimpleEntry<String, String>> rejectTexts = getRejectTexts();
        for(AbstractMap.SimpleEntry<String, String> pair : rejectTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(pair.getValue());
            for (InfoboxConfiguration.InfoboxConfig infoboxConfig : infoboxConfiguration.getInfoboxConfigs()) {
                String ret = infoboxConfig.getExtractor().extractMatchedInfobox(infoBox, pair.getKey());
                Assert.assertSame(pair.getKey(), DefaultInfoboxExtractor.NA, ret);
            }
        }
    }

    @Test
    public void testPersonOrEventFilter() {
        InfoboxFilter filter = new InfoboxFilter(infoboxConfiguration);

        final List<AbstractMap.SimpleEntry<String, String>> peopleText = getPeopleText();
        for(AbstractMap.SimpleEntry<String, String> text : peopleText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertFalse(text.getKey(), filter.isConditionMet(input));
        }

        final List<AbstractMap.SimpleEntry<String, String>> stringList = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : stringList) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertTrue(text.getKey(), filter.isConditionMet(input));
        }

        final List<AbstractMap.SimpleEntry<String, String>> accidentText = getAccidentText();
        for(AbstractMap.SimpleEntry<String, String> text : accidentText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertTrue(text.getKey(), filter.isConditionMet(input));
        }

        final List<AbstractMap.SimpleEntry<String, String>> disasterText = getDisasterText();
        for(AbstractMap.SimpleEntry<String, String> text : disasterText) {
            RawElasticResult input = new RawElasticResult(text.getKey(), WECLinksExtractor.extractPageInfoBox(text.getValue()));
            Assert.assertTrue(text.getKey(), filter.isConditionMet(input));
        }
    }

    private InfoboxConfiguration.InfoboxConfig getInfoboxConfig(InfoboxConfiguration configuration, String corefType) {
        for(InfoboxConfiguration.InfoboxConfig infoboxConfig : configuration.getInfoboxConfigs()) {
            if(infoboxConfig.getCorefType().equals(corefType)) {
                return infoboxConfig;
            }
        }

        return null;
    }

    private Configuration getConfigFile() throws IOException {
        final String property = System.getProperty("user.dir");
        return GSON.fromJson(new FileReader(property + "/config.json"), Configuration.class);
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
