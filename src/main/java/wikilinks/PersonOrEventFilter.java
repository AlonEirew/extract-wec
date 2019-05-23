package wikilinks;

import data.CorefType;
import data.RawElasticResult;
import data.WikiLinksCoref;

public class PersonOrEventFilter implements ICorefFilter<RawElasticResult> {


    @Override
    public boolean isConditionMet(RawElasticResult result) {
        boolean retCond = false;

        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            final boolean isPerson = WikiLinksExtractor.isPerson(result.getText());
            final boolean isDisaster = WikiLinksExtractor.isDisaster(result.getText());
            final boolean isElection = WikiLinksExtractor.isElection(result.getText());
            final boolean isCivilAttack = WikiLinksExtractor.isCivilAttack(result.getText());
            final boolean isAccidentEvent = WikiLinksExtractor.isAccident(result.getText());
            final boolean isSportEvent = WikiLinksExtractor.isSportEvent(result.getText());
            final boolean isAwardEvent = WikiLinksExtractor.isAwardEvent(result.getText(), result.getTitle());
            final boolean isNewsEvent = WikiLinksExtractor.isNewsEvent(result.getText());
            final boolean isGeneralEvent = WikiLinksExtractor.isGeneralEvent(result.getText());
            final boolean isHistoricalEvent = WikiLinksExtractor.isHistoricalEvent(result.getText());
            final boolean isInfoBoxEvent = WikiLinksExtractor.hasDateAndLocation(result.getText());

            if (isPerson) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.PERSON);
            } else if (isDisaster) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.DISASTER_EVENT);
            } else if (isElection) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.ELECTION_EVENT);
            } else if (isCivilAttack) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.CIVIL_ATTACK_EVENT);
            } else if (isAccidentEvent) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.ACCIDENT_EVENT);
            } else if (isSportEvent) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.SPORT_EVENT);
            } else if (isAwardEvent) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.AWARD_EVENT);
            } else if (isNewsEvent) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.NEWS_EVENT);
            } else if (isGeneralEvent) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.GENERAL_EVENT);
            } else if (isHistoricalEvent) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.HISTORICAL_EVENT);
            } else if (isInfoBoxEvent) {
                WikiLinksCoref.getAndSetIfNotExistCorefChain(result.getTitle()).setCorefType(CorefType.EVENT_UNK);
            }

            retCond = isPerson || isDisaster || isElection || isCivilAttack || isAccidentEvent || isSportEvent
                    || isAwardEvent || isNewsEvent || isGeneralEvent || isHistoricalEvent || isInfoBoxEvent;
        }

        return !retCond;
    }
}
