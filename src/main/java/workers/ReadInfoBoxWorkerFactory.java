package workers;

import data.RawElasticResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReadInfoBoxWorkerFactory implements IWorkerFactory {

    protected Map<String, Set<String>> infoBoxes = new HashMap<>();

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new ReadInfoBoxWorker(rawElasticResults, this.infoBoxes);
    }

    @Override
    public void finalizeIfNeeded() {

    }

    public Map<String, Set<String>> getInfoBoxes() {
        return infoBoxes;
    }
}
