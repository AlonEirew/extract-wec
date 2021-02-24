package experimentscripts.wec;

import config.WECConfigurations;
import wec.InfoboxFilter;
import workers.ReadCompanyWorker;
import workers.WorkerFactory;

import java.io.IOException;

public class ExtractCompanyMain {
    public static void main(String[] args) throws IOException {
        WikipediaExperimentUtils wikipediaUtils = new WikipediaExperimentUtils();
        WorkerFactory<InfoboxFilter> readCompWorkerFactory = new WorkerFactory<>(ReadCompanyWorker.class,
                InfoboxFilter.class, new InfoboxFilter(WECConfigurations.getInfoboxConf()));
        wikipediaUtils.runWikipediaExperiment(readCompWorkerFactory);
    }
}
