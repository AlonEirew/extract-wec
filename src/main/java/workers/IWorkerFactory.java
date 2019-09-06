package workers;

import data.RawElasticResult;

import java.util.List;

/**
 * Interface for creating workers @see workers.AWorker
 */
public interface IWorkerFactory {
    AWorker createNewWorker(List<RawElasticResult> rawElasticResults);
    void finalizeIfNeeded();
}
