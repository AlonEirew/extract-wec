package wec;

import data.CorefSubType;
import data.CorefType;
import data.RawElasticResult;
import data.WECCoref;

public class PersonOrEventFilter implements ICorefFilter {


    @Override
    public boolean isConditionMet(RawElasticResult result) {
        boolean retCond = false;

        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            final CorefSubType isPerson = CorefSubType.NA; //WECLinksExtractor.isPerson(result.getText());
            final CorefSubType isDisaster = WECLinksExtractor.isDisaster(result.getText());
            final CorefSubType isElection = CorefSubType.NA; //WECLinksExtractor.isElection(result.getText(), result.getTitle());
            final CorefSubType isCivilAttack = WECLinksExtractor.isCivilAttack(result.getText());
            final CorefSubType isAccidentEvent = WECLinksExtractor.isAccident(result.getText());
            final CorefSubType isSportEvent = WECLinksExtractor.isSportEvent(result.getText(), result.getTitle());
            final CorefSubType isAwardEvent = WECLinksExtractor.isAwardEvent(result.getText(), result.getTitle());
            final CorefSubType isConcreteGeneralEvent = WECLinksExtractor.isConcreteGeneralEvent(result.getText(), result.getTitle());
            final boolean isGeneralEvent = false; //WECLinksExtractor.isGeneralEvent(result.getText());
            final boolean isHistoricalEvent = false; //WECLinksExtractor.isHistoricalEvent(result.getText());
            final boolean isInfoBoxEvent = false; //WECLinksExtractor.hasDateAndLocation(result.getText());

            retCond = isPerson != CorefSubType.NA || isDisaster != CorefSubType.NA || isElection != CorefSubType.NA ||
                    isCivilAttack != CorefSubType.NA || isAccidentEvent != CorefSubType.NA || isSportEvent != CorefSubType.NA ||
                    isAwardEvent != CorefSubType.NA || isConcreteGeneralEvent != CorefSubType.NA;

            if (retCond) {
                WECCoref wecCoref = WECCoref.getAndSetIfNotExist(result.getTitle());
                if (isPerson != CorefSubType.NA) {
                    wecCoref.setCorefType(CorefType.PERSON);
                    wecCoref.setCorefSubType(isPerson);
                } else if (isDisaster != CorefSubType.NA) {
                    wecCoref.setCorefType(CorefType.DISASTER_EVENT);
                    wecCoref.setCorefSubType(isDisaster);
                } else if (isElection != CorefSubType.NA) {
                    wecCoref.setCorefType(CorefType.ELECTION_EVENT);
                    wecCoref.setCorefSubType(isElection);
                } else if (isCivilAttack != CorefSubType.NA) {
                    wecCoref.setCorefType(CorefType.CIVIL_ATTACK_EVENT);
                    wecCoref.setCorefSubType(isCivilAttack);
                } else if (isAccidentEvent != CorefSubType.NA) {
                    wecCoref.setCorefType(CorefType.ACCIDENT_EVENT);
                    wecCoref.setCorefSubType(isAccidentEvent);
                } else if (isSportEvent != CorefSubType.NA) {
                    wecCoref.setCorefType(CorefType.SPORT_EVENT);
                    wecCoref.setCorefSubType(isSportEvent);
                } else if (isAwardEvent != CorefSubType.NA) {
                    wecCoref.setCorefType(CorefType.AWARD_EVENT);
                    wecCoref.setCorefSubType(isAwardEvent);
                } else if (isConcreteGeneralEvent != CorefSubType.NA) {
                    wecCoref.setCorefType(CorefType.CONCRETE_GENERAL_EVENT);
                    wecCoref.setCorefSubType(isConcreteGeneralEvent);
                } else if (isHistoricalEvent) {
                    wecCoref.setCorefType(CorefType.HISTORICAL_EVENT);
                } else {
                    wecCoref.setCorefType(CorefType.EVENT_UNK);
                }
            }
        }

        return !retCond;
    }
}
