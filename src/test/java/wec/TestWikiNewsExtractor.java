package wec;

import data.RawElasticResult;
import data.WECCoref;
import data.WikiNewsMention;
import org.junit.Assert;
import org.junit.Test;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import workers.WikiNewsWorker;

import java.util.*;

public class TestWikiNewsExtractor {


//    @Test
//    public void testExtractWikiNewsLinks() {
//        final List<AbstractMap.SimpleEntry<String, String>> textAndTitle = TestUtils.getTextAndTitle("wiki_news/colombia.json");
//        AbstractMap.SimpleEntry<String, String> pair = textAndTitle.get(0);
//        final List<WikiNewsMention> wikiNewsMentions = WikipediaLinkExtractor.extractFromWikiNews(pair.getKey(), pair.getValue());
//        Assert.assertEquals(23, wikiNewsMentions.size());
//
//    }

//    @Test
    public void testNewsWorker() {
        final List<RawElasticResult> textAndTitle = TestUtils.getTextAndTitle("wiki_news/colombia.json");
        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections("jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v5.db"));
        final List<WECCoref> WECCorefMap = sqlApi.readTable(WECCoref.TABLE_COREF,
                new WECCoref("NA"));

        final Map<String, WECCoref> corefMap = new HashMap<>();
        for(WECCoref coref : WECCorefMap) {
            corefMap.put(coref.getCorefValue(), coref);
        }

        WikiNewsWorker worker = new WikiNewsWorker(textAndTitle, null, corefMap);
        worker.run();
        Assert.assertEquals(6, WikiNewsWorker.getFinalToCommit().size());
        System.out.println();
    }
}
