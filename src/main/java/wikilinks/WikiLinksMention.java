package wikilinks;

import persistence.SQLObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiLinksMention implements SQLObject {
    private static volatile AtomicInteger runningId = new AtomicInteger();

    private final long mentionId = runningId.incrementAndGet();
    private String mentionText = "";
    private int tokenStart = -1;
    private int tokenEnd = -1;
    private String corefChain = "";
    private int coreChainId = -1;
    private String extractedFromPage = "";
    private List<String> mentinoTokens = new ArrayList<>();
    private List<String> context;

    public WikiLinksMention() {
    }

    public WikiLinksMention(int coreChainId, String mentionText,
                            int tokenStart, int tokenEnd, String extractedFromPage, List<String> context) {
        this.coreChainId = coreChainId;
        this.mentionText = mentionText;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.extractedFromPage = extractedFromPage;
        this.context = context;
    }

    public WikiLinksMention(String extractedFromPage) {
        this.extractedFromPage = extractedFromPage;
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

    public String getCorefChain() {
        return corefChain;
    }

    public void setCorefChain(String corefChain) {
        this.corefChain = corefChain;
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

    public int getCoreChainId() {
        return coreChainId;
    }

    public void setCoreChainId(int coreChainId) {
        this.coreChainId = coreChainId;
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
        if(this.corefChain.isEmpty() ||
            this.mentionText.length() <= 1 ||
                this.mentionText.startsWith("Category:") ||
                this.corefChain.toLowerCase().startsWith("file:") ||
                this.corefChain.toLowerCase().startsWith("wikipedia:") ||
                this.tokenStart == -1 || this.tokenEnd == -1 ||
                this.mentinoTokens.size() == 0 ||
                ((this.tokenEnd - this.tokenStart + 1) != this.mentinoTokens.size()) ||
                this.context.get(0).contains("#") ||
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
                coreChainId + "," +
                "'" + mentionText + "'" + "," +
                tokenStart + "," +
                tokenEnd + "," +
                "'" + extractedFromPage + "'" +  "," +
                "'" + getContextAsSQLBlob() + "'";
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setLong(1, this.mentionId);
        preparedStatement.setInt(2, this.coreChainId);
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
