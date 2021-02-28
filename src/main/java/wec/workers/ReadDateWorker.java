package wec.workers;

import wec.data.RawElasticResult;
import wec.validators.TimeSpan1MonthInfoboxValidator;

import java.util.List;

public class ReadDateWorker extends AWorker {

    private final List<String> datesSchemas;

    public ReadDateWorker(List<RawElasticResult> rawElasticResults, List<String> datesSchemas) {
        super(rawElasticResults);
        this.datesSchemas = datesSchemas;
    }

    @Override
    public void run() {
        for(RawElasticResult rawResult : this.rawElasticResults) {
            String date = extractDate(rawResult);
            if(date != null && !date.isEmpty()) {
                this.datesSchemas.add(date);
            }
        }
    }

    private String extractDate(RawElasticResult rawResult) {
        String infoboxLow = rawResult.getInfobox().toLowerCase().replaceAll(" ", "");
        if (infoboxLow.contains("{{infoboxcivilianattack") || infoboxLow.contains("{{infoboxterroristattack") ||
                infoboxLow.contains("{{infoboxmilitaryattack") || infoboxLow.contains("{{infoboxcivilconflict") ||
                infoboxLow.contains("{{infoboxmilitaryconflict")) {
            String dateline = TimeSpan1MonthInfoboxValidator.extractDateLine(rawResult.getInfobox());
//            String dateString = attack.extractDateString(dateline);

            if (!dateline.isEmpty()) {// && !dateString.isEmpty()) {
                return dateline + " => " + rawResult.getTitle();
            }
        }

        return null;
    }
}
