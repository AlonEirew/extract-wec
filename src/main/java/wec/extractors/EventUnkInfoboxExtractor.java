package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

public class EventUnkInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.EVENT};

    public EventUnkInfoboxExtractor() {
        super(subTypes, CorefType.EVENT_UNK);
    }

    @Override
    public CorefSubType extract(String infobox, String title) {
        infobox = infobox.toLowerCase().replaceAll(" ", "");
        if(this.isExtracted(infobox, "{{infoboxevent")) {
            return CorefSubType.EVENT;
        }

        return CorefSubType.NA;
    }
}
