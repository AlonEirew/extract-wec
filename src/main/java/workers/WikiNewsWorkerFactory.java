package workers;

import data.RawElasticResult;
import data.WECCoref;
import persistence.SQLQueryApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WikiNewsWorkerFactory implements IWorkerFactory {

    private final Map<String, WECCoref> wikiLinksCorefMap;
    private final SQLQueryApi sqlApi;

    public WikiNewsWorkerFactory(Map<String, WECCoref> wikiLinksCorefMap, SQLQueryApi sqlApi) {
        this.wikiLinksCorefMap = wikiLinksCorefMap;
        this.sqlApi = sqlApi;
    }

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new WikiNewsWorker(rawElasticResults, sqlApi, wikiLinksCorefMap);
    }

    @Override
    public void finalizeIfNeeded() {
        WikiNewsWorker worker = new WikiNewsWorker(new ArrayList<>(), sqlApi, new HashMap<>());
        worker.handle(new ArrayList<>(), true);
    }
}
