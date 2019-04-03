package wikilinks;

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
            "pilot error"};

    public PersonOrEventFilter(CreateWikiLinks wikiLinks) {
        this.wikiLinks = wikiLinks;
    }

    @Override
    public boolean isConditionMet(WikiLinksMention input) {
        boolean retCond = false;

        try {
            final String pageText = this.wikiLinks.getPageText(input.getCorefChain().getCorefValue());
            final boolean isPerson = WikiLinksExtractor.isPersonPage(pageText);
            final Set<String> extractTypes = WikiLinksExtractor.extractTypes(pageText);
            final boolean isEventType = !Collections.disjoint(extractTypes, Arrays.asList(EVENT_TYPES));

            retCond = isPerson || isEventType;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return retCond;
    }
}
