package workers;

import data.CorefSubType;
import data.RawElasticResult;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.util.Pair;
import utils.StanfordNlpApi;
import wec.AInfoboxExtractor;
import wec.WECLinksExtractor;
import wec.extractors.AttackInfoboxExtractor;
import wec.extractors.GeneralEventInfoboxExtractor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String infoBox = WECLinksExtractor.extractPageInfoBox(text);
        AInfoboxExtractor attack = new AttackInfoboxExtractor();

        String infoboxLow = infoBox.toLowerCase().replaceAll(" ", "");
        CorefSubType corefSubType = CorefSubType.NA;
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
