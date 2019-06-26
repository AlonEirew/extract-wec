package workers;

import data.RawElasticResult;

import java.util.List;

public abstract class AWorker implements Runnable {
    protected List<RawElasticResult> rawElasticResults;
    protected ParseListener listener;

    public AWorker(List<RawElasticResult> rawElasticResults, ParseListener listener) {
        this.rawElasticResults = rawElasticResults;
        this.listener = listener;
    }
}
