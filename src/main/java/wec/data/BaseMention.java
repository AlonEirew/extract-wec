package wec.data;

import javax.persistence.*;
import java.util.Objects;

@MappedSuperclass
public class BaseMention {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long mentionId;
    private int tokenStart;
    private int tokenEnd;
    private String extractedFromPage;

    @JoinColumn(name="context_id", nullable=false)
    private long contextId;

    protected BaseMention() {
    }

    public BaseMention(int tokenStart, int tokenEnd, String extractedFromPage) {
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.extractedFromPage = extractedFromPage;
    }

    public long getMentionId() {
        return mentionId;
    }

    public void setMentionId(long mentionId) {
        this.mentionId = mentionId;
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

    public long getContextId() {
        return this.contextId;
    }

    public void setContextId(long context) {
        this.contextId = context;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseMention that = (BaseMention) o;
        return mentionId == that.mentionId && tokenStart == that.tokenStart && tokenEnd == that.tokenEnd && contextId == that.contextId && extractedFromPage.equals(that.extractedFromPage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mentionId, tokenStart, tokenEnd, extractedFromPage, contextId);
    }
}
