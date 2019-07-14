package workers;

import data.RawElasticResult;

import java.util.List;

public class WikiNewsRedirectCounterWorkerFactory implements IWorkerFactory {

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new WikiNewsRedirectCounterWorker(rawElasticResults);
    }

    @Override
    public void finalizeIfNeeded() {

    }
}
