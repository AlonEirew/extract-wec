package wec.workers;

import wec.data.RawElasticResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiNewsRedirectCounterWorker extends AWorker {

    private static volatile AtomicInteger redCounter = new AtomicInteger();

    public WikiNewsRedirectCounterWorker(List<RawElasticResult> rawElasticResults) {
        super(rawElasticResults);
    }

    @Override
    public void run() {
        for(RawElasticResult rowResult : this.rawElasticResults) {
            if(rowResult.getText().toLowerCase().startsWith("#redirect")) {
                redCounter.incrementAndGet();
            }
        }
    }

    public static int getCounter() {
        return redCounter.get();
    }
}
