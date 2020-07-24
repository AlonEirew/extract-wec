package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralEventInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.SOLAR_ECLIPSE, CorefSubType.NEWS_EVENT, CorefSubType.CONCERT,
            CorefSubType.WEAPONS_TEST, CorefSubType.MEETING, CorefSubType.FESTIVAL};

    private static final Pattern CONCRETE_EVENT = Pattern.compile("\\{\\{infobox[\\w\\|]*?(solareclipse|newsevent|concert|weaponstest|explosivetest" +
            "|summit|convention|conference|summitmeeting|festival)");

    public GeneralEventInfoboxExtractor() {
        super(subTypes, CorefType.CONCRETE_GENERAL_EVENT);
    }

    @Override
    public CorefSubType extract(String infobox, String title) {
        infobox = infobox.toLowerCase().replaceAll(" ", "");
        Matcher concreteMatcher = CONCRETE_EVENT.matcher(infobox);
        boolean titleMatch = titleNumberMatch(title);
        final boolean isSingleDay = isSpanSingleMonth(infobox);
        if (concreteMatcher.find() && (titleMatch || isSingleDay)) {
            if(concreteMatcher.group(1).contains("solareclipse")) {
                return CorefSubType.SOLAR_ECLIPSE;
            } else if(concreteMatcher.group(1).contains("newsevent")) {
                return CorefSubType.NEWS_EVENT;
            } else if(concreteMatcher.group(1).contains("concert")) {
                return CorefSubType.CONCERT;
            } else if(concreteMatcher.group(1).contains("weaponstest") || concreteMatcher.group(1).contains("explosivetest")) {
                return CorefSubType.WEAPONS_TEST;
            } else if(concreteMatcher.group(1).contains("summit") || concreteMatcher.group(1).contains("convention") ||
                    concreteMatcher.group(1).contains("conference") || concreteMatcher.group(1).contains("summitmeeting")) {
                return CorefSubType.MEETING;
            } else if(concreteMatcher.group(1).contains("festival")) {
                return CorefSubType.FESTIVAL;
            }
        }

        return CorefSubType.NA;
    }
}
