package wec.extractors;

import wec.DefaultInfoboxExtractor;

import java.util.List;

public class TimeSpan1MonthInfoboxExtractor extends DefaultInfoboxExtractor {

    public TimeSpan1MonthInfoboxExtractor(String corefType, List<String> infoboxs) {
        super(corefType, infoboxs);
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
}
