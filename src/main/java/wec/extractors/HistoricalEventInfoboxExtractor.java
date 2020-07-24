package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

public class HistoricalEventInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.HISTORICAL};

    public HistoricalEventInfoboxExtractor() {
        super(subTypes, CorefType.HISTORICAL_EVENT);
    }

    @Override
    public CorefSubType extract(String infobox, String title) {
        infobox = infobox.toLowerCase().replaceAll(" ", "");
        if(super.isExtracted(infobox, "{{infoboxhistoricalevent")) {
            return CorefSubType.HISTORICAL;
        }

        return CorefSubType.NA;
    }
}
