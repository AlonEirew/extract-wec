package workers;

import com.google.gson.Gson;
import data.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import wec.InfoboxFilter;
import wec.TestUtils;
import wec.TestWikipediaLinkExtractor;
import wec.validators.DefaultInfoboxValidator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class TestParseAndExtractMentionWorker {

    private static final Gson GSON = new Gson();

    private InfoboxConfiguration infoboxConfiguration;
    private InfoboxFilter filter;

    @Before
    public void initTest() throws FileNotFoundException {
        String config_file = Objects.requireNonNull(TestWikipediaLinkExtractor.class.getClassLoader()
                .getResource("test_conf.json")).getFile();

        Configuration config = GSON.fromJson(new FileReader(config_file), Configuration.class);
        WECResources.setSqlApi(new SQLQueryApi(new SQLiteConnections(config.getSqlConnectionUrl())));
        WECResources.setElasticApi(new ElasticQueryApi(config));

        String inputStreamNlp = Objects.requireNonNull(TestWikipediaLinkExtractor.class.getClassLoader()
                .getResource("en_infobox_config.json")).getFile();

        infoboxConfiguration = GSON.fromJson(new FileReader(inputStreamNlp), InfoboxConfiguration.class);
        filter = new InfoboxFilter(infoboxConfiguration);
    }

    @Test
    public void testFilterUnwantedMentions() throws FileNotFoundException {
        WECMention mention1 = new WECMention();
        WECMention mention2 = new WECMention();
        WECMention mention3 = new WECMention();
        WECMention mention4 = new WECMention();
        List<WECMention> mentions = new ArrayList<>();
        mentions.add(mention1);
        mentions.add(mention2);
        mentions.add(mention3);
        mentions.add(mention4);

        // Mention SHOULD be filtered
        WECCoref crs1 = WECCoref.getAndSetIfNotExist("TEST1");
        crs1.setWasAlreadyRetrived(true);
        crs1.setMarkedForRemoval(true);
        mention1.setCorefChain(crs1);

        // Mention SHOULD NOT be filtered
        WECCoref crs2 = WECCoref.getAndSetIfNotExist("TEST2");
        crs2.setWasAlreadyRetrived(true);
        crs2.setMarkedForRemoval(false);
        mention2.setCorefChain(crs2);

        // Mention SHOULD NOT be filtered (MAP with valid key)
        WECCoref crs3 = WECCoref.getAndSetIfNotExist("TEST3");
        crs3.setWasAlreadyRetrived(false);
        mention3.setCorefChain(crs3);

        // Mention SHOULD be filtered (MAP with invalid key)
        WECCoref crs4 = WECCoref.getAndSetIfNotExist("TEST4");
        crs4.setWasAlreadyRetrived(false);
        mention4.setCorefChain(crs4);

        String inputStreamNlp = Objects.requireNonNull(TestWikipediaLinkExtractor.class.getClassLoader()
                .getResource("en_infobox_config.json")).getFile();

        InfoboxConfiguration infoboxConfiguration = GSON.fromJson(new FileReader(inputStreamNlp), InfoboxConfiguration.class);

        ParseAndExtractMentionsWorker worker = new ParseAndExtractMentionsWorker(
                new InfoboxFilter(infoboxConfiguration));

        Map<String, RawElasticResult> testMap = new HashMap<>();
        testMap.put("TEST3", new RawElasticResult("TEST3", "", "{{Infobox earthquake"));
        testMap.put("TEST4", new RawElasticResult("TEST4", "", "{Infobox tofilter}"));

        final List<WECMention> finalToCommit = worker.filterUnwantedMentions(mentions, testMap);

        Assert.assertEquals(1, finalToCommit.size());
        for(WECMention mention : finalToCommit) {
            if(!mention.getCorefChain().getCorefValue().equals("TEST2") &&
                    !mention.getCorefChain().getCorefValue().equals("TEST3")) {
                Assert.fail("Not as expected-" + mention.getCorefChain().getCorefValue());
            }
        }
    }

//    @Test
    public void testRun() {
        String corefType = "SPORT_EVENT";
        DefaultInfoboxValidator sportExtractor = infoboxConfiguration.getExtractorByCorefType(corefType);
        final List<RawElasticResult> sportText = TestUtils.getTextAndTitle("wikipedia/tmp.json");

        ParseAndExtractMentionsWorker worker = new ParseAndExtractMentionsWorker(sportText, filter);

        worker.run();
        System.out.println();
    }
}
