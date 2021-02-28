package wec.workers;

import wec.data.RawElasticResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WorkerFactory<T> implements IWorkerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerFactory.class);

    private final T resource;
    private final Class<T> tClass;
    private final Class<? extends AWorker> worker;

    public WorkerFactory(Class<? extends AWorker> worker) {
        this.resource = null;
        this.tClass = null;
        this.worker = worker;
    }

    public WorkerFactory(Class<? extends AWorker> worker, Class<T> tClass, T resource) {
        this.resource = resource;
        this.tClass = tClass;
        this.worker = worker;
    }

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        try {
            if (tClass != null && resource != null) {
                return (AWorker) Class.forName(worker.getName()).getConstructor(List.class, tClass)
                        .newInstance(rawElasticResults, this.resource);
            } else {
                return (AWorker) Class.forName(worker.getName()).getConstructor(List.class).newInstance(rawElasticResults);
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to create worker!", ex);
        }
        return null;
    }

    public T getResource() {
        return resource;
    }

    @Override
    public void finalizeIfNeeded() {
    }
}
