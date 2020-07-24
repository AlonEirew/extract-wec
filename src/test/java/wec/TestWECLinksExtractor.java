package wec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.CorefSubType;
import data.RawElasticResult;
import data.WECMention;
import data.WikiNewsMention;
import org.junit.Assert;
import org.junit.Test;
import persistence.ElasticQueryApi;
import wec.extractors.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TestWECLinksExtractor {

    private static final Gson GSON = new Gson();

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
    public void testHasDateAndLocationExtractor() {
        AInfoboxExtractor extractor = new AInfoboxExtractor(null, null) {
            @Override
            public CorefSubType extract(String infobox, String title) {
                return null;
            }
        };

        List<AbstractMap.SimpleEntry<String, String>> pageTexts = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : pageTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            boolean ret = extractor.hasDateAndLocation(infoBox);
            Assert.assertTrue(ret);
            break;
        }

        String pageText = getWeddingText();
        String infoBox = WECLinksExtractor.extractPageInfoBox(pageText);
        boolean ret = extractor.hasDateAndLocation(infoBox);
        Assert.assertTrue(ret);
        System.out.println();

        final List<AbstractMap.SimpleEntry<String, String>> peopleText = getPeopleText();
        for(AbstractMap.SimpleEntry<String, String> text : peopleText) {
            infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            ret = extractor.hasDateAndLocation(infoBox);
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
        AInfoboxExtractor personExtractor = new PersonInfoboxExtractor();
        Configuration config = getConfigFile();

        ElasticQueryApi elasticQueryApi = new ElasticQueryApi(config);
        final String alan_turing = elasticQueryApi.getPageText("Alan Turing");
        final String infoBox = WECLinksExtractor.extractPageInfoBox(alan_turing);
        Assert.assertNotSame(CorefSubType.NA, personExtractor.extract(infoBox, ""));
        Assert.assertTrue(WECLinksExtractor.extractTypes(infoBox).isEmpty());

        final String sep_11 = elasticQueryApi.getPageText("September 11 attacks");
        Assert.assertSame(CorefSubType.NA, personExtractor.extract(sep_11, ""));
        Assert.assertFalse(WECLinksExtractor.extractTypes(sep_11).isEmpty());
    }

    @Test
    public void testIsPerson() {
        AInfoboxExtractor personExtractor = new PersonInfoboxExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> peopleText = getPeopleText();
        for(AbstractMap.SimpleEntry<String, String> text : peopleText) {
            CorefSubType ret = personExtractor.extract(text.getValue(), "");
            Assert.assertNotSame(CorefSubType.NA, ret);
        }
    }

    @Test
    public void testIsElection() {
        AInfoboxExtractor electionExtractor = new ElectionInfoboxExtractor();
        List<AbstractMap.SimpleEntry<String, String>> pageText = getElectionText();
        for(AbstractMap.SimpleEntry<String, String> text : pageText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = electionExtractor.extract(infoBox, text.getKey());
            Assert.assertNotSame(CorefSubType.NA, ret);
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
            CorefSubType ret = electionExtractor.extract(infoBox, text.getKey());
            Assert.assertSame(CorefSubType.NA, ret);
        }
    }

    @Test
    public void testAccident() {
        AInfoboxExtractor accidentExtractor = new AccidentInfoboxExtractor();
        List<AbstractMap.SimpleEntry<String, String>> pageTexts = getAccidentText();
        for(AbstractMap.SimpleEntry<String, String> text : pageTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = accidentExtractor.extract(infoBox, "");
            Assert.assertNotSame(CorefSubType.NA, ret);
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
            CorefSubType ret = accidentExtractor.extract(infoBox, "");
            Assert.assertSame(CorefSubType.NA, ret);
        }
    }

    @Test
    public void testSmallCompany() {
        AInfoboxExtractor companyExtractor = new CompanyInfoboxExtractor();
        String infoBox = WECLinksExtractor.extractPageInfoBox(getSmallCompanyText());
        CorefSubType ret = companyExtractor.extract(infoBox, "");
        Assert.assertSame(CorefSubType.NA, ret);
    }

    @Test
    public void testIsSport() {
        AInfoboxExtractor sportExtractor = new SportInfoboxExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> sportText = getSportText();
        for(AbstractMap.SimpleEntry<String, String> text : sportText) {
            CorefSubType ret = sportExtractor.extract(WECLinksExtractor.extractPageInfoBox(text.getValue()), text.getKey());
            Assert.assertNotSame(text.getKey(), CorefSubType.NA, ret);
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
            CorefSubType ret = sportExtractor.extract(infoBox, text.getKey());
            Assert.assertSame(text.getKey(), CorefSubType.NA, ret);
        }
    }

    @Test
    public void testIsAward() {
        AInfoboxExtractor awardExtractor = new AwardInfoboxExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> awardPair = getAwards();
        for(AbstractMap.SimpleEntry<String, String> pair : awardPair) {
            CorefSubType ret = awardExtractor.extract(WECLinksExtractor.extractPageInfoBox(pair.getValue()), pair.getKey());
            Assert.assertNotSame(CorefSubType.NA, ret);
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
            CorefSubType ret = awardExtractor.extract(WECLinksExtractor.extractPageInfoBox(pair.getValue()), pair.getKey());

            Assert.assertSame(CorefSubType.NA, ret);
        }
    }

    @Test
    public void testIsConcreteGeneralEvent() {
        AInfoboxExtractor generalExtractor = new GeneralEventInfoboxExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> newsPair = getConcreteGeneralTexts();
        for(AbstractMap.SimpleEntry<String, String> pair : newsPair) {
            CorefSubType ret = generalExtractor.extract(WECLinksExtractor.extractPageInfoBox(pair.getValue()), pair.getKey());
            Assert.assertNotSame(CorefSubType.NA, ret);
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
            CorefSubType ret = generalExtractor.extract(WECLinksExtractor.extractPageInfoBox(pair.getValue()), pair.getKey());
            Assert.assertSame(CorefSubType.NA, ret);
        }
    }

    @Test
    public void testIsDisaster() {
        AInfoboxExtractor disasterExtractor = new DisasterInfoboxExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> disasterText = getDisasterText();
        for(AbstractMap.SimpleEntry<String, String> text : disasterText) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = disasterExtractor.extract(infoBox, "");
            Assert.assertNotSame(text.getKey(), CorefSubType.NA, ret);
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
            CorefSubType ret = disasterExtractor.extract(infoBox, "");
            Assert.assertSame(text.getKey(), CorefSubType.NA, ret);
        }
    }

    @Test
    public void testIsCivilAttack() {
        AInfoboxExtractor attackExtractor = new AttackInfoboxExtractor();
        final List<AbstractMap.SimpleEntry<String, String>> civilAttack = getCivilAttack();
        for(AbstractMap.SimpleEntry<String, String> text : civilAttack) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(text.getValue());
            CorefSubType ret = attackExtractor.extract(infoBox, "");
            Assert.assertNotSame(text.getKey(), CorefSubType.NA, ret);
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
            CorefSubType ret = attackExtractor.extract(infoBox, "");
            Assert.assertSame(CorefSubType.NA, ret);
        }
    }

    @Test
    public void testReject() {
        AInfoboxExtractor disasterInfoboxExtractor = new DisasterInfoboxExtractor();
        AInfoboxExtractor attackInfoboxExtractor = new AttackInfoboxExtractor();
        AInfoboxExtractor accidentInfoboxExtractor = new AccidentInfoboxExtractor();
        AInfoboxExtractor awardInfoboxExtractor = new AwardInfoboxExtractor();
        AInfoboxExtractor generalEventInfoboxExtractor = new GeneralEventInfoboxExtractor();
        AInfoboxExtractor sportInfoboxExtractor = new SportInfoboxExtractor();
        AInfoboxExtractor electionInfoboxExtracor = new ElectionInfoboxExtractor();

        final List<AbstractMap.SimpleEntry<String, String>> rejectTexts = getRejectTexts();
        for(AbstractMap.SimpleEntry<String, String> pair : rejectTexts) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(pair.getValue());

            CorefSubType ret = disasterInfoboxExtractor.extract(infoBox, "");
            Assert.assertSame(pair.getKey(), CorefSubType.NA, ret);

            ret = awardInfoboxExtractor.extract(infoBox, pair.getKey());
            Assert.assertSame(pair.getKey(), CorefSubType.NA, ret);

            ret = generalEventInfoboxExtractor.extract(infoBox, pair.getKey());
            Assert.assertSame(pair.getKey(), CorefSubType.NA, ret);

            ret = accidentInfoboxExtractor.extract(infoBox, "");
            Assert.assertSame(pair.getKey(), CorefSubType.NA, ret);

            ret = sportInfoboxExtractor.extract(infoBox, pair.getKey());
            Assert.assertSame(pair.getKey(), CorefSubType.NA, ret);

            ret = attackInfoboxExtractor.extract(infoBox, "");
            Assert.assertSame(pair.getKey(), CorefSubType.NA, ret);

            ret = electionInfoboxExtracor.extract(infoBox, pair.getKey());
            Assert.assertSame(pair.getKey(), CorefSubType.NA, ret);
        }
    }

    @Test
    public void testPersonOrEventFilter() {
        List<AInfoboxExtractor> extractors = new ArrayList<>();

        DisasterInfoboxExtractor disasterInfoboxExtractor = new DisasterInfoboxExtractor();
        AttackInfoboxExtractor attackInfoboxExtractor = new AttackInfoboxExtractor();
        AccidentInfoboxExtractor accidentInfoboxExtractor = new AccidentInfoboxExtractor();
        AwardInfoboxExtractor awardInfoboxExtractor = new AwardInfoboxExtractor();
        GeneralEventInfoboxExtractor generalEventInfoboxExtractor = new GeneralEventInfoboxExtractor();

        extractors.add(disasterInfoboxExtractor);
        extractors.add(attackInfoboxExtractor);
        extractors.add(accidentInfoboxExtractor);
        extractors.add(awardInfoboxExtractor);
        extractors.add(generalEventInfoboxExtractor);
        PersonOrEventFilter filter = new PersonOrEventFilter(extractors);


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
