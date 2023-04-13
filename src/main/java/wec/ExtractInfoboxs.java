package wec;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import scripts.WikipediaExperimentUtils;
import wec.workers.InfoboxWorker;
import wec.workers.WorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ExtractInfoboxs {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractInfoboxs.class);

    public void runInfoboxScript(String[] args) throws IOException {
        if (args.length == 2) {
            LOGGER.info("Setting default language: infobox " + args[1]);
            InfoboxWorker.setInfoboxLang(args[1]);
        } else {
            LOGGER.info("No language specified, using default: infobox");
        }
        WikipediaExperimentUtils wikipediaExperimentUtils = new WikipediaExperimentUtils();
        WorkerFactory infoboxWorkerFactory = new WorkerFactory(InfoboxWorker.class);
        wikipediaExperimentUtils.runWikipediaExperiment(infoboxWorkerFactory);

        List<Map.Entry<String, AtomicInteger>> sortedInfoboxs = new ArrayList<>(InfoboxWorker.getInfoboxCounts().entrySet());
        sortedInfoboxs.sort(Comparator.comparingInt(o -> o.getValue().get()));
        Collections.reverse(sortedInfoboxs);

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, AtomicInteger> entry : sortedInfoboxs) {
            result.add(entry.getKey() + "=" + entry.getValue());
        }

        FileUtils.writeLines(new File("output/InfoboxCount.txt"), result, "\n");
        LOGGER.info(String.valueOf(sortedInfoboxs.size()));
    }
}
