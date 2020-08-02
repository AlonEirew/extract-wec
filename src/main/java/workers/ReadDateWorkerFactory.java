package workers;

import data.RawElasticResult;

import java.util.ArrayList;
import java.util.List;

public class ReadDateWorkerFactory implements IWorkerFactory {

    private final List<String> datesSchemas = new ArrayList<>();

    @Override
    public AWorker createNewWorker(List<RawElasticResult> rawElasticResults) {
        return new ReadDateWorker(rawElasticResults, this.datesSchemas);
    }

    @Override
    public void finalizeIfNeeded() {

    }

    public List<String> getDatesSchemas() {
        return this.datesSchemas;
    }
}
