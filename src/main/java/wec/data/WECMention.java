package wec.data;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import wec.utils.StanfordNlpApi;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "MENTIONS")
public class WECMention extends BaseMention {

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name="coref_id", nullable=false)
    private WECCoref corefChain;
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

    public WECMention(WECCoref coref, String mentionText,
                      int tokenStart, int tokenEnd, String extractedFromPage, WECContext context) {
        super(tokenStart, tokenEnd, extractedFromPage, context);
        this.corefChain = coref;
        this.corefChain.incMentionsCount();
        this.mentionText = mentionText;
    }

    public String getMentionText() {
        return mentionText;
    }

    public void setMentionText(String mentionText) {
        this.mentionText = mentionText;
    }

    public WECCoref getCorefChain() {
        return this.corefChain;
    }

    public void setCorefChain(WECCoref corefChain) {
        this.corefChain = corefChain;
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
        if(coreDocument != null) {
            CoreLabel coreLabel = coreDocument.sentences().get(0).dependencyParse().getFirstRoot().backingLabel();
            this.mentionNer = coreLabel.ner();
            this.mentionLemma = coreLabel.lemma();
            this.mentionPos = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            this.mentionHead = coreLabel.value();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WECMention mention = (WECMention) o;
        return corefChain.equals(mention.corefChain) && mentionText.equals(mention.mentionText) &&
                Objects.equals(mentionNer, mention.mentionNer) && Objects.equals(mentionLemma, mention.mentionLemma) &&
                Objects.equals(mentionPos, mention.mentionPos) && Objects.equals(mentionHead, mention.mentionHead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), corefChain, mentionText, mentionNer, mentionLemma, mentionPos, mentionHead);
    }
}
