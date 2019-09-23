package experimentscripts;

import java.util.Objects;

public class MentionResultSet {
    private int mentionId;
    private int corefId;
    private String mentionString;
    private String extractedFromPage;
    private int tokenStart;
    private int tokenEnd;
    private int context;
    private boolean markedForDelete = false;
    private String pos;

    public MentionResultSet(int corefId, String mentionString, String extractedFromPage,
                            int tokenStart, int tokenEnd, int context, String pos) {
        this.corefId = corefId;
        this.mentionString = mentionString;
        this.extractedFromPage = extractedFromPage;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.context = context;
        this.pos = pos;
    }

    public MentionResultSet(MentionResultSet mention) {
        this.corefId = mention.corefId;
        this.mentionString = mention.mentionString;
        this.extractedFromPage = mention.extractedFromPage;
        this.tokenStart = mention.tokenStart;
        this.tokenEnd = mention.tokenEnd;
        this.context = mention.context;
        this.pos = mention.pos;
    }

    public MentionResultSet(int corefId, int mentionId, String mentionText, String extractedFromPage, int tokenStart, int tokenEnd, int context, String partOfSpeech) {
        this(corefId, mentionText, extractedFromPage, tokenStart, tokenEnd, context, partOfSpeech);
        this.mentionId = mentionId;
    }

    public int getMentionId() {
        return mentionId;
    }

    public String getMentionString() {
        return mentionString;
    }

    public String getExtractedFromPage() {
        return extractedFromPage;
    }

    public int getContext() {
        return context;
    }

    public int getCorefId() {
        return corefId;
    }

    public int getTokenStart() {
        return tokenStart;
    }

    public int getTokenEnd() {
        return tokenEnd;
    }

    public boolean isMarkedForDelete() {
        return markedForDelete;
    }

    public void setMarkedForDelete(boolean markedForDelete) {
        this.markedForDelete = markedForDelete;
    }

    public String getPos() {
        return pos;
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
