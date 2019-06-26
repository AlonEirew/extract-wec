package workers;

import data.RawElasticResult;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wikilinks.PersonOrEventFilter;

import java.util.List;

public class ParseAndExtractWorkersFactory implements IWorkerFactory {

    private ParseListener listener;

    public ParseAndExtractWorkersFactory(SQLQueryApi sqlApi, ElasticQueryApi elasticApi) {
        this.listener = new ParseListener(sqlApi, elasticApi, new PersonOrEventFilter());
    }

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new ParseAndExtractMentionsWorker(rawElasticResults, this.listener);
    }

    @Override
    public void finalizeIfNeeded() {
        this.listener.handle();
    }
}
