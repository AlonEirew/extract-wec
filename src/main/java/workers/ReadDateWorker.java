package workers;

import data.InfoboxConfiguration;
import data.RawElasticResult;
import wec.DefaultInfoboxExtractor;
import wec.InfoboxFilter;
import wec.WECLinksExtractor;
import wec.extractors.TimeSpan1MonthInfoboxExtractor;

import java.util.*;

public class ReadDateWorker extends AWorker {

    private final List<String> datesSchemas;

    public ReadDateWorker(List<RawElasticResult> rawElasticResults, List<String> datesSchemas) {
        super(rawElasticResults);
        this.datesSchemas = datesSchemas;
    }

    @Override
    public void run() {
        for(RawElasticResult rawResult : this.rawElasticResults) {
            String date = extractDate(rawResult.getText(), rawResult.getTitle());
            if(date != null && !date.isEmpty()) {
                this.datesSchemas.add(date);
            }
        }
    }

    private String extractDate(String text, String title) {
        InfoboxFilter filter = new InfoboxFilter(new InfoboxConfiguration());
        String infoBox = filter.extractPageInfoBox(text);
        DefaultInfoboxExtractor attack = new TimeSpan1MonthInfoboxExtractor(null, null, null);

        String infoboxLow = infoBox.toLowerCase().replaceAll(" ", "");
        if (infoboxLow.contains("{{infoboxcivilianattack") || infoboxLow.contains("{{infoboxterroristattack") ||
                infoboxLow.contains("{{infoboxmilitaryattack") || infoboxLow.contains("{{infoboxcivilconflict") ||
                infoboxLow.contains("{{infoboxmilitaryconflict")) {
            String dateline = attack.extractDateLine(infoBox);
//            String dateString = attack.extractDateString(dateline);

            if (!dateline.isEmpty()) {// && !dateString.isEmpty()) {
                return dateline + " => " + title;
            }
        }

        return null;
    }
}
