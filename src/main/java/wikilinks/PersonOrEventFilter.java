package wikilinks;

import data.CorefType;
import data.WikiLinksCoref;
import data.WikiLinksMention;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class PersonOrEventFilter implements ICorefFilter<WikiLinksMention> {

    private CreateWikiLinks wikiLinks;

    private static final String[] EVENT_TYPES = {
            "mass murder",
            "stabbing",
            "aircraft hijackings",
            "suicide attack",
            "terrorism",
            "cyberattack",
            "mass shooting",
            "runway collision",
            "mid-air collision",
            "pilot error",
            "fault (geology)",
            "megathrust earthquake",
            "hurricane"};

    public PersonOrEventFilter(CreateWikiLinks wikiLinks) {
        this.wikiLinks = wikiLinks;
    }

    @Override
    public boolean isConditionMet(WikiLinksMention input) {
        boolean retCond = false;

        try {
            if(WikiLinksCoref.getGlobalCorefMap().containsKey(input.getCorefChain().getCorefValue())) {
                final String pageText = this.wikiLinks.getPageText(input.getCorefChain().getCorefValue());
                if (pageText != null && !pageText.isEmpty()) {
                    final String infoBox = WikiLinksExtractor.extractPageInfoBox(pageText);
                    final boolean isPerson = WikiLinksExtractor.isPersonPage(infoBox);
                    final boolean isElection = WikiLinksExtractor.isElection(infoBox);
                    final Set<String> extractTypes = WikiLinksExtractor.extractTypes(infoBox);
                    final boolean isEventType = !Collections.disjoint(extractTypes, Arrays.asList(EVENT_TYPES));
                    final boolean isInfoBoxEvent = WikiLinksExtractor.hasDateAndLocation(infoBox);

                    if(isPerson && isEventType) {
                        WikiLinksCoref.getCorefChain(input.getCorefChain().getCorefValue()).setCorefType(CorefType.PERSON_AND_EVENT);
                    } else if(isPerson) {
                        WikiLinksCoref.getCorefChain(input.getCorefChain().getCorefValue()).setCorefType(CorefType.PERSON);
                    } else if(isEventType) {
                        WikiLinksCoref.getCorefChain(input.getCorefChain().getCorefValue()).setCorefType(CorefType.EVENT_TERROR);
                    } else if(isElection) {
                        WikiLinksCoref.getCorefChain(input.getCorefChain().getCorefValue()).setCorefType(CorefType.EVENT_ELECTION);
                    } else if(isInfoBoxEvent) {
                        WikiLinksCoref.getCorefChain(input.getCorefChain().getCorefValue()).setCorefType(CorefType.UNK_EVENT);
                    }

                    retCond = isPerson || isEventType || isElection || isInfoBoxEvent;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return !retCond;
    }
}
