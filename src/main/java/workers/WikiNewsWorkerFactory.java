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

    public WikiNewsWorkerFactory(Map<String, WECCoref> wikiLinksCorefMap) {
        this.wikiLinksCorefMap = wikiLinksCorefMap;
    }

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new WikiNewsWorker(rawElasticResults, wikiLinksCorefMap);
    }

    @Override
    public void finalizeIfNeeded() {
        WikiNewsWorker worker = new WikiNewsWorker(new ArrayList<>(), new HashMap<>());
        worker.handle(new ArrayList<>(), true);
    }
}
