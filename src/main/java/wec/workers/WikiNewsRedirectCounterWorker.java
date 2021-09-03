package wec.workers;

import wec.data.RawElasticResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiNewsRedirectCounterWorker extends AWorker {

    private static volatile AtomicInteger redCounter = new AtomicInteger();

    public WikiNewsRedirectCounterWorker(List<RawElasticResult> rawElasticResults) {
        this.setRawElasticResults(rawElasticResults);
    }

    @Override
    public void run() {
        for(RawElasticResult rowResult : this.getRawElasticResults()) {
            if(rowResult.getText().toLowerCase().startsWith("#redirect")) {
                redCounter.incrementAndGet();
            }
        }

        invokeListener();
    }

    public static int getCounter() {
        return redCounter.get();
    }
}
