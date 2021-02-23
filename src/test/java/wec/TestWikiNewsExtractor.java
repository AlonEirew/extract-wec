package wec;

import data.RawElasticResult;
import data.WECCoref;
import org.junit.Assert;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import persistence.WECResources;
import workers.WikiNewsWorker;

import java.util.*;

public class TestWikiNewsExtractor {


//    @Test
//    public void testExtractWikiNewsLinks() {
//        final List<AbstractMap.SimpleEntry<String, String>> textAndTitle = TestUtils.getTextAndTitle("wikinews/colombia.json");
//        AbstractMap.SimpleEntry<String, String> pair = textAndTitle.get(0);
//        final List<WikiNewsMention> wikiNewsMentions = WikipediaLinkExtractor.extractFromWikiNews(pair.getKey(), pair.getValue());
//        Assert.assertEquals(23, wikiNewsMentions.size());
//
//    }

//    @Test
    public void testNewsWorker() {
        final List<RawElasticResult> textAndTitle = TestUtils.getTextAndTitle("wikinews/colombia.json");
        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(
                "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v5.db"));
        WECResources.setSqlApi(sqlApi);

        final List<WECCoref> WECCorefMap = sqlApi.readTable(WECCoref.TABLE_COREF,
                new WECCoref("NA"));

        final Map<String, WECCoref> corefMap = new HashMap<>();
        for(WECCoref coref : WECCorefMap) {
            corefMap.put(coref.getCorefValue(), coref);
        }

        WikiNewsWorker worker = new WikiNewsWorker(textAndTitle, corefMap);
        worker.run();
        Assert.assertEquals(6, WikiNewsWorker.getFinalToCommit().size());
        System.out.println();
    }
}
