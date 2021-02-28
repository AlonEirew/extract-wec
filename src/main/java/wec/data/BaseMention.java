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

    @ManyToOne(fetch = FetchType.LAZY, cascade=CascadeType.ALL)
    @JoinColumn(name="context_id", nullable=false)
    private WECContext context;

    protected BaseMention() {
    }

    public BaseMention(BaseMention mention) {
        this(mention.tokenStart, mention.tokenEnd,
                mention.extractedFromPage, mention.context);

        this.mentionId = mention.mentionId;
    }

    public BaseMention(int tokenStart, int tokenEnd, String extractedFromPage, WECContext context) {
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.extractedFromPage = extractedFromPage;
        this.context = context;
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

    public WECContext getContext() {
        return this.context;
    }

    public void setContext(WECContext context) {
        this.context = context;
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
        return mentionId == that.mentionId && tokenStart == that.tokenStart && tokenEnd == that.tokenEnd && Objects.equals(extractedFromPage, that.extractedFromPage) && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mentionId, tokenStart, tokenEnd, extractedFromPage, context);
    }
}
