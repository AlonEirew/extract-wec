package wec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wec.config.Configuration;
import wec.config.InfoboxConfiguration;
import wec.data.RawElasticResult;
import wec.data.WECMention;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import wec.extractors.WikipediaLinkExtractor;
import wec.filters.InfoboxFilter;
import wec.validators.DefaultInfoboxValidator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestWikipediaLinkExtractor {

    private static final Gson GSON = new Gson();

    private InfoboxConfiguration infoboxConfiguration;
    private InfoboxFilter filter;
    private WikipediaLinkExtractor extractor = new WikipediaLinkExtractor();

    @Before
    public void initTest() throws FileNotFoundException {
        String inputStreamNlp = Objects.requireNonNull(TestWikipediaLinkExtractor.class.getClassLoader()
                .getResource("en_infobox_config.json")).getFile();

        infoboxConfiguration = GSON.fromJson(new FileReader(inputStreamNlp), InfoboxConfiguration.class);
        filter = new InfoboxFilter(infoboxConfiguration);
    }

    @Test
    public void testExtract() {

        final List<JsonObject> extractFromPage = getExtractFromPage();
        for(JsonObject jo : extractFromPage) {
            String pageText = jo.get("text").getAsString();
            String title = jo.get("title").getAsString();
            String infobox = jo.get("infobox").getAsString();
            int expected = jo.get("expected").getAsInt();
            RawElasticResult rawElasticResult = new RawElasticResult(title, pageText, infobox);
            List<WECMention> extractMentions1 = new ArrayList<>();
            if(infobox != null && !infobox.isEmpty()) {
                extractMentions1 = extractor.extract(rawElasticResult);
            }
            Assert.assertEquals(title, expected, extractMentions1.size());
        }
    }

//    @Test
//    public void testHasDateAndLocationExtractor() {
//        DefaultInfoboxValidator extractor = new DefaultInfoboxValidator(null, null);
//
//        List<RawElasticResult> pageTexts = getCivilAttack();
//        for(RawElasticResult text : pageTexts) {
//            String infoBox = text.getInfobox();
//            boolean ret = extractor.hasDateAndLocation(infoBox);
//            Assert.assertTrue(text.getTitle(), ret);
//            break;
//        }
//
//        final List<RawElasticResult> peopleText = getPeopleText();
//        for(RawElasticResult text : peopleText) {
//            String infoBox = text.getInfobox();
//            boolean ret = extractor.hasDateAndLocation(infoBox);
//            Assert.assertFalse(text.getTitle(), ret);
//        }
//    }

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
        DefaultInfoboxValidator personExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        final List<RawElasticResult> peopleText = getPeopleText();
        for(RawElasticResult text : peopleText) {
            String ret = personExtractor.validateMatchedInfobox(text.getInfobox(), "");
            Assert.assertNotSame(DefaultInfoboxValidator.NA, ret);
        }
    }

    @Test
    public void testIsElection() {
        String corefType = "ELECTION_EVENT";
        DefaultInfoboxValidator electionExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        List<RawElasticResult> pageText = getElectionText();
        for(RawElasticResult text : pageText) {
            String infoBox = text.getInfobox();
            String ret = electionExtractor.validateMatchedInfobox(infoBox, text.getTitle());
            Assert.assertNotSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }

        List<RawElasticResult> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getAccidentText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getOthersTexts());
        other.addAll(getPeopleText());

         for(RawElasticResult text : other) {
            String infoBox = text.getInfobox();
            String ret = electionExtractor.validateMatchedInfobox(infoBox, text.getTitle());
            Assert.assertSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }
    }

    @Test
    public void testAccident() {
        String corefType = "ACCIDENT_EVENT";
        DefaultInfoboxValidator accidentExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        List<RawElasticResult> pageTexts = getAccidentText();
        for(RawElasticResult text : pageTexts) {
            String infoBox = text.getInfobox();
            String ret = accidentExtractor.validateMatchedInfobox(infoBox, "");
            Assert.assertNotSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }

        List<RawElasticResult> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getOthersTexts());
        other.addAll(getPeopleText());

        for(RawElasticResult text : other) {
            String infoBox = text.getInfobox();
            String ret = accidentExtractor.validateMatchedInfobox(infoBox, "");
            Assert.assertSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }
    }

    @Test
    public void testSmallCompany() {
        String corefType = "COMPANY";
        DefaultInfoboxValidator companyExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        RawElasticResult smallCompanyText = getSmallCompanyText();
        String ret = companyExtractor.validateMatchedInfobox(smallCompanyText.getInfobox(), "");
        Assert.assertNotSame(smallCompanyText.getTitle(), DefaultInfoboxValidator.NA, ret);
    }

    @Test
    public void testIsSport() {
        String corefType = "SPORT_EVENT";
        DefaultInfoboxValidator sportExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        final List<RawElasticResult> sportText = getSportText();
        for(RawElasticResult text : sportText) {
            String ret = sportExtractor.validateMatchedInfobox(text.getInfobox(), text.getTitle());
            Assert.assertNotSame(text.getText(), DefaultInfoboxValidator.NA, ret);
        }

        List<RawElasticResult> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getAwards());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getOthersTexts());
        other.addAll(getPeopleText());

        for(RawElasticResult text : other) {
            String infoBox = text.getInfobox();
            String ret = sportExtractor.validateMatchedInfobox(infoBox, text.getTitle());
            Assert.assertSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }
    }

    @Test
    public void testIsAward() {
        String corefType = "AWARD_EVENT";
        DefaultInfoboxValidator awardExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        final List<RawElasticResult> awardPair = getAwards();
        for(RawElasticResult pair : awardPair) {
            String ret = awardExtractor.validateMatchedInfobox(pair.getInfobox(), pair.getTitle());
            Assert.assertNotSame(pair.getTitle(), DefaultInfoboxValidator.NA, ret);
        }

        List<RawElasticResult> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getOthersTexts());
        other.addAll(getPeopleText());

        for(RawElasticResult pair : other) {
            String ret = awardExtractor.validateMatchedInfobox(pair.getInfobox(), pair.getTitle());
            Assert.assertSame(pair.getTitle(), DefaultInfoboxValidator.NA, ret);
        }
    }

    @Test
    public void testIsConcreteGeneralEvent() {
        String generalEvent = "GENERAL_EVENT";
        String otherEvent = "OTHERS";
        DefaultInfoboxValidator generalExtractor = infoboxConfiguration.getExtractorByCorefType(generalEvent);
        final List<RawElasticResult> newsPair = getConcreteGeneralTexts();
        for(RawElasticResult pair : newsPair) {
            String ret1 = generalExtractor.validateMatchedInfobox(pair.getInfobox(), pair.getTitle());
            Assert.assertNotSame(pair.getTitle(), DefaultInfoboxValidator.NA, ret1);
        }

        DefaultInfoboxValidator otherExtractor = infoboxConfiguration.getExtractorByCorefType(otherEvent);
        final List<RawElasticResult> othersEvents = getOthersTexts();
        for(RawElasticResult pair : othersEvents) {
            String ret2 = otherExtractor.validateMatchedInfobox(pair.getInfobox(), pair.getTitle());
            Assert.assertNotSame(pair.getTitle(), DefaultInfoboxValidator.NA, ret2);
        }

        List<RawElasticResult> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getDisasterText());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(RawElasticResult pair : other) {
            String ret1 = generalExtractor.validateMatchedInfobox(pair.getInfobox(), pair.getTitle());
            String ret2 = otherExtractor.validateMatchedInfobox(pair.getInfobox(), pair.getTitle());
            Assert.assertSame(pair.getTitle(), DefaultInfoboxValidator.NA, ret1);
            Assert.assertSame(pair.getTitle(), DefaultInfoboxValidator.NA, ret2);
        }
    }

    @Test
    public void testIsDisaster() {
        String corefType = "DISASTER_EVENT";
        DefaultInfoboxValidator disasterExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        final List<RawElasticResult> disasterText = getDisasterText();
        for(RawElasticResult text : disasterText) {
            String infoBox = text.getInfobox();
            String ret = disasterExtractor.validateMatchedInfobox(infoBox, "");
            Assert.assertNotSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }

        List<RawElasticResult> other = new ArrayList<>();
        other.addAll(getCivilAttack());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getOthersTexts());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(RawElasticResult text : other) {
            String infoBox = text.getInfobox();
            String ret = disasterExtractor.validateMatchedInfobox(infoBox, "");
            Assert.assertSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }
    }

    @Test
    public void testIsCivilAttack() {
        String corefType = "ATTACK_EVENT";
        DefaultInfoboxValidator attackExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        final List<RawElasticResult> civilAttack = getCivilAttack();
        for(RawElasticResult text : civilAttack) {
            String infoBox = text.getInfobox();
            String ret = attackExtractor.validateMatchedInfobox(infoBox, "");
            Assert.assertNotSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }

        List<RawElasticResult> other = new ArrayList<>();
        other.addAll(getTimeFullPages());
        other.addAll(getDisasterText());
        other.addAll(getSportText());
        other.addAll(getConcreteGeneralTexts());
        other.addAll(getOthersTexts());
        other.addAll(getElectionText());
        other.addAll(getAccidentText());
        other.addAll(getAwards());
        other.addAll(getPeopleText());

        for(RawElasticResult text : other) {
            String infoBox = text.getInfobox();
            String ret = attackExtractor.validateMatchedInfobox(infoBox, "");
            Assert.assertSame(text.getTitle(), DefaultInfoboxValidator.NA, ret);
        }
    }

    private List<RawElasticResult> getTimeFullPages() {
        return TestUtils.getTextAndTitle("time/page_time_extract.json");
    }

    @Test
    public void testReject() {
        final List<RawElasticResult> rejectTexts = getRejectTexts();
        for(RawElasticResult pair : rejectTexts) {
            String infoBox = pair.getInfobox();
            for (DefaultInfoboxValidator extractor : infoboxConfiguration.getAllIncludedValidators()) {
                String ret = extractor.validateMatchedInfobox(infoBox, pair.getTitle());
                Assert.assertSame(pair.getTitle(), DefaultInfoboxValidator.NA, ret);
            }
        }
    }

    @Test
    public void testPersonOrEventFilter() {
        InfoboxFilter filter = new InfoboxFilter(infoboxConfiguration);

        final List<RawElasticResult> peopleText = getPeopleText();
        for(RawElasticResult text : peopleText) {
            Assert.assertFalse(text.getTitle(), filter.isConditionMet(text));
        }

        final List<RawElasticResult> stringList = getCivilAttack();
        for(RawElasticResult text : stringList) {
            Assert.assertTrue(text.getTitle(), filter.isConditionMet(text));
        }

        final List<RawElasticResult> accidentText = getAccidentText();
        for(RawElasticResult text : accidentText) {
            Assert.assertTrue(text.getTitle(), filter.isConditionMet(text));
        }

        final List<RawElasticResult> disasterText = getDisasterText();
        for(RawElasticResult text : disasterText) {
            Assert.assertTrue(text.getTitle(), filter.isConditionMet(text));
        }
    }

    private Configuration getConfigFile() throws IOException {
        final String property = System.getProperty("user.dir");
        return GSON.fromJson(new FileReader(property + "/config.json"), Configuration.class);
    }

    private RawElasticResult getSmallCompanyText() {
        return TestUtils.getText("wikipedia/mobileye.json");
    }

    private List<RawElasticResult> getSportText() {
        return TestUtils.getTextAndTitle("wikipedia/sport.json");
    }

    private List<RawElasticResult> getDisasterText() {
        return TestUtils.getTextAndTitle("wikipedia/disaster.json");
    }

    private List<RawElasticResult> getCivilAttack() {
        return TestUtils.getTextAndTitle("wikipedia/civil_attack.json");
    }

    private List<RawElasticResult> getPeopleText() {
        return TestUtils.getTextAndTitle("wikipedia/people.json");
    }

    private RawElasticResult getWeddingText() {
        return TestUtils.getText("wikipedia/wedding.json");
    }

    private List<RawElasticResult> getElectionText() {
        return TestUtils.getTextAndTitle("wikipedia/election.json");
    }

    private List<RawElasticResult> getAccidentText() {
        return TestUtils.getTextAndTitle("wikipedia/accident.json");
    }

    private List<RawElasticResult> getAwards() {
        return TestUtils.getTextAndTitle("wikipedia/award.json");
    }

    private List<RawElasticResult> getConcreteGeneralTexts() {
        return TestUtils.getTextAndTitle("wikipedia/concrete_general.json");
    }

    private List<RawElasticResult> getOthersTexts() {
        return TestUtils.getTextAndTitle("wikipedia/others.json");
    }

    private List<JsonObject> getExtractFromPage() {
        return TestUtils.getTextTitleAndExpected("wikipedia/extractfrompage.json");
    }

    private List<RawElasticResult> getRejectTexts() {
        return TestUtils.getTextAndTitle("wikipedia/reject.json");
    }

    private RawElasticResult getInfoBoxs() {
        return TestUtils.getText("wikipedia/many_infobox.json");
    }

}
