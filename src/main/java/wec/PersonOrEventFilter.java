package wec;

import data.CorefType;
import data.RawElasticResult;
import data.WECCoref;

public class PersonOrEventFilter implements ICorefFilter {


    @Override
    public boolean isConditionMet(RawElasticResult result) {
        boolean retCond = false;

        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            final boolean isPerson = WECLinksExtractor.isPerson(result.getText());
            final boolean isDisaster = WECLinksExtractor.isDisaster(result.getText());
            final boolean isElection = WECLinksExtractor.isElection(result.getText(), result.getTitle());
            final boolean isCivilAttack = WECLinksExtractor.isCivilAttack(result.getText());
            final boolean isAccidentEvent = WECLinksExtractor.isAccident(result.getText());
            final boolean isSportEvent = WECLinksExtractor.isSportEvent(result.getText(), result.getTitle());
            final boolean isAwardEvent = WECLinksExtractor.isAwardEvent(result.getText(), result.getTitle());
            final boolean isConcreteGeneralEvent = WECLinksExtractor.isConcreteGeneralEvent(result.getText(), result.getTitle());
            final boolean isGeneralEvent = WECLinksExtractor.isGeneralEvent(result.getText());
            final boolean isHistoricalEvent = WECLinksExtractor.isHistoricalEvent(result.getText());
            final boolean isInfoBoxEvent = WECLinksExtractor.hasDateAndLocation(result.getText());

            if (isPerson) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.PERSON);
            } else if (isDisaster) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.DISASTER_EVENT);
            } else if (isElection) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.ELECTION_EVENT);
            } else if (isCivilAttack) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.CIVIL_ATTACK_EVENT);
            } else if (isAccidentEvent) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.ACCIDENT_EVENT);
            } else if (isSportEvent) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.SPORT_EVENT);
            } else if (isAwardEvent) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.AWARD_EVENT);
            } else if (isConcreteGeneralEvent) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.CONCRETE_GENERAL_EVENT);
            } else if (isHistoricalEvent) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.HISTORICAL_EVENT);
            } else if (isGeneralEvent || isInfoBoxEvent) {
                WECCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.EVENT_UNK);
            }

            retCond = isPerson || isDisaster || isElection || isCivilAttack || isAccidentEvent || isSportEvent
                    || isAwardEvent || isConcreteGeneralEvent || isGeneralEvent || isHistoricalEvent || isInfoBoxEvent;
        }

        return !retCond;
    }
}
