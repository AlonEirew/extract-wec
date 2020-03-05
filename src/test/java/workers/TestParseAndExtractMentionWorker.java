package workers;

import data.WECCoref;
import data.WECMention;
import org.junit.Assert;
import org.junit.Test;
import wec.PersonOrEventFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestParseAndExtractMentionWorker {

    @Test
    public void testFilterUnwantedMentions() {
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

        ParseAndExtractMentionsWorker worker = new ParseAndExtractMentionsWorker(mentions, new PersonOrEventFilter());

        Map<String, String> testMap = new HashMap<>();
        testMap.put("TEST3", "{{infoboxearthquake");
        testMap.put("TEST4", "{infoboxtofilter}");

        worker.filterUnwantedMentions(testMap);

        final List<WECMention> finalToCommit = worker.getFinalToCommit();
        Assert.assertEquals(2, finalToCommit.size());
        for(WECMention mention : finalToCommit) {
            if(!mention.getCorefChain().getCorefValue().equals("TEST2") &&
                    !mention.getCorefChain().getCorefValue().equals("TEST3")) {
                Assert.fail("Not as expected-" + mention.getCorefChain().getCorefValue());
            }
        }
    }
}
