package workers;

import data.RawElasticResult;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wec.InfoboxFilter;

import java.util.List;

public class ParseAndExtractWorkersFactory implements IWorkerFactory {

    private final SQLQueryApi sqlApi;
    private final ElasticQueryApi elasticApi;
    private final InfoboxFilter filter;

    public ParseAndExtractWorkersFactory(SQLQueryApi sqlApi, ElasticQueryApi elasticApi, InfoboxFilter filter) {
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
    }
}
