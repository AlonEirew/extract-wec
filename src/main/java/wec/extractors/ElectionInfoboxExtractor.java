package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElectionInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.ELECTION};

    private static final Pattern ELECTION_PATTERN = Pattern.compile("\\{\\{infobox[\\w\\|]*?(election)");

    public ElectionInfoboxExtractor() {
        super(subTypes, CorefType.ELECTION_EVENT);
    }

    @Override
    public CorefSubType extract(String infobox, String title) {
        boolean titleMatch = titleNumberMatch(title);
        Matcher linkMatcher = ELECTION_PATTERN.matcher(infobox);
        if(linkMatcher.find() && titleMatch) {
            return CorefSubType.ELECTION;
        }

        return CorefSubType.NA;
    }
}
