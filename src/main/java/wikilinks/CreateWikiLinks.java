package wikilinks;

import data.CorefType;
import data.RawElasticResult;
import data.WikiLinksCoref;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import utils.ExecutorServiceFactory;
import workers.IWorkerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CreateWikiLinks {

    private final SQLQueryApi sqlApi;
    private final IWorkerFactory workerFactory;
    private final ElasticQueryApi elasticApi;

    public CreateWikiLinks(SQLQueryApi sqlApi, ElasticQueryApi elasticApi, IWorkerFactory workerFactory) {
        this.sqlApi = sqlApi;
        this.elasticApi = elasticApi;
        this.workerFactory = workerFactory;
    }

    public void readAllWikiPagesAndProcess(int totalAmountToExtract) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        System.out.println("Strating process, Reading all documents from wikipedia (elastic)");

        List<Future<?>> allTasks = new ArrayList<>();

        long totalDocsCount = this.elasticApi.getTotalDocsCount();

        final Scroll scroll = new Scroll(TimeValue.timeValueHours(5L));
        SearchResponse searchResponse = this.elasticApi.createElasticSearchResponse(scroll);

        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        int count = 0;
        while (searchHits != null && searchHits.length > 0) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = elasticApi.getSearchScroll(scrollRequest);
            scrollId = searchResponse.getScrollId();

            List<RawElasticResult> rawElasticResults = this.elasticApi.getNextScrollResults(searchHits);
            allTasks.add(ExecutorServiceFactory.submit(this.workerFactory.createNewWorker(rawElasticResults)));
            System.out.println((totalDocsCount - count) + " documents to go");

            if(count >= totalAmountToExtract) {
                break;
            }

            count += searchHits.length;
            searchHits = searchResponse.getHits().getHits();
        }


        elasticApi.closeScroll(scrollId);
        System.out.println("Handling last mentions if exists");

        for (Future<?> future : allTasks) {
            future.get(1000, TimeUnit.SECONDS);
        }

        this.workerFactory.finalizeIfNeeded();
    }

    public void persistAllCorefs() {
        System.out.println("Persisting corefs tables values");
        final Collection<WikiLinksCoref> allCorefs = WikiLinksCoref.getGlobalCorefMap().values();
        final Iterator<WikiLinksCoref> corefIterator = allCorefs.iterator();

        while(corefIterator.hasNext()) {
            final WikiLinksCoref wikiLinksCoref = corefIterator.next();
            if(wikiLinksCoref.getMentionsCount() < 2 || wikiLinksCoref.getCorefType() == CorefType.NA ||
                    wikiLinksCoref.isMarkedForRemoval()) {
                corefIterator.remove();
            }
        }

        try {
            if (!this.sqlApi.insertRowsToTable(new ArrayList<>(allCorefs))) {
                System.out.println("Failed to insert Corefs!!!!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
