package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

import java.util.Set;

public class AttackInfoboxExtractor extends AInfoboxExtractor {

    private static CorefSubType[] subTypes = {CorefSubType.CIVILIAN_ATTACK, CorefSubType.TERRORIST_ATTACK,
            CorefSubType.MILITARY_ATTACK, CorefSubType.CIVIL_CONFLICT, CorefSubType.MILITARY_CONFLICT};

    public AttackInfoboxExtractor() {
        super(subTypes, CorefType.CIVIL_ATTACK_EVENT);
    }

    @Override
    protected CorefSubType extract(String infobox, String title) {
        final Set<String> uniqueDates = this.getYears(infobox);

//        if (uniqueDates != null && uniqueDates.size() < 2) {
        if (infobox.contains("{{infoboxcivilianattack")) {
            return CorefSubType.CIVILIAN_ATTACK;
        } else if(infobox.contains("{{infoboxterroristattack")) {
            return CorefSubType.TERRORIST_ATTACK;
        }
//            else if(infoBox.contains("{{infoboxmilitaryattack")) {
//                return CorefSubType.MILITARY_ATTACK;
//            } else if(infoBox.contains("{{infoboxcivilconflict")) {
//                return CorefSubType.CIVIL_CONFLICT;
//            } else if(infoBox.contains("{{infoboxmilitaryconflict")) {
//                return CorefSubType.MILITARY_CONFLICT;
//            }
//        }

        return CorefSubType.NA;
    }
}
