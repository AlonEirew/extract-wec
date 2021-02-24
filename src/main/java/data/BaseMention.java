package data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import java.util.Objects;

public class BaseMention {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final long mentionId;
    private int corefId;
    private int tokenStart;
    private int tokenEnd;
    private String extractedFromPage;
    private transient JsonArray context;

    public BaseMention(long mentionId) {
        this.mentionId = mentionId;
    }

    public BaseMention(BaseMention mention) {
        this(mention.mentionId, mention.corefId,
                mention.tokenStart, mention.tokenEnd,
                mention.extractedFromPage, mention.context);
    }

    public BaseMention(long mentionId, int corefId, int tokenStart, int tokenEnd, String extractedFromPage, JsonArray context) {
        this.mentionId = mentionId;
        this.corefId = corefId;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.extractedFromPage = extractedFromPage;
        this.context = context;
    }

    public long getMentionId() {
        return mentionId;
    }

    public int getCorefId() {
        return corefId;
    }

    public void setCorefId(int corefId) {
        this.corefId = corefId;
    }

    public int getTokenStart() {
        return tokenStart;
    }

    public int getTokenEnd() {
        return tokenEnd;
    }

    public String getExtractedFromPage() {
        return extractedFromPage;
    }

    public JsonArray getContext() {
        return context;
    }

    public void setTokenStart(int tokenStart) {
        this.tokenStart = tokenStart;
    }

    public void setTokenEnd(int tokenEnd) {
        this.tokenEnd = tokenEnd;
    }

    public void setExtractedFromPage(String extractedFromPage) {
        this.extractedFromPage = extractedFromPage;
    }

    public void setContext(JsonArray context) {
        this.context = context;
    }

    public String getContextAsJsonString() {
        return GSON.toJson(this.context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseMention that = (BaseMention) o;
        return mentionId == that.mentionId &&
                corefId == that.corefId &&
                tokenStart == that.tokenStart &&
                tokenEnd == that.tokenEnd &&
                Objects.equals(extractedFromPage, that.extractedFromPage) &&
                Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mentionId, corefId, tokenStart, tokenEnd, extractedFromPage, context);
    }
}