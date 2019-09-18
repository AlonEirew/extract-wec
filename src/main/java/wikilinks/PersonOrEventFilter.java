package wikilinks;

import data.CorefType;
import data.RawElasticResult;
import data.WikiLinksCoref;

public class PersonOrEventFilter implements ICorefFilter {


    @Override
    public boolean isConditionMet(RawElasticResult result) {
        boolean retCond = false;

        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            final boolean isPerson = WikiLinksExtractor.isPerson(result.getText());
            final boolean isDisaster = WikiLinksExtractor.isDisaster(result.getText());
            final boolean isElection = WikiLinksExtractor.isElection(result.getText(), result.getTitle());
            final boolean isCivilAttack = WikiLinksExtractor.isCivilAttack(result.getText());
            final boolean isAccidentEvent = WikiLinksExtractor.isAccident(result.getText());
            final boolean isSportEvent = WikiLinksExtractor.isSportEvent(result.getText(), result.getTitle());
            final boolean isAwardEvent = WikiLinksExtractor.isAwardEvent(result.getText(), result.getTitle());
            final boolean isConcreteGeneralEvent = WikiLinksExtractor.isConcreteGeneralEvent(result.getText(), result.getTitle());
            final boolean isGeneralEvent = WikiLinksExtractor.isGeneralEvent(result.getText());
            final boolean isHistoricalEvent = WikiLinksExtractor.isHistoricalEvent(result.getText());
            final boolean isInfoBoxEvent = WikiLinksExtractor.hasDateAndLocation(result.getText());

            if (isPerson) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.PERSON);
            } else if (isDisaster) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.DISASTER_EVENT);
            } else if (isElection) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.ELECTION_EVENT);
            } else if (isCivilAttack) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.CIVIL_ATTACK_EVENT);
            } else if (isAccidentEvent) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.ACCIDENT_EVENT);
            } else if (isSportEvent) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.SPORT_EVENT);
            } else if (isAwardEvent) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.AWARD_EVENT);
            } else if (isConcreteGeneralEvent) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.CONCRETE_GENERAL_EVENT);
            } else if (isHistoricalEvent) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.HISTORICAL_EVENT);
            } else if (isGeneralEvent || isInfoBoxEvent) {
                WikiLinksCoref.getAndSetIfNotExist(result.getTitle()).setCorefType(CorefType.EVENT_UNK);
            }

            retCond = isPerson || isDisaster || isElection || isCivilAttack || isAccidentEvent || isSportEvent
                    || isAwardEvent || isConcreteGeneralEvent || isGeneralEvent || isHistoricalEvent || isInfoBoxEvent;
        }

        return !retCond;
    }
}
