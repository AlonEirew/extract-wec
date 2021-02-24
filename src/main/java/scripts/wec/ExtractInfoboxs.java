package scripts.wec;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import workers.InfoboxWorker;
import workers.WorkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtractInfoboxs {
    private final static Logger LOGGER = LogManager.getLogger(ExtractInfoboxs.class);

    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            InfoboxWorker.setInfoboxLang(args[0]);
        }
        WikipediaExperimentUtils wikipediaExperimentUtils = new WikipediaExperimentUtils();
        Map<String, AtomicInteger> infoboxesCounts = new ConcurrentHashMap<>();
        WorkerFactory infoboxWorkerFactory = new WorkerFactory(InfoboxWorker.class, Map.class, infoboxesCounts);
        wikipediaExperimentUtils.runWikipediaExperiment(infoboxWorkerFactory);

        List<Map.Entry<String, AtomicInteger>> sortedInfoboxs = new ArrayList<>(infoboxesCounts.entrySet());
        sortedInfoboxs.sort(Comparator.comparingInt(o -> o.getValue().get()));
        Collections.reverse(sortedInfoboxs);

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, AtomicInteger> entry : sortedInfoboxs) {
            result.add(entry.getKey() + "=" + entry.getValue());
        }

        FileUtils.writeLines(new File("output/InfoboxCount.txt"), result, "\n");
        LOGGER.info(sortedInfoboxs.size());
    }
}
