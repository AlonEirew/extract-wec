package workers;

import data.RawElasticResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadWorkerFactory implements IWorkerFactory {

    protected Map<String, String> infoBoxes = new HashMap<>();

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new ReadInfoBoxWorker(rawElasticResults, this.infoBoxes);
    }

    @Override
    public void finalizeIfNeeded() {

    }

    public Map<String, String> getInfoBoxes() {
        return infoBoxes;
    }
}
