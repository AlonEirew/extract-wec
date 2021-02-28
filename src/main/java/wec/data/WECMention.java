package wec.data;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import wec.utils.StanfordNlpApi;

import javax.persistence.*;
import java.util.Objects;

@Entity
public class WECMention extends BaseMention {

    @ManyToOne(fetch = FetchType.LAZY, cascade= CascadeType.ALL)
    private WECCoref coreChain;
    private String mentionText;

    @Transient
    private String mentionNer;
    @Transient
    private String mentionLemma;
    @Transient
    private String mentionPos;
    @Transient
    private String mentionHead;

    public WECMention() {
    }

    public WECMention(WECMention mention) {
        super(mention);
        this.coreChain = mention.coreChain;
        this.mentionText = mention.mentionText;
        this.mentionHead = mention.mentionHead;
        this.mentionPos = mention.mentionPos;
        this.mentionLemma = mention.mentionLemma;
        this.mentionNer = mention.mentionNer;

        if(this.coreChain != null) {
            this.coreChain.incMentionsCount();
        }
    }

    public WECMention(WECCoref coref, String mentionText,
                      int tokenStart, int tokenEnd, String extractedFromPage, WECContext context) {
        super(tokenStart, tokenEnd, extractedFromPage, context);

        this.coreChain = coref;
        this.mentionText = mentionText;
        this.coreChain.incMentionsCount();
    }

    public WECCoref getCoreChain() {
        return coreChain;
    }

    public void setCoreChain(WECCoref coreChain) {
        this.coreChain = coreChain;
    }

    public String getMentionText() {
        return mentionText;
    }

    public void setMentionText(String mentionText) {
        this.mentionText = mentionText;
    }

    public WECCoref getCorefChain() {
        return this.coreChain;
    }

    public void setCorefChain(String corefChainValue) {
        this.coreChain = WECCoref.getAndSetIfNotExist(corefChainValue);
    }

    public void setCorefChain(WECCoref corefChainValue) {
        this.coreChain = corefChainValue;
    }

    public String getMentionNer() {
        return mentionNer;
    }

    public String getMentionLemma() {
        return mentionLemma;
    }

    public String getMentionPos() {
        return mentionPos;
    }

    public String getMentionHead() {
        return mentionHead;
    }

    public void fillMentionNerPosLemma() {
        CoreDocument coreDocument = StanfordNlpApi.withPosAnnotate(this.mentionText);
        CoreLabel coreLabel = coreDocument.sentences().get(0).dependencyParse().getFirstRoot().backingLabel();
        this.mentionNer = coreLabel.ner();
        this.mentionLemma = coreLabel.lemma();
        this.mentionPos = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        this.mentionHead = coreLabel.value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WECMention mention = (WECMention) o;
        return Objects.equals(coreChain, mention.coreChain) && Objects.equals(mentionText, mention.mentionText) && Objects.equals(mentionNer, mention.mentionNer) && Objects.equals(mentionLemma, mention.mentionLemma) && Objects.equals(mentionPos, mention.mentionPos) && Objects.equals(mentionHead, mention.mentionHead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coreChain, mentionText, mentionNer, mentionLemma, mentionPos, mentionHead);
    }
}
