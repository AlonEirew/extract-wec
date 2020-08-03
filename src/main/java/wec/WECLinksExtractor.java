package wec;

import data.RawElasticResult;
import data.WECMention;
import info.bliki.wiki.model.WikiModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WECLinksExtractor {

    public static List<WECMention> extractFromWikipedia(RawElasticResult rawElasticResult) {
        List<WECMention> finalResults = new ArrayList<>();

        String pageName = rawElasticResult.getTitle();
        String text = rawElasticResult.getText();

        if(pageName.toLowerCase().startsWith("file:") ||
                pageName.toLowerCase().startsWith("wikipedia:") || pageName.toLowerCase().startsWith("category:") ||
                pageName.toLowerCase().contains("list of") || pageName.toLowerCase().contains("lists of") ||
                pageName.toLowerCase().contains("listings")) {
            return finalResults;
        }

        String htmlText = WikiModel.toHtml(text);
        String cleanHtml = cleanTextField(htmlText);
        finalResults = extractMentions(pageName, cleanHtml);

        return finalResults;
    }

    private static List<WECMention> extractMentions(String pageName, String cleanHtml) {
        List<WECMention> finalResults = new ArrayList<>();
        Document doc = Jsoup.parse(cleanHtml);
        Elements pElements = doc.getElementsByTag("p");
        for (Element paragraph : pElements) {
            int index = 0;
            final List<Map.Entry<String, Integer>> contextAsStringList = new ArrayList<>();
            List<Node> nodes = paragraph.childNodes();
            List<WECMention> paragraphResults = new ArrayList<>();
            for(Node child : nodes) {
                String text = null;
                String linkHref = null;
                if(child instanceof Element) {
                    if (((Element) child).tag().getName().equals("a")) {
                        linkHref = child.attr("href");
                        linkHref = linkHref.substring(linkHref.indexOf("/") + 1).replaceAll("_", " ");
                    }

                    text = ((Element) child).text();
                }
                else if (child instanceof TextNode) {
                    text = ((TextNode) child).text();
                }

                if(text != null) {
                    text = text.trim();
                    String[] splText = text.split(" ");

                    int i = 0;
                    for(; i < splText.length ; i++) {
                        contextAsStringList.add(new AbstractMap.SimpleEntry<>(splText[i], i+index));
                    }
                    int startIndex = index;
                    index = index+i;

                    if(linkHref != null) {
                        WECMention mention = new WECMention(pageName);
                        mention.setMentionText(text);
                        mention.setCorefChain(linkHref);
                        mention.setTokenStart(startIndex);
                        mention.setTokenEnd(index-1);
                        paragraphResults.add(mention);
                    }
                }
            }

            for(WECMention ment : paragraphResults) {
                ment.setContext(contextAsStringList);
            }

            finalResults.addAll(paragraphResults);
        }

        return finalResults;
    }

    private static String cleanTextField(String html) {
        String cleanHtml = html;
        Pattern pat1 = Pattern.compile("(?s)\\{\\{[^{]*?\\}\\}");
        Matcher match1 = pat1.matcher(cleanHtml);
        while (match1.find()) {
            cleanHtml = match1.replaceAll("");
            match1 = pat1.matcher(cleanHtml);
        }

        return cleanHtml;
    }

    public static String extractPageInfoBox(String pageText) {
        return extractPageInfoBox(pageText, false);
    }

    public static String extractPageInfoBox(String pageText, boolean toLowerForm) {
//        String text = pageText.toLowerCase().replaceAll(" ", "");
        StringBuilder infoBoxFinal = new StringBuilder();

        final int beginIndex = pageText.indexOf("{{Infobox");
        if (beginIndex != -1) {
            final String infoboxSubstring = pageText.substring(beginIndex);
            int infoBarCount = 0;
            for (int i = 0; i < infoboxSubstring.length(); i++) {
                final char c = infoboxSubstring.charAt(i);
                if (c == '}') {
                    infoBarCount--;
                    if (infoBarCount == 0) {
                        infoBoxFinal.append(c);
                        break;
                    }
                } else if (c == '{') {
                    infoBarCount++;
                }

                infoBoxFinal.append(c);
            }
        }

        if(toLowerForm) {
            return infoBoxFinal.toString().toLowerCase().replaceAll(" ", "");
        }

        return infoBoxFinal.toString();
    }
}
