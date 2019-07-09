package workers;

import data.RawElasticResult;

import java.util.List;

public class WikiNewsWorkerFactory implements IWorkerFactory {

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new WikiNewsWorker(rawElasticResults);
    }

    @Override
    public void finalizeIfNeeded() {

    }
}
