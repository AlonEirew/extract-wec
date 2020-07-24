package workers;

import data.RawElasticResult;
import wec.WECLinksExtractor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReadInfoBoxWorker extends AWorker {

    private Map<String, Set<String>> infoBoxes;

    public ReadInfoBoxWorker(List<RawElasticResult> rawElasticResults, Map<String, Set<String>> infoBoxes) {
        super(rawElasticResults);
        this.infoBoxes = infoBoxes;
    }

    @Override
    public void run() {
        for(RawElasticResult rawResult : this.rawElasticResults) {
            String infoBox = WECLinksExtractor.extractPageInfoBox(rawResult.getText(), true);
            if(infoBox != null && !infoBox.isEmpty()) {
                infoBox = toReadableString(infoBox);

                if (infoBoxValid(infoBox)) {
                    if (!this.infoBoxes.containsKey(infoBox)) {
                        this.infoBoxes.put(infoBox, new HashSet<>());
                    }

                    this.infoBoxes.get(infoBox).add(rawResult.getTitle());
                }
            }
        }
    }

    private boolean infoBoxValid(String infoBox) {
        return !infoBox.contains("template:") && !infoBox.contains("wikipedia:");
    }

    private String toReadableString(String infoBox) {
        if(infoBox != null && !infoBox.isEmpty()) {
            if(infoBox.contains("|")) {
                infoBox = infoBox.substring(0, infoBox.indexOf("|"));
            }

            infoBox = infoBox
                    .replaceAll("\\{", "")
                    .replaceAll("}", "")
                    .replaceAll("\n", "")
                    .replaceAll("-", "")
                    .replaceAll("=", "")
                    .replaceAll("!", "")
                    .replaceAll("<", "")
                    .replaceAll(">", "")
                    .replaceAll("\\s+", "")
                    .replaceAll("\\|", "")
                    .replaceAll("<.*?>", "")
                    .replace("infobox", "").trim();
        }

        return infoBox;
    }
}
