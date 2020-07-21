package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

public class AccidentInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.AIRLINE_ACCIDENT, CorefSubType.RAIL_ACCIDENT, CorefSubType.BUS_ACCIDENT};

    public AccidentInfoboxExtractor() {
        super(subTypes, CorefType.ACCIDENT_EVENT);
    }

    @Override
    protected CorefSubType extract(String infobox, String title) {
        if(infobox.contains("{{infoboxairlinerincident") || infobox.contains("{{infoboxairlineraccident") ||
                infobox.contains("{{infoboxaircraftcrash") || infobox.contains("{{infoboxaircraftaccident") ||
                infobox.contains("{{infoboxaircraftincident") || infobox.contains("{{infoboxaircraftoccurrence")) {
            return CorefSubType.AIRLINE_ACCIDENT;
        } else if(infobox.contains("{{infoboxrailaccident")) {
            return CorefSubType.RAIL_ACCIDENT;
        } else if(infobox.contains("{{infoboxbusaccident")) {
            return CorefSubType.BUS_ACCIDENT;
        }

        return CorefSubType.NA;
    }
}
