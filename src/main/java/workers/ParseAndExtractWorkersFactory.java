package workers;

import data.RawElasticResult;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wec.InfoboxFilter;

import java.util.List;

public class ParseAndExtractWorkersFactory implements IWorkerFactory {

    private final InfoboxFilter filter;

    public ParseAndExtractWorkersFactory(InfoboxFilter filter) {
        this.filter = filter;
    }

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new ParseAndExtractMentionsWorker(rawElasticResults, this.filter);
    }

    @Override
    public void finalizeIfNeeded() {
    }
}
