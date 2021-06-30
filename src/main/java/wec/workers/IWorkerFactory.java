package wec.workers;

import org.apache.commons.pool2.PooledObjectFactory;
import wec.data.RawElasticResult;

import java.io.Closeable;
import java.util.List;

/**
 * Interface for creating wec.workers @see wec.workers.AWorker
 */
public interface IWorkerFactory extends PooledObjectFactory<AWorker>, IThreadDoneListener<AWorker>, Closeable {
    AWorker borrowNewWorker(List<RawElasticResult> rawElasticResults);
}
