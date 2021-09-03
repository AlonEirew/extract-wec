package wec.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wec.data.RawElasticResult;
import wec.data.WECContext;
import wec.extractors.ExtractFirstParagraph;
import wec.filters.ByCorefFilter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtractSearchNegativeExamplesWorker extends AWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractSearchNegativeExamplesWorker.class);
    private static final List<String> finalFirstPassagesList = new ArrayList<>();
    private static final AtomicInteger totalPassages = new AtomicInteger();
    private static final int MAX_TO_SAVE = 10000;

    private final ExtractFirstParagraph extractor = new ExtractFirstParagraph();
    private final ByCorefFilter filter = new ByCorefFilter();

    @Override
    public void run() {
        LOGGER.debug("Preparing to parse " + this.getRawElasticResults().size() + " wikipedia first passage and validate mentions");
        List<String> passagesList = new ArrayList<>();
        for(RawElasticResult rawResult : this.getRawElasticResults()) {
            List<WECContext> wecContexts = extractor.extract(rawResult);
            if (!wecContexts.isEmpty()) {
                WECContext wecContext = wecContexts.get(0);
                List<String> contextAsArray = wecContext.getContextAsArray();
                if(contextAsArray.size() > 10 && !wecContext.getMentionList().isEmpty() && filter.isConditionMet(wecContext)) {
                    passagesList.add(String.join(" ", contextAsArray));
                }
            }
        }

        synchronized (finalFirstPassagesList) {
            finalFirstPassagesList.addAll(passagesList);
            if(finalFirstPassagesList.size() >= MAX_TO_SAVE) {
                writeToFile();
            }
        }

        invokeListener();
    }

    public static int getTotalPassages() {
        return totalPassages.get();
    }

    public static void close() {
        if(!finalFirstPassagesList.isEmpty()) {
            writeToFile();
        }
    }

    private synchronized static void writeToFile() {
        try {
            LOGGER.debug("Writing-" + finalFirstPassagesList.size() + " passages");
            String json = String.join("\n", finalFirstPassagesList);
            Files.writeString(new File("input/Negative_First_Passages.txt").toPath(), json,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            totalPassages.addAndGet(finalFirstPassagesList.size());
            LOGGER.debug("Done writing-" + totalPassages.get() + " passages till now");
            finalFirstPassagesList.clear();
        } catch (Exception e) {
            LOGGER.error("Failed to write to file!", e);
        }
    }
}
