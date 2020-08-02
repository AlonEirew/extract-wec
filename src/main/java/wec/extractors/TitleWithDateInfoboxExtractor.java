package wec.extractors;

import wec.DefaultInfoboxExtractor;

import java.util.List;

public class TitleWithDateInfoboxExtractor extends DefaultInfoboxExtractor {

    public TitleWithDateInfoboxExtractor(String corefType, List<String> infoboxs) {
        super(corefType, infoboxs);
    }

    @Override
    public String extractMatchedInfobox(String infobox, String title) {
        String extract = super.extractMatchedInfobox(infobox, title);
        boolean titleMatch = this.titleNumberMatch(title);
        if(titleMatch) {
            return extract;
        }

        return DefaultInfoboxExtractor.NA;
    }
}
