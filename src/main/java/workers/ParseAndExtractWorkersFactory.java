package workers;

import data.RawElasticResult;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wikilinks.PersonOrEventFilter;

import java.util.ArrayList;
import java.util.List;

public class ParseAndExtractWorkersFactory implements IWorkerFactory {

    private SQLQueryApi sqlApi;
    private ElasticQueryApi elasticApi;

    public ParseAndExtractWorkersFactory(SQLQueryApi sqlApi, ElasticQueryApi elasticApi) {
        this.sqlApi = sqlApi;
        this.elasticApi = elasticApi;
    }

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new ParseAndExtractMentionsWorker(rawElasticResults, this.sqlApi, this.elasticApi, new PersonOrEventFilter());
    }

    @Override
    public void finalizeIfNeeded() {
        final ParseAndExtractMentionsWorker parseAndExtractMentionsWorker = new
                ParseAndExtractMentionsWorker(new ArrayList<>(),
                this.sqlApi, this.elasticApi, new PersonOrEventFilter());

        parseAndExtractMentionsWorker.commitCurrent();
    }
}
