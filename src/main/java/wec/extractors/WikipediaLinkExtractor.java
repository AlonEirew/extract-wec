package wec.extractors;

import data.RawElasticResult;
import data.WECCoref;
import data.WECMention;
import info.bliki.wiki.model.WikiModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import utils.WikipediaUtils;
import wec.extractors.IExtractor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikipediaLinkExtractor implements IExtractor<List<WECMention>> {

    @Override
    public List<WECMention> extract(RawElasticResult rawElasticResult) {
        String pageName = rawElasticResult.getTitle();
        String text = rawElasticResult.getText();
        String htmlText = WikiModel.toHtml(text);
        String cleanHtml = WikipediaUtils.cleanTextField(htmlText);
        return extractMentions(pageName, cleanHtml);
    }

    private List<WECMention> extractMentions(String pageName, String cleanHtml) {
        List<WECMention> finalResults = new ArrayList<>();
        Document doc = Jsoup.parse(cleanHtml);
        Elements pElements = doc.getElementsByTag("p");
        for (Element paragraph : pElements) {
            int index = 0;
            final List<Map.Entry<String, Integer>> contextAsStringList = new ArrayList<>();
            List<Node> nodes = paragraph.childNodes();
            List<WECMention> paragraphMentions = new ArrayList<>();
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
                        WECCoref wecCoref = WECCoref.getAndSetIfNotExist(linkHref);
                        WECMention mention = new WECMention(
                                wecCoref,
                                text,
                                startIndex,
                                index-1,
                                pageName,
                                null);

                        paragraphMentions.add(mention);
                    }
                }
            }

            for(WECMention ment : paragraphMentions) {
                ment.setContext(contextAsStringList);
            }

            finalResults.addAll(paragraphMentions);
        }

        return finalResults;
    }
}
