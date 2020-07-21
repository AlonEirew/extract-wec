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
    protected CorefSubType extract(String infobox, String title) {
        if(super.isExtracted(infobox, title, "{{infoboxhistoricalevent")) {
            return CorefSubType.HISTORICAL;
        }

        return CorefSubType.NA;
    }
}
