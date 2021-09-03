package wec.workers;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import wec.config.Configuration;
import wec.data.RawElasticResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class WorkerFactory implements IWorkerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerFactory.class);

    private final Class<? extends AWorker> workerCls;
    private final GenericObjectPool<AWorker> objectPool;

    public WorkerFactory(Class<? extends AWorker> workerCls) {
        this.workerCls = workerCls;

        GenericObjectPoolConfig<AWorker> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(50);
        this.objectPool = new GenericObjectPool<>(this, config);
        try {
            this.objectPool.addObjects(this.objectPool.getMaxTotal());
        } catch (Exception e) {
            LOGGER.error("Failed to ini");
        }
    }

    @Override
    public AWorker borrowNewWorker(List<RawElasticResult> rawElasticResults) {
        AWorker aWorker = null;
        try {
            aWorker = this.objectPool.borrowObject();
            aWorker.setRawElasticResults(rawElasticResults);
        } catch (Exception ex) {
            LOGGER.error("Failed to create worker!", ex);
        }
        return aWorker;
    }

    @Override
    public void close() {
        LOGGER.debug("Closing pool..");
        this.objectPool.close();
    }

    @Override
    public void activateObject(PooledObject<AWorker> pooledObject) throws Exception {
        pooledObject.getObject().setListener(this);
    }

    @Override
    public void destroyObject(PooledObject<AWorker> pooledObject) throws Exception {
        passivateObject(pooledObject);
    }

    @Override
    public PooledObject<AWorker> makeObject() throws Exception {
        DefaultPooledObject<AWorker> pooledObject = null;
        try {
            pooledObject = new DefaultPooledObject<>((AWorker) Class.forName(workerCls.getName()).getConstructor().newInstance());
        } catch (Exception ex) {
            LOGGER.error("Failed to create worker!", ex);
        }

        return pooledObject;
    }

    @Override
    public void passivateObject(PooledObject<AWorker> pooledObject) throws Exception {
        pooledObject.getObject().setListener(null);
        pooledObject.getObject().setRawElasticResults(null);
    }

    @Override
    public boolean validateObject(PooledObject<AWorker> p) {
        return true;
    }

    @Override
    public void onThreadDone(AWorker doneThread) {
        this.objectPool.returnObject(doneThread);
    }
}
