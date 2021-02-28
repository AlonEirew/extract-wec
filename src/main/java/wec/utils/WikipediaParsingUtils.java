package wec.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikipediaParsingUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaParsingUtils.class);

    @Deprecated
    public static String cleanParenthesesBkp(String html) {
        String cleanHtml = html;
        Pattern pat1 = Pattern.compile("(?s)\\{\\{.*?}}");
        Pattern pat2 = Pattern.compile("(?s)\\[.*?]");
        Matcher match1 = pat1.matcher(cleanHtml);

        while (match1.find()) {
            cleanHtml = match1.replaceAll("");
            match1 = pat1.matcher(cleanHtml);
        }

        Matcher match2 = pat2.matcher(cleanHtml);
        while (match2.find()) {
            cleanHtml = match2.replaceAll("");
            match2 = pat2.matcher(cleanHtml);
        }

        return cleanHtml;
    }

    public static String cleanParentheses(String html) {
        String cleanHtml = html;
        while (cleanHtml.contains("{")) {
            int start = cleanHtml.indexOf("{") + 1;
            int end = findEndParenthesesIndex(start, cleanHtml, '{', '}');
            if (end != -1) {
                String removeString = cleanHtml.substring(start - 1, end + 1);
                cleanHtml = StringUtils.remove(cleanHtml, removeString);
            } else {
                LOGGER.debug("Failed to remove all page parentheses");
                break;
            }
        }

        return cleanHtml;
    }

    private static int findEndParenthesesIndex(int start, String str, char parentStart, char parentEnd) {
        int parentStartCount = 0;
        for(int i = start ; i < str.length() ; i++) {
            if(str.charAt(i) == parentEnd) {
                if (parentStartCount == 0) {
                    return i;
                } else {
                    parentStartCount --;
                }
            } else if (str.charAt(i) == parentStart) {
                parentStartCount ++;
            }
        }
        return -1;
    }
}
