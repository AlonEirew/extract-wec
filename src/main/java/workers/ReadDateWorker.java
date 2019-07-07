package workers;

import data.RawElasticResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadDateWorker extends AWorker {

    private List<String> datesSchemas = new ArrayList<>();

    public ReadDateWorker() {
    }

    public ReadDateWorker(List<RawElasticResult> rawElasticResults, List<String> datesSchemas) {
        super(rawElasticResults);
        this.datesSchemas = datesSchemas;
    }

    @Override
    public void run() {
        for(RawElasticResult rawResult : this.rawElasticResults) {
            String date = extractDate(rawResult.getText());
            if(date != null && !date.isEmpty()) {
                this.datesSchemas.add(date);
            }
        }
    }

    private String extractDate(String text) {
        Pattern patternStart = Pattern.compile("\\{\\{infobox");
        Matcher matcherStart = patternStart.matcher(text);

        Pattern patternEnd = Pattern.compile("'''|footnotes=|==");

        String infoBox = null;
        // Check all occurrences
        if (matcherStart.find()) {
            final int infoStart = matcherStart.start();
            Matcher matcherEnd = patternEnd.matcher(text.substring(infoStart));
            if (matcherEnd.find()) {
                final int infoEnd = matcherEnd.end() + infoStart;
                if (infoStart < infoEnd && infoStart != -1 && infoEnd != -1) {
                    infoBox = text.substring(infoStart, infoEnd);
                }
            }
        }

        String date = null;
        if(infoBox != null) {
            String dateSubStr = infoBox.substring(infoBox.indexOf("date"));
            date = dateSubStr.substring(0, dateSubStr.indexOf("\n"));
        }

        return date;
    }
}
