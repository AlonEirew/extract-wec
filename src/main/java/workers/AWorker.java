package workers;

import data.RawElasticResult;

import java.util.List;

public abstract class AWorker implements Runnable {
    protected List<RawElasticResult> rawElasticResults;

    public AWorker() {

    }

    public AWorker(List<RawElasticResult> rawElasticResults) {
        this.rawElasticResults = rawElasticResults;
    }
}
