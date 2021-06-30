package wec.extractors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import wec.data.WECContext;

import java.util.ArrayList;
import java.util.List;

public class ExtractFirstParagraph extends WikipediaLinkExtractor {
    
    @Override
    protected List<WECContext> extractMentions(String pageName, String cleanHtml) {
        List<WECContext> finalResults = new ArrayList<>();
        Document doc = Jsoup.parse(cleanHtml);
        Elements pElements = doc.getElementsByTag("p");
        if(!pElements.isEmpty()) {
            Element paragraph = pElements.get(0);
            finalResults.add(extractFromParagraph(pageName, paragraph));
        }
        return finalResults;
    }
}
