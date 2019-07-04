package workers;

import data.RawElasticResult;

import java.util.List;
import java.util.Map;

public class ReadInfoBoxWorker extends AWorker {

    private Map<String, String> infoBoxes;

    public ReadInfoBoxWorker() {

    }

    public ReadInfoBoxWorker(List<RawElasticResult> rawElasticResults, Map<String, String> infoBoxes) {
        super(rawElasticResults);
        this.infoBoxes = infoBoxes;
    }

    @Override
    public void run() {
        for(RawElasticResult rawResult : this.rawElasticResults) {
            String infoBox = extractPageInfoBox(rawResult.getText());
            if(infoBox != null && !this.infoBoxes.containsKey(infoBox)) {
                this.infoBoxes.put(infoBox, rawResult.getTitle());
            }
        }
    }

    public String extractPageInfoBox(String text) {
        String infoBox = null;
        if(text.contains("Infobox")) {
            infoBox = text.substring(text.indexOf("Infobox"));
            infoBox = infoBox.substring(0, text.indexOf("\n"));
        }
        // Check all occurrences
        if(infoBox != null && !infoBox.isEmpty()) {
            infoBox = infoBox.substring(0, infoBox.indexOf("\n"));
            infoBox = infoBox.replaceAll("\\{", "");
            infoBox = infoBox.replaceAll("}", "");
            infoBox = infoBox.replaceAll("\\|", "");
            infoBox = infoBox.replaceAll("<.*?>", "");
            infoBox = infoBox.replace("Infobox", "").trim();
        }

        return infoBox;
    }
}
