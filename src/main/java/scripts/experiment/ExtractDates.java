package scripts.experiment;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripts.wec.WikipediaExperimentUtils;
import workers.ReadDateWorker;
import workers.WorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtractDates {
    private final static Logger LOGGER = LogManager.getLogger(ExtractDates.class);

    public static void main(String[] args) throws IOException {
        WikipediaExperimentUtils wikipediaExperimentUtils = new WikipediaExperimentUtils();
        WorkerFactory<List<String>> readDateWorkerFactory = new WorkerFactory(ReadDateWorker.class,
                List.class, new ArrayList<>());

        wikipediaExperimentUtils.runWikipediaExperiment(readDateWorkerFactory);
        final List<String> datesSchemas = readDateWorkerFactory.getResource();
        FileUtils.writeLines(new File("output/datesParse.txt"), datesSchemas, "\n");
        LOGGER.info(datesSchemas.size());
    }
}
