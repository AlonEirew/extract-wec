package wec.extractors;

import wec.DefaultInfoboxExtractor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleWithDateInfoboxExtractor extends DefaultInfoboxExtractor {

    public TitleWithDateInfoboxExtractor(String corefType, List<String> infoboxs, Pattern pattern) {
        super(corefType, infoboxs, pattern);
    }

    @Override
    public String extractMatchedInfobox(String infobox, String title) {
        String extract = super.extractMatchedInfobox(infobox, title);
        boolean titleMatch = this.titleNumberMatch(title);
        if(titleMatch) {
            return extract;
        }

        return DefaultInfoboxExtractor.NA;
    }

    private boolean titleNumberMatch(String title) {
        Pattern titlePattern = Pattern.compile("\\s?\\d\\d?th\\s|[12][90][0-9][0-9]|\\b[MDCLXVI]+\\b");
        Matcher titleMatcher = titlePattern.matcher(title);
        return titleMatcher.find();
    }
}
