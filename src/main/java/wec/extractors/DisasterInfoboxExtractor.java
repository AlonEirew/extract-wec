package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

public class DisasterInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.EARTHQUAKE, CorefSubType.FIRE, CorefSubType.STORM,
            CorefSubType.FLOOD, CorefSubType.OIL_SPILL, CorefSubType.ERUPTION};

    public DisasterInfoboxExtractor() {
        super(subTypes, CorefType.DISASTER_EVENT);
    }

    @Override
    protected CorefSubType extract(String infobox, String title) {
        if (infobox.contains("{{infoboxearthquake")) {
            return CorefSubType.EARTHQUAKE;
        } else if(infobox.contains("{{infoboxwildfire")) {
            return CorefSubType.FIRE;
//        } else if(infoBox.contains("infoboxstorm/sandbox") || infoBox.contains("infoboxstorm") || infoBox.contains("{{infoboxhurricane")) {
//            return CorefSubType.STORM;
        } else if(infobox.contains("infoboxflood")) {
            return CorefSubType.FLOOD;
        } else if(infobox.contains("infoboxoilspill")) {
            return CorefSubType.OIL_SPILL;
        } else if(infobox.contains("infoboxeruption")) {
            return CorefSubType.ERUPTION;
        }

        return CorefSubType.NA;
    }
}
