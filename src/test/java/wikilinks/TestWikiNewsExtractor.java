package wikilinks;

import data.RawElasticResult;
import data.WikiLinksCoref;
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
        final List<WikiNewsMention> wikiNewsMentions = WikiLinksExtractor.extractFromWikiNews(pair.getKey(), pair.getValue());
        Assert.assertEquals(23, wikiNewsMentions.size());

    }

//    @Test
    public void testNewsWorker() {
        final List<Pair<String, String>> textAndTitle = TestUtils.getTextAndTitle("wiki_news/colombia.json");
        Pair<String, String> pair = textAndTitle.get(0);
        List<RawElasticResult> elasticResults = new ArrayList<>();
        elasticResults.add(new RawElasticResult(pair.getKey(), pair.getValue()));
        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections("jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v5.db"));
        final List<WikiLinksCoref> wikiLinksCorefMap = sqlApi.readTable(WikiLinksCoref.TABLE_COREF,
                new WikiLinksCoref("NA"));

        final Map<String, WikiLinksCoref> corefMap = new HashMap<>();
        for(WikiLinksCoref coref : wikiLinksCorefMap) {
            corefMap.put(coref.getCorefValue(), coref);
        }

        WikiNewsWorker worker = new WikiNewsWorker(elasticResults, null, corefMap);
        worker.run();
        Assert.assertEquals(6, WikiNewsWorker.getFinalToCommit().size());
        System.out.println();
    }
}
