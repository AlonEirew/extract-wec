package data;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class WECEventWithRelMention {
    private int corefId;
    private String wikidataPageId;
    private String wikipediaLangPageTitle;
    private String elasticPageId;
    private String eventType;
    private String subEventType;
    private Set<String> aliases;
    private Set<String> partOf;
    private Set<String> hasPart;
    private Set<String> hasEffect;
    private Set<String> hasCause;
    private Set<String> hasImmediateCause;
    private Set<String> immediateCauseOf;

    public WECEventWithRelMention() {}

    public WECEventWithRelMention(WECCoref coref) {
        this.corefId = coref.getCorefId();
        this.wikipediaLangPageTitle = coref.getCorefValue();
        this.eventType = coref.getCorefType();
        this.subEventType = coref.getCorefSubType();

        this.aliases = new HashSet<>();
        this.partOf = new HashSet<>();
        this.hasPart = new HashSet<>();
        this.hasEffect = new HashSet<>();
        this.hasCause = new HashSet<>();
        this.hasImmediateCause = new HashSet<>();
        this.immediateCauseOf = new HashSet<>();
    }

    public int getCorefId() {
        return corefId;
    }

    public void setCorefId(int corefId) {
        this.corefId = corefId;
    }

    public void setWikidataPageId(String wikidataPageId) {
        this.wikidataPageId = wikidataPageId;
    }

    public void setWikipediaLangPageTitle(String wikipediaLangPageTitle) {
        this.wikipediaLangPageTitle = wikipediaLangPageTitle;
    }

    public String getWikidataPageId() {
        return wikidataPageId;
    }

    public String getElasticPageId() {
        return elasticPageId;
    }

    public void setElasticPageId(String elasticPageId) {
        this.elasticPageId = elasticPageId;
    }

    public String getWikipediaLangPageTitle() {
        return wikipediaLangPageTitle;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public Set<String> getPartOf() {
        return partOf;
    }

    public Set<String> getHasPart() {
        return hasPart;
    }

    public Set<String> getHasEffect() {
        return hasEffect;
    }

    public Set<String> getHasCause() {
        return hasCause;
    }

    public Set<String> getHasImmediateCause() {
        return hasImmediateCause;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public void setPartOf(Set<String> partOf) {
        this.partOf = partOf;
    }

    public void setHasPart(Set<String> hasPart) {
        this.hasPart = hasPart;
    }

    public void setHasEffect(Set<String> hasEffect) {
        this.hasEffect = hasEffect;
    }

    public void setHasCause(Set<String> hasCause) {
        this.hasCause = hasCause;
    }

    public void setHasImmediateCause(Set<String> hasImmediateCause) {
        this.hasImmediateCause = hasImmediateCause;
    }

    public Set<String> getImmediateCauseOf() {
        return immediateCauseOf;
    }

    public void setImmediateCauseOf(Set<String> immediateCauseOf) {
        this.immediateCauseOf = immediateCauseOf;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSubEventType() {
        return subEventType;
    }

    public void setSubEventType(String subEventType) {
        this.subEventType = subEventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    private boolean isEmpty() {
        return (this.aliases == null || this.aliases.isEmpty()) && (this.partOf == null || this.partOf.isEmpty()) &&
                (this.hasPart == null || this.hasPart.isEmpty()) && (this.hasEffect == null || this.hasEffect.isEmpty()) &&
                (this.hasCause == null || this.hasCause.isEmpty()) &&
                (this.immediateCauseOf == null || this.immediateCauseOf.isEmpty()) &&
                (this.hasImmediateCause == null || this.hasImmediateCause.isEmpty());
    }

    public boolean isValid() {
        boolean retVal = false;
        if(!isEmpty()) {
            String title = this.wikipediaLangPageTitle;
            retVal = title != null && !(title.startsWith("Wikipedia:") || title.startsWith("Template:") || title.startsWith("Category:") ||
                    title.startsWith("Help:") || StringUtils.isNumericSpace(title) || title.length() == 1);
        }

        return retVal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WECEventWithRelMention that = (WECEventWithRelMention) o;
        return corefId == that.corefId &&
                Objects.equals(wikidataPageId, that.wikidataPageId) &&
                Objects.equals(wikipediaLangPageTitle, that.wikipediaLangPageTitle) &&
                Objects.equals(elasticPageId, that.elasticPageId) &&
                Objects.equals(eventType, that.eventType) &&
                Objects.equals(subEventType, that.subEventType) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(partOf, that.partOf) &&
                Objects.equals(hasPart, that.hasPart) &&
                Objects.equals(hasEffect, that.hasEffect) &&
                Objects.equals(hasCause, that.hasCause) &&
                Objects.equals(hasImmediateCause, that.hasImmediateCause) &&
                Objects.equals(immediateCauseOf, that.immediateCauseOf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corefId, wikidataPageId, wikipediaLangPageTitle, elasticPageId, eventType, subEventType, aliases, partOf, hasPart, hasEffect, hasCause, hasImmediateCause, immediateCauseOf);
    }
}
