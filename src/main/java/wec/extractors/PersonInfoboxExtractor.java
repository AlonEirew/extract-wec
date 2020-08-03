package wec.extractors;

import wec.DefaultInfoboxExtractor;

import java.util.regex.Pattern;

public class PersonInfoboxExtractor extends DefaultInfoboxExtractor {

    public PersonInfoboxExtractor(String corefType, Pattern pattern) {
        super(corefType, pattern);
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
