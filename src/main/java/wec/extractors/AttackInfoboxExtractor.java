package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

public class AttackInfoboxExtractor extends AInfoboxExtractor {

    private static final CorefSubType[] subTypes = {CorefSubType.CIVILIAN_ATTACK, CorefSubType.TERRORIST_ATTACK,
            CorefSubType.MILITARY_ATTACK, CorefSubType.CIVIL_CONFLICT, CorefSubType.MILITARY_CONFLICT};

    public AttackInfoboxExtractor() {
        super(subTypes, CorefType.CIVIL_ATTACK_EVENT);
    }

    @Override
    public CorefSubType extract(String infobox, String title) {
        final boolean isSingleDay = this.isSpanSingleMonth(infobox);

        String infoboxLow = infobox.toLowerCase().replaceAll(" ", "");
        if(isSingleDay) {
            if (infoboxLow.contains("{{infoboxcivilianattack")) {
                return CorefSubType.CIVILIAN_ATTACK;
            } else if (infoboxLow.contains("{{infoboxterroristattack")) {
                return CorefSubType.TERRORIST_ATTACK;
            } else if (infoboxLow.contains("{{infoboxmilitaryattack")) {
                return CorefSubType.MILITARY_ATTACK; // REMOVE IF NOT TIME
            } else if (infoboxLow.contains("{{infoboxcivilconflict")) {
                return CorefSubType.CIVIL_CONFLICT; // REMOVE IF NOT TIME
            } else if (infoboxLow.contains("{{infoboxmilitaryconflict")) {
                return CorefSubType.MILITARY_CONFLICT; // REMOVE IF NOT TIME
            }
        }

        return CorefSubType.NA;
    }
}
