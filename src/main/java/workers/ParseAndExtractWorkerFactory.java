package workers;

import data.RawElasticResult;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wikilinks.PersonOrEventFilter;

import java.util.List;

public class ParseAndExtractWorkerFactory implements IWorkerFactory {

    private ParseListener listener;

    public ParseAndExtractWorkerFactory(SQLQueryApi sqlApi, ElasticQueryApi elasticApi) {
        this.listener = new ParseListener(sqlApi, elasticApi, new PersonOrEventFilter());
    }

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new ParseAndHandleMentions(rawElasticResults, this.listener);
    }

    @Override
    public void finalizeIfNeeded() {
        this.listener.handle();
    }
}
