package wec.extractors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import wec.data.RawElasticResult;
import wec.data.WECContext;
import wec.data.WECCoref;
import wec.data.WECMention;
import info.bliki.wiki.model.WikiModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import wec.utils.WikipediaParsingUtils;

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
        String cleanHtml = WikipediaParsingUtils.cleanParentheses(htmlText);
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
                        try {
                            String decodedLink = URLDecoder.decode(linkHref, StandardCharsets.UTF_8);
                            WECMention mention = new WECMention(
                                    WECCoref.getAndSetIfNotExist(decodedLink),
                                    text,
                                    startIndex,
                                    index-1,
                                    pageName,
                                    null);

                            paragraphMentions.add(mention);
                        } catch (IllegalArgumentException ignored) { }
                    }
                }
            }

            WECContext wecContext = new WECContext(contextAsStringList);

            for(WECMention ment : paragraphMentions) {
                ment.setContext(wecContext);
            }

            finalResults.addAll(paragraphMentions);
        }

        return finalResults;
    }
}
