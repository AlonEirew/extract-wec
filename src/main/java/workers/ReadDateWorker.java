package workers;

import data.RawElasticResult;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
            Date date = extractDate(rawResult.getText());
            if(date != null && !date.toString().isEmpty()) {
                this.datesSchemas.add(date.toString());
            }
        }
    }

    public Date extractDate(String text) {
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
                if (infoStart < infoEnd) {
                    infoBox = text.substring(infoStart, infoEnd);
                    if(infoBox.contains("}")) {
                        infoBox = infoBox.substring(0, infoBox.indexOf("}"));

                    }
                }
            }
        }

        String dateline = extractDateLine(infoBox);
        String dateString = extractDateString(dateline);
        Date date = convertToDateTime(dateString);

        return date;
    }

    private Date convertToDateTime(String dateString) {
        Date date = null;
        if(dateString != null && !dateString.isEmpty()) {
            DateFormat fmt1 = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
            DateFormat fmt2 = new SimpleDateFormat("MMMM dd yyyy", Locale.US);
            DateFormat fmt3 = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
            DateFormat fmt4 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            DateFormat fmt5 = new SimpleDateFormat("MMMMdd,yyyy", Locale.US);
            DateFormat fmt6 = new SimpleDateFormat("ddMMMM,yyyy", Locale.US);
            DateFormat fmt7 = new SimpleDateFormat("MMMMddyyyy", Locale.US);
            DateFormat fmt8 = new SimpleDateFormat("ddMMMMyyyy", Locale.US);

            try {
                date = fmt1.parse(dateString);
            } catch (ParseException e) {
            }
            if (date == null) {
                try {
                    date = fmt2.parse(dateString);
                } catch (ParseException e) {
                }
            }
            if (date == null) {
                try {
                    date = fmt3.parse(dateString);
                } catch (ParseException e) {
                }
            }
            if (date == null) {
                try {
                    date = fmt4.parse(dateString);
                } catch (ParseException e) {
                }
            }
            if (date == null) {
                try {
                    date = fmt5.parse(dateString);
                } catch (ParseException e) {
                }
            }
            if (date == null) {
                try {
                    date = fmt6.parse(dateString);
                } catch (ParseException e) {
                }
            }
            if (date == null) {
                try {
                    date = fmt7.parse(dateString);
                } catch (ParseException e) {
                }
            }
            if (date == null) {
                try {
                    date = fmt8.parse(dateString);
                } catch (ParseException e) {
                }
            }

            if(date == null) {
                System.out.println("Couldnt Parse-" + dateString);
            }
        }

        return date;
    }

    private String extractDateString(String dateline) {
        String extractedDate = "";
        if(dateline != null && !dateline.isEmpty()) {
            String dateLineSplit[] = dateline.split("=");
            if(dateLineSplit.length == 2) {
                extractedDate = dateLineSplit[1].trim();
            }
        }

        return extractedDate;
    }

    private String extractDateLine(String infoBox) {
        String date = null;
        if(infoBox != null && infoBox.contains("date")) {
            String dateSubStr = infoBox.substring(infoBox.indexOf("date"));
            if(dateSubStr.contains("\n")) {
                date = dateSubStr.substring(0, dateSubStr.indexOf("\n"));
            } else {
                date = dateSubStr;
            }
        }
        return date;
    }
}
