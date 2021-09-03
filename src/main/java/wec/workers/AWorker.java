package wec.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wec.data.RawElasticResult;

import java.util.List;

public abstract class AWorker implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AWorker.class);

    private List<RawElasticResult> rawElasticResults;
    private IThreadDoneListener<AWorker> listener;

    protected void setRawElasticResults(List<RawElasticResult> rawElasticResults) {
        this.rawElasticResults = rawElasticResults;
    }

    protected void setListener(IThreadDoneListener<AWorker> listener) {
        this.listener = listener;
    }

    protected IThreadDoneListener<AWorker> getListener() {
        return listener;
    }

    protected List<RawElasticResult> getRawElasticResults() {
        return rawElasticResults;
    }

    protected void invokeListener() {
        if (this.listener != null) {
            this.listener.onThreadDone(this);
        } else {
            LOGGER.debug("No registered listener!");
        }
    }
}
