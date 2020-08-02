package wec.extractors;

import wec.DefaultInfoboxExtractor;

import java.util.List;

public class PersonInfoboxExtractor extends DefaultInfoboxExtractor {

    public PersonInfoboxExtractor(String corefType, List<String> infoboxs) {
        super(corefType, infoboxs);
    }

    @Override
    public String extractMatchedInfobox(String infobox, String title) {
        infobox = infobox.toLowerCase().replaceAll(" ", "");
        if(infobox.contains("birth_name") || infobox.contains("birth_date") ||
                infobox.contains("birth_place")) {
            return this.getCorefType();
        }

        return DefaultInfoboxExtractor.NA;
    }
}