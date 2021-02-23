package wec.extractors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WikipediaLinkExtractor implements IExtractor<List<WECMention>> {

    @Override
    public List<WECMention> extract(RawElasticResult rawElasticResult) {
        String pageName = rawElasticResult.getTitle();
        String text = rawElasticResult.getText();
        String htmlText = WikiModel.toHtml(text);
        String cleanHtml = WikipediaUtils.cleanParentheses(htmlText);
        return extractMentions(pageName, cleanHtml);
    }

    private List<WECMention> extractMentions(String pageName, String cleanHtml) {
        List<WECMention> finalResults = new ArrayList<>();
        Document doc = Jsoup.parse(cleanHtml);
        Elements pElements = doc.getElementsByTag("p");
        for (Element paragraph : pElements) {
            int index = 0;
            final JsonArray contextAsStringList = new JsonArray();
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
                        JsonObject jo = new JsonObject();
                        jo.addProperty(splText[i], i+index);
                        contextAsStringList.add(jo);
                    }
                    int startIndex = index;
                    index = index+i;

                    if(linkHref != null) {
                        String decodedLink = URLDecoder.decode(linkHref, StandardCharsets.UTF_8);
                        WECCoref wecCoref = WECCoref.getAndSetIfNotExist(decodedLink);
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
