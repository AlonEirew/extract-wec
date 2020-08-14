package wec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultInfoboxValidator {

    public static final String NA = "NA";

    private final String corefType;
    private final Pattern pattern;

    public DefaultInfoboxValidator(String corefType, Pattern pattern) {
        this.corefType = corefType;
        this.pattern = pattern;
    }

    public String extractMatchedInfobox(String infobox, String title) {
        String infoboxLow = infobox.toLowerCase().replaceAll(" ", "");
        Matcher matcher = this.pattern.matcher(infoboxLow);

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
}
