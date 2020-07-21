package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwardInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.AWARD, CorefSubType.CONTEST,
            CorefSubType.BEAUTY_PAGEANT};

    private static final Pattern AWARD_PATTERN = Pattern.compile("\\{\\{infobox[\\w\\|]*?(award|awards|contest|beautypageant)");

    public AwardInfoboxExtractor() {
        super(subTypes, CorefType.AWARD_EVENT);
    }

    @Override
    protected CorefSubType extract(String infobox, String title) {
        boolean titleMatch = this.titleNumberMatch(title);
        Matcher awardMatcher = AWARD_PATTERN.matcher(infobox);
        if (awardMatcher.find() && titleMatch) {
            if(awardMatcher.group(1).contains("award") || awardMatcher.group(1).contains("awards")) {
                return CorefSubType.AWARD;
            } else if(awardMatcher.group(1).contains("contest")) {
                return CorefSubType.CONTEST;
            } else if(awardMatcher.group(1).contains("beautypageant")) {
                return CorefSubType.BEAUTY_PAGEANT;
            }
        }

        return CorefSubType.NA;
    }
}
