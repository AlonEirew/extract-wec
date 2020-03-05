package wec;

import data.RawElasticResult;
import data.WECCoref;
import data.WikiNewsMention;
import javafx.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import workers.WikiNewsWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestWikiNewsExtractor {


    @Test
    public void testExtractWikiNewsLinks() {
        final List<Pair<String, String>> textAndTitle = TestUtils.getTextAndTitle("wiki_news/colombia.json");
        Pair<String, String> pair = textAndTitle.get(0);
        final List<WikiNewsMention> wikiNewsMentions = WECLinksExtractor.extractFromWikiNews(pair.getKey(), pair.getValue());
        Assert.assertEquals(23, wikiNewsMentions.size());

    }

//    @Test
    public void testNewsWorker() {
        final List<Pair<String, String>> textAndTitle = TestUtils.getTextAndTitle("wiki_news/colombia.json");
        Pair<String, String> pair = textAndTitle.get(0);
        List<RawElasticResult> elasticResults = new ArrayList<>();
        elasticResults.add(new RawElasticResult(pair.getKey(), pair.getValue()));
        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections("jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v5.db"));
        final List<WECCoref> WECCorefMap = sqlApi.readTable(WECCoref.TABLE_COREF,
                new WECCoref("NA"));

        final Map<String, WECCoref> corefMap = new HashMap<>();
        for(WECCoref coref : WECCorefMap) {
            corefMap.put(coref.getCorefValue(), coref);
        }

        WikiNewsWorker worker = new WikiNewsWorker(elasticResults, null, corefMap);
        worker.run();
        Assert.assertEquals(6, WikiNewsWorker.getFinalToCommit().size());
        System.out.println();
    }
}
