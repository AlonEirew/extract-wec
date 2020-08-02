package workers;

import data.RawElasticResult;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wec.PersonOrEventFilter;

import java.util.List;

public class ParseAndExtractWorkersFactory implements IWorkerFactory {

    private final SQLQueryApi sqlApi;
    private final ElasticQueryApi elasticApi;
    private final PersonOrEventFilter filter;

    public ParseAndExtractWorkersFactory(SQLQueryApi sqlApi, ElasticQueryApi elasticApi, PersonOrEventFilter filter) {
        this.sqlApi = sqlApi;
        this.elasticApi = elasticApi;
        this.filter = filter;
    }

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new ParseAndExtractMentionsWorker(rawElasticResults, this.sqlApi, this.elasticApi, this.filter);
    }

    @Override
    public void finalizeIfNeeded() {
//        final ParseAndExtractMentionsWorker parseAndExtractMentionsWorker = new
//                ParseAndExtractMentionsWorker(new ArrayList<>(),
//                this.sqlApi, this.elasticApi, new PersonOrEventFilter());
//
//        parseAndExtractMentionsWorker.handle(new ArrayList<>(), true);
    }
}
