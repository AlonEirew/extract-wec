package wec.workers;

import wec.data.RawElasticResult;

import java.util.List;

/**
 * Interface for creating wec.workers @see wec.workers.AWorker
 */
public interface IWorkerFactory {
    AWorker createNewWorker(List<RawElasticResult> rawElasticResults);
    void finalizeIfNeeded();
}
