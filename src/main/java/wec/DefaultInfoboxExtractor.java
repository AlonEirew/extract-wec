package wec;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.Timex;
import utils.StanfordNlpApi;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultInfoboxExtractor {

    public static final String NA = "NA";

    private final List<String> infoboxs;
    private final String corefType;

    public DefaultInfoboxExtractor(String corefType, List<String> infoboxs) {
        this.corefType = corefType;
        this.infoboxs = infoboxs;
    }

    public String extractMatchedInfobox(String infobox, String title) {
        String infoboxLow = infobox.toLowerCase().replaceAll(" ", "");
        Pattern pattern = Pattern.compile("\\{\\{infobox[\\w|]*?("+ String.join("|", this.infoboxs) + ")");
        Matcher matcher = pattern.matcher(infoboxLow);

        if(matcher.find()) {
            return matcher.group(1);
        }

        return NA;
    }

    protected boolean isExtracted(String infobox, String infoboxAtt) {
        return infobox.contains(infoboxAtt);
    }

    public String getCorefType() {
        return corefType;
    }

    protected boolean titleNumberMatch(String title) {
        Pattern titlePattern = Pattern.compile("\\s?\\d\\d?th\\s|[12][90][0-9][0-9]|\\b[MDCLXVI]+\\b");
        Matcher titleMatcher = titlePattern.matcher(title);
        return titleMatcher.find();
    }

    public boolean isSpanSingleMonth(String infoBox) {
        String dateLine = extractDateLine(infoBox);
        String dateString = extractDateString(dateLine);
        Pattern p = Pattern.compile("PT?(\\d+)([DHM])");
        Matcher matcher = p.matcher(dateString);
        return matcher.matches() && ((matcher.group(2).equals("H") && Integer.parseInt(matcher.group(1)) <= 28*24) ||
                (matcher.group(2).equals("D") && Integer.parseInt(matcher.group(1)) <= 28) ||
                (matcher.group(2).equals("M") && Integer.parseInt(matcher.group(1)) == 1));
    }

    public String extractDateLine(String infoBox) {
        String date = "";
        if(infoBox != null && infoBox.contains("date")) {
            String dateSubStr = infoBox.substring(infoBox.indexOf("date"));
            if(dateSubStr.contains("<br>")) {
                date = dateSubStr.substring(0, dateSubStr.indexOf("<br>"));
            } else if(dateSubStr.contains("\n")) {
                date = dateSubStr.substring(0, dateSubStr.indexOf("\n"));
            } else {
                date = dateSubStr;
            }
        }

        return date.replaceAll("\\s+", " ");
    }

    public String extractLocationLine(String infoBox) {
        String location = "";
        if(infoBox != null && infoBox.contains("location")) {
            String locationSubStr = infoBox.substring(infoBox.indexOf("location"));
            if(locationSubStr.contains("<br>")) {
                location = locationSubStr.substring(0, locationSubStr.indexOf("<br>"));
            } else if(locationSubStr.contains("\n")) {
                location = locationSubStr.substring(0, locationSubStr.indexOf("\n"));
            } else {
                location = locationSubStr;
            }
        }

        return location.replaceAll("\\s+", " ");
    }

    public String extractDateString(String dateline) {
        String ret = "";
        if (!dateline.isEmpty() && !dateline.startsWith("dates")) {
            String finalDateline = dateline.toLowerCase();
            if (finalDateline.contains("=")) {
                finalDateline = finalDateline.substring(finalDateline.indexOf("=") + 1);
            }

            if(finalDateline.contains("start and end date")) {
                finalDateline = convertStartAndEndDate(dateline);
            } else if(finalDateline.contains("start date") || finalDateline.contains("end date") ||
                    finalDateline.contains("start date and")) {
                String[] dateSplit = null;
                if(finalDateline.contains("<br />")) {
                    dateSplit = finalDateline.split("(<br />|<br/>)");
                } else if(finalDateline.contains("-") || finalDateline.contains("–")) {
                    dateSplit = finalDateline.split("[-–]");
                }
                if (dateSplit != null && dateSplit.length == 2) {
                    finalDateline = "from " + convertStartDateAgeToDate(dateSplit[0]) + " to " + convertStartDateAgeToDate(dateSplit[1]);
                } else {
                    finalDateline = convertStartDateAgeToDate(finalDateline);
                }
            }

            if(finalDateline.contains("-") || finalDateline.contains("–")) {
                finalDateline = finalDateline.replaceAll("[-–]", " to ");
            }

            CoreDocument coreDocument = StanfordNlpApi.withPosAnnotate(finalDateline);
            if (coreDocument.entityMentions() != null) {
                for (CoreEntityMention cem : coreDocument.entityMentions()) {
                    Timex timex = cem.coreMap().get(TimeAnnotations.TimexAnnotation.class);
                    if (timex != null) {
                        if (timex.timexType().equals("DURATION")) {
                            String begin = timex.range().begin;
                            String end = timex.range().end;
                            try {
                                return getDuration(begin, end, timex.range().duration);
                            } catch (ParseException ignored) {
                            }
                        } else if (timex.timexType().equals("DATE")) {
                            return timex.range().duration;
                        }
                    }
                }
            }
        }

        return ret;
    }

    private String convertStartAndEndDate(String dateLine) {
        Pattern p = Pattern.compile("(start and end dates?)(\\|df=yes)?\\|(\\d{4})\\|(\\d+)\\|(\\d+)\\|(\\d{4})\\|(\\d+)\\|(\\d+)");
        Matcher matcher = p.matcher(dateLine);
        if (matcher.find()) {
            String yearStrt = matcher.group(3);
            String monthStrt = fixDigit(matcher.group(4));
            String dayStart = fixDigit(matcher.group(5));

            String yearEnd = matcher.group(6);
            String monthEnd = fixDigit(matcher.group(7));
            String dayEnd = fixDigit(matcher.group(8));

            return "from " + yearStrt + "/" + monthStrt + "/" + dayStart +
                    " to " + yearEnd + "/" + monthEnd + "/" + dayEnd;
        }

        return dateLine;
    }

    private String convertStartDateAgeToDate(String dateLine) {
        Pattern p = Pattern.compile("(start date and age|start date|end date)(\\|df=yes)?\\|(\\d{4})\\|(\\d+)\\|(\\d+)");
        Matcher matcher = p.matcher(dateLine);
        if (matcher.find()) {
            String year = matcher.group(3);
            String month = fixDigit(matcher.group(4));
            String day = fixDigit(matcher.group(5));
            return year + "/" + month + "/" + day;
        }

        return dateLine;
    }

    private String getDuration(String begin, String end, String timx) throws ParseException {
        String[] splitBeg = begin.split("-");
        String[] splitEnd = end.split("-");
        if(splitBeg.length == 3 && splitEnd.length == 3) {
            if (splitBeg[0].equals("XXXX")) {
                splitBeg[0] = splitEnd[0];
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar begCal = Calendar.getInstance();
            begCal.setTime(sdf.parse(String.join("-", splitBeg)));
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(sdf.parse(String.join("-", splitEnd)));
            long between = ChronoUnit.HOURS.between(begCal.toInstant(), endCal.toInstant());
            return "PT" + between + "H";
        }

        return timx;
    }

    protected boolean hasDateAndLocation(String infoBox) {
        if (infoBox != null) {
            String dateLine = extractDateLine(infoBox);
            String locationLine = extractLocationLine(infoBox);

            return locationLine != null && !locationLine.isEmpty()
                    && dateLine != null && !dateLine.isEmpty()
                    && !dateLine.contains("{{startandenddates")
                    && !dateLine.contains("startdate|yyyy|mm|dd");
        }

        return false;
    }

    private String fixDigit(String num) {
        if (num.length() < 2) {
            num = "0" + num;
        }
        return num;
    }
}
