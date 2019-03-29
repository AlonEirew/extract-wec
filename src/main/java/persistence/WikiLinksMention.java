package persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiLinksMention implements ISQLObject {
    private static final String TABLE_MENTIONS = "Mentions";

    private static volatile AtomicInteger runningId = new AtomicInteger();

    private final long mentionId = runningId.incrementAndGet();
    private String mentionText = "";
    private int tokenStart = -1;
    private int tokenEnd = -1;
    private WikiLinksCoref coreChain;
    private String extractedFromPage = "";
    private List<String> mentinoTokens = new ArrayList<>();
    private List<String> context;

    public WikiLinksMention() {
    }

    public WikiLinksMention(String extractedFromPage) {
        this.extractedFromPage = extractedFromPage;
    }

    public WikiLinksMention(WikiLinksCoref coref, String mentionText,
                            int tokenStart, int tokenEnd, String extractedFromPage, List<String> context) {
        this.coreChain = coref;
        this.mentionText = mentionText;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.extractedFromPage = extractedFromPage;
        this.context = context;
    }

    public String getMentionText() {
        return mentionText;
    }

    public void setMentionText(String mentionText) {
        this.mentionText = mentionText;
    }

    public int getTokenStart() {
        return tokenStart;
    }

    public void setTokenStart(int tokenStart) {
        this.tokenStart = tokenStart;
    }

    public int getTokenEnd() {
        return tokenEnd;
    }

    public void setTokenEnd(int tokenEnd) {
        this.tokenEnd = tokenEnd;
    }

    public WikiLinksCoref getCorefChain() {
        return this.coreChain;
    }

    public void setCorefChain(String corefChainValue) {
        this.coreChain = WikiLinksCoref.getCorefChain(corefChainValue);
    }

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public String getExtractedFromPage() {
        return extractedFromPage;
    }

    public void setExtractedFromPage(String extractedFromPage) {
        this.extractedFromPage = extractedFromPage;
    }

    public long getMentionId() {
        return mentionId;
    }

    public List<String> getMentinoTokens() {
        return mentinoTokens;
    }

    public void addMentionToken(String token) {
        if(token != null) {
            mentinoTokens.add(token);
        }
    }

    public boolean isValid() {
        if(this.coreChain.getCorefValue().isEmpty() ||
            this.mentionText.length() <= 1 ||
                this.mentionText.toLowerCase().startsWith("category:") ||
                this.coreChain.getCorefValue().toLowerCase().startsWith("file:") ||
                this.coreChain.getCorefValue().toLowerCase().startsWith("wikipedia:") ||
                this.coreChain.getCorefValue().toLowerCase().startsWith("category:") ||
                this.tokenStart == -1 || this.tokenEnd == -1 ||
                this.mentinoTokens.size() == 0 ||
                ((this.tokenEnd - this.tokenStart + 1) != this.mentinoTokens.size()) ||
                this.context.contains("#") ||
                this.context.contains("jpg") ||
                this.context.contains("{") ||
                this.context.contains("}")) {
            return false;
        }

        return true;
    }

    private String getContextAsSQLBlob() {
        return String.join(" ", this.context);
    }

    @Override
    public String getColumnNames() {
        return "mentionId, coreChainId, mentionText, tokenStart, tokenEnd, extractedFromPage, context";
    }

    @Override
    public String getColumnNamesAndValues() {
        return "" +
                "mentionId INT," +
                "coreChainId INT, " +
                "mentionText VARCHAR(500), " +
                "tokenStart INT, " +
                "tokenEnd INT, " +
                "extractedFromPage VARCHAR(500), " +
                "context TEXT," +
                "PRIMARY KEY (mentionId)";
    }

    @Override
    public String getValues() {
        return mentionId + "," +
                this.coreChain.getCorefId() + "," +
                "'" + mentionText + "'" + "," +
                tokenStart + "," +
                tokenEnd + "," +
                "'" + extractedFromPage + "'" +  "," +
                "'" + getContextAsSQLBlob() + "'";
    }

    @Override
    public String getTableName() {
        return TABLE_MENTIONS;
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setLong(1, this.mentionId);
        preparedStatement.setInt(2, this.coreChain.getCorefId());
        preparedStatement.setString(3, this.mentionText);
        preparedStatement.setInt(4, this.tokenStart);
        preparedStatement.setInt(5, this.tokenEnd);
        preparedStatement.setString(6, this.extractedFromPage);
        preparedStatement.setString(7, this.getContextAsSQLBlob());
    }

    @Override
    public String getPrepareInsertStatementQuery(String tableName) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(tableName).append(" ")
                .append("(").append(getColumnNames()).append(")").append(" ")
                .append("VALUES").append(" ")
                .append("(?,?,?,?,?,?,?)")
                .append(";");

        return query.toString();
    }
}
