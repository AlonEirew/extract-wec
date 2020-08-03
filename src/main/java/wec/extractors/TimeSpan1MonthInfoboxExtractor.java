package wec.extractors;

import wec.DefaultInfoboxExtractor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeSpan1MonthInfoboxExtractor extends DefaultInfoboxExtractor {

    public TimeSpan1MonthInfoboxExtractor(String corefType, List<String> infoboxs, Pattern pattern) {
        super(corefType, infoboxs, pattern);
    }

    @Override
    public String extractMatchedInfobox(String infobox, String title) {
        String extract = super.extractMatchedInfobox(infobox, title);

        if(!extract.equals(DefaultInfoboxExtractor.NA)) {
            if (this.isSpanSingleMonth(infobox)) {
                return extract;
            }
        }

        return DefaultInfoboxExtractor.NA;
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
}
