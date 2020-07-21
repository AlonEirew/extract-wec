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
    protected CorefSubType extract(String infobox, String title) {
        if(this.isExtracted(infobox, title, "{{infoboxevent")) {
            return CorefSubType.EVENT;
        }

        return CorefSubType.NA;
    }
}
