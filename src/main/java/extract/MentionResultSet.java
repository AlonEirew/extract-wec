package extract;

import java.util.Objects;

public class MentionResultSet {
    private int corefId;
    private String mentionString;
    private String extractedFromPage;
    private int tokenStart;
    private int tokenEnd;
    private String context;

    public MentionResultSet(int corefId, String mentionString, String extractedFromPage,
                            int tokenStart, int tokenEnd, String context) {
        this.corefId = corefId;
        this.mentionString = mentionString;
        this.extractedFromPage = extractedFromPage;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.context = context;

    }

    public String getMentionString() {
        return mentionString;
    }

    public String getExtractedFromPage() {
        return extractedFromPage;
    }

    public String getContext() {
        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MentionResultSet that = (MentionResultSet) o;
        return corefId == that.corefId &&
                tokenStart == that.tokenStart &&
                tokenEnd == that.tokenEnd &&
                Objects.equals(mentionString, that.mentionString) &&
                Objects.equals(extractedFromPage, that.extractedFromPage) &&
                Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corefId, mentionString, extractedFromPage, tokenStart, tokenEnd, context);
    }
}
