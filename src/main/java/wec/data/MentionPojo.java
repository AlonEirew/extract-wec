package wec.data;

import java.util.List;
import java.util.Objects;

public class MentionPojo {
    private int coref_chain;
    private String coref_link;
    private String doc_id;
    private List<String> mention_context;
    private String mention_head;
    private String mention_head_lemma;
    private String mention_head_pos;
    private long mention_id;
    private int mention_index;
    private String mention_ner;
    private String mention_type;
    private List<Integer> tokens_number;
    private String tokens_str;

    public int getCoref_chain() {
        return coref_chain;
    }

    public String getCoref_link() {
        return coref_link;
    }

    public String getDoc_id() {
        return doc_id;
    }

    public String getTokens_str() {
        return tokens_str;
    }

    public String getMention_type() {
        return mention_type;
    }

    public long getMention_id() {
        return mention_id;
    }

    public String getMention_head() {
        return mention_head;
    }

    public String getMention_head_lemma() {
        return mention_head_lemma;
    }

    public String getMention_head_pos() {
        return mention_head_pos;
    }

    public String getMention_ner() {
        return mention_ner;
    }

    public List<Integer> getTokens_number() {
        return tokens_number;
    }

    public List<String> getMention_context() {
        return mention_context;
    }

    public void setCoref_chain(int coref_chain) {
        this.coref_chain = coref_chain;
    }

    public void setCoref_link(String coref_link) {
        this.coref_link = coref_link;
    }

    public void setDoc_id(String doc_id) {
        this.doc_id = doc_id;
    }

    public void setTokens_str(String tokens_str) {
        this.tokens_str = tokens_str;
    }

    public void setMention_type(String mention_type) {
        this.mention_type = mention_type;
    }

    public void setMention_id(long mention_id) {
        this.mention_id = mention_id;
    }

    public void setMention_head(String mention_head) {
        this.mention_head = mention_head;
    }

    public void setMention_head_lemma(String mention_head_lemma) {
        this.mention_head_lemma = mention_head_lemma;
    }

    public void setMention_head_pos(String mention_head_pos) {
        this.mention_head_pos = mention_head_pos;
    }

    public void setMention_ner(String mention_ner) {
        this.mention_ner = mention_ner;
    }

    public void setTokens_number(List<Integer> tokens_number) {
        this.tokens_number = tokens_number;
    }

    public void setMention_context(List<String> mention_context) {
        this.mention_context = mention_context;
    }

    public int getMention_index() {
        return mention_index;
    }

    public void setMention_index(int mention_index) {
        this.mention_index = mention_index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MentionPojo that = (MentionPojo) o;
        return coref_chain == that.coref_chain && mention_id == that.mention_id && mention_index == that.mention_index && coref_link.equals(that.coref_link) && doc_id.equals(that.doc_id) && mention_context.equals(that.mention_context) && Objects.equals(mention_head, that.mention_head) && Objects.equals(mention_head_lemma, that.mention_head_lemma) && Objects.equals(mention_head_pos, that.mention_head_pos) && Objects.equals(mention_type, that.mention_type) && Objects.equals(mention_ner, that.mention_ner) && tokens_str.equals(that.tokens_str) && tokens_number.equals(that.tokens_number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coref_chain, coref_link, doc_id, mention_context, mention_head, mention_head_lemma, mention_head_pos, mention_id, mention_index, mention_type, mention_ner, tokens_str, tokens_number);
    }
}
