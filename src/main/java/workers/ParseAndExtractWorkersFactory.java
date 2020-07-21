package workers;

import data.RawElasticResult;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wec.AInfoboxExtractor;
import wec.PersonOrEventFilter;
import wec.extractors.*;

import java.util.ArrayList;
import java.util.List;

public class ParseAndExtractWorkersFactory implements IWorkerFactory {

    private SQLQueryApi sqlApi;
    private ElasticQueryApi elasticApi;
    private PersonOrEventFilter filter;

    public ParseAndExtractWorkersFactory(SQLQueryApi sqlApi, ElasticQueryApi elasticApi) {
        this.sqlApi = sqlApi;
        this.elasticApi = elasticApi;
        List<AInfoboxExtractor> extractors = new ArrayList<>();

        DisasterInfoboxExtractor disasterInfoboxExtractor = new DisasterInfoboxExtractor();
        AttackInfoboxExtractor attackInfoboxExtractor = new AttackInfoboxExtractor();
        AccidentInfoboxExtractor accidentInfoboxExtractor = new AccidentInfoboxExtractor();
        AwardInfoboxExtractor awardInfoboxExtractor = new AwardInfoboxExtractor();
        GeneralEventInfoboxExtractor generalEventInfoboxExtractor = new GeneralEventInfoboxExtractor();

        extractors.add(disasterInfoboxExtractor);
        extractors.add(attackInfoboxExtractor);
        extractors.add(accidentInfoboxExtractor);
        extractors.add(awardInfoboxExtractor);
        extractors.add(generalEventInfoboxExtractor);

        this.filter = new PersonOrEventFilter(extractors);
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
