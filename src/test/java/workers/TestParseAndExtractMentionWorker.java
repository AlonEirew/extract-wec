package workers;

import com.google.gson.Gson;
import data.InfoboxConfiguration;
import data.RawElasticResult;
import data.WECCoref;
import data.WECMention;
import org.junit.Assert;
import org.junit.Test;
import wec.InfoboxFilter;
import wec.TestWECLinksExtractor;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class TestParseAndExtractMentionWorker {

    private static final Gson GSON = new Gson();

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

        String inputStreamNlp = Objects.requireNonNull(TestWECLinksExtractor.class.getClassLoader()
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
}
