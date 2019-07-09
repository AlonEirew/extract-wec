package workers;

import data.RawElasticResult;

import java.util.List;

public class WikiNewsWorker extends AWorker {

    public WikiNewsWorker(List<RawElasticResult> rawElasticResults) {
        super(rawElasticResults);
    }

    @Override
    public void run() {

    }
}
