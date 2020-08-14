package wec.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleWithDateInfoboxValidator extends DefaultInfoboxValidator {

    public TitleWithDateInfoboxValidator(String corefType, Pattern pattern) {
        super(corefType, pattern);
    }

    @Override
    public String validateMatchedInfobox(String infobox, String title) {
        String extract = super.validateMatchedInfobox(infobox, title);
        boolean titleMatch = this.titleNumberMatch(title);
        if(titleMatch) {
            return extract;
        }

        return DefaultInfoboxValidator.NA;
    }

    private boolean titleNumberMatch(String title) {
        Pattern titlePattern = Pattern.compile("\\s?\\d\\d?th\\s|[12][90][0-9][0-9]|\\b[MDCLXVI]+\\b");
        Matcher titleMatcher = titlePattern.matcher(title);
        return titleMatcher.find();
    }
}
