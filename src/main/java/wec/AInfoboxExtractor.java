package wec;

import data.CorefSubType;
import data.CorefType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AInfoboxExtractor {

    private CorefSubType[] subTypesArray;
    private CorefType corefType;

    public AInfoboxExtractor(CorefSubType[] subTypesArray, CorefType type) {
        this.subTypesArray = subTypesArray;
        this.corefType = type;
    }

    protected abstract CorefSubType extract(String infobox, String title);

    CorefType extractTypeFromSubType(CorefSubType subType) {
        if (Arrays.asList(this.subTypesArray).contains(subType)) {
            return this.corefType;
        }
        return CorefType.NA;
    }

    protected boolean isExtracted(String infobox, String title, String infoboxAtt) {
        return infobox.contains(infoboxAtt);
    }

    protected boolean titleNumberMatch(String title) {
        Pattern titlePattern = Pattern.compile("\\s?\\d\\d?th\\s|[12][90][0-9][0-9]|\\b[MDCLXVI]+\\b");
        Matcher titleMatcher = titlePattern.matcher(title);
        return titleMatcher.find();
    }

    protected Set<String> getYears(String infoBox) {
        Set<String> uniqueDates = null;
        Pattern yearPattern = Pattern.compile("[12][0-9][0-9][0-9]|[0-9][0-9][0-9]");

        Pattern dateEql = Pattern.compile("\\n\\|date=(.*)\n");
        Matcher matcher = dateEql.matcher(infoBox);

        if (matcher.find()) {
            String dateLine = matcher.group(1);
            if (!dateLine.isEmpty()) {
                uniqueDates = new HashSet<>();
            }

            Matcher yearMatch = yearPattern.matcher(dateLine);
            while (yearMatch.find()) {
                uniqueDates.add(yearMatch.group());
            }

            if (dateLine.contains("plainlist")) {
                uniqueDates.add("rej1");
                uniqueDates.add("rej2");
            }
        }

        return uniqueDates;
    }

    protected static boolean hasDateAndLocation(String infoBox) {
        if (infoBox != null) {
            String dateLine = null;
            String locationLine = null;
            for (String line : infoBox.split("\n")) {
                if (line.startsWith("|date=")) {
                    final String[] split = line.split("=");
                    if (split.length > 1) {
                        dateLine = split[1].trim();
                        if (dateLine.split("-").length != 1) {
                            dateLine = null;
                        }
                    }
                } else if (line.startsWith("|location=")) {
                    final String[] split = line.split("=");
                    if (split.length > 1) {
                        locationLine = split[1].trim();
                    }
                }
            }

            return locationLine != null && !locationLine.isEmpty()
                    && dateLine != null && !dateLine.isEmpty()
                    && !dateLine.contains("{{startandenddates")
                    && !dateLine.contains("startdate|yyyy|mm|dd");
        }

        return false;
    }

}
