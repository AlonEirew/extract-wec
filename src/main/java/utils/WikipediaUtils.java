package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikipediaUtils {
    public static String cleanTextField(String html) {
        String cleanHtml = html;
        Pattern pat1 = Pattern.compile("(?s)\\{\\{[^{]*?\\}\\}");
        Matcher match1 = pat1.matcher(cleanHtml);
        while (match1.find()) {
            cleanHtml = match1.replaceAll("");
            match1 = pat1.matcher(cleanHtml);
        }

        return cleanHtml;
    }
}
