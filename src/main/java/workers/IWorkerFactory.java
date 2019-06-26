package workers;

import data.RawElasticResult;

import java.util.List;

public interface IWorkerFactory {
    AWorker createNewWorker(List<RawElasticResult> rawElasticResults);
    void finalizeIfNeeded();
}
