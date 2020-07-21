package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

public class PersonInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.PERSON};

    public PersonInfoboxExtractor() {
        super(subTypes, CorefType.PERSON);
    }

    @Override
    public CorefSubType extract(String infobox, String title) {
        if(infobox.contains("birth_name") || infobox.contains("birth_date") ||
                infobox.contains("birth_place")) {
            return CorefSubType.PERSON;
        }

        return CorefSubType.NA;
    }
}
