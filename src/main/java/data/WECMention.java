package data;

import com.google.common.collect.Lists;
import com.google.gson.*;
import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WECMention implements ISQLObject<WECMention> {
    private static final String TABLE_MENTIONS = "Mentions";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static volatile AtomicInteger runningId = new AtomicInteger();

    private final long mentionId = runningId.incrementAndGet();
    private int corefId = -1;
    private String mentionText = "";
    private int tokenStart = -1;
    private int tokenEnd = -1;
    private WECCoref coreChain;
    private String extractedFromPage = "";
    private final List<String> mentionTokens = new ArrayList<>();
    private final List<String> mentionTokensPos = new ArrayList<>();
    private JsonArray context;

    public WECMention() {
    }

    public WECMention(WECCoref coref, String mentionText,
                      int tokenStart, int tokenEnd, String extractedFromPage, JsonArray context) {
        this.coreChain = coref;
        this.corefId = coref.getCorefId();
        this.mentionText = mentionText;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.extractedFromPage = extractedFromPage;
        this.context = context;

        this.coreChain.incMentionsCount();
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

    public WECCoref getCorefChain() {
        return this.coreChain;
    }

    public void setCorefChain(String corefChainValue) {
        this.coreChain = WECCoref.getAndSetIfNotExist(corefChainValue);
        this.corefId = coreChain.getCorefId();
    }

    public void setCorefChain(WECCoref corefChainValue) {
        this.coreChain = corefChainValue;
    }

    public JsonArray getContext() {
        return context;
    }

    public void setContext(JsonArray context) {
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

    public int getCorefId() {
        return corefId;
    }

    public void setCorefId(int corefId) {
        this.corefId = corefId;
    }

    @Override
    public String getColumnNames() {
        return "mentionId, coreChainId, mentionText, tokenStart, tokenEnd, extractedFromPage, context, PartOfSpeech";
    }

    @Override
    public String getColumnNamesAndValues() {
        return "mentionId INT," +
                "coreChainId INT," +
                "mentionText VARCHAR(500)," +
                "tokenStart INT," +
                "tokenEnd INT," +
                "extractedFromPage VARCHAR(500)," +
                "context TEXT," +
                "PartOfSpeech TEXT," +
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
                "'" + getContextAsSQLBlob() + "'" +
                "'" + String.join(", ", this.mentionTokensPos) + "'";
    }

    private String getContextAsSQLBlob() {
        return GSON.toJson(this.context);
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
        preparedStatement.setString(7, getContextAsSQLBlob());
        preparedStatement.setString(8, String.join(", ", this.mentionTokensPos));
    }

    @Override
    public String getPrepareInsertStatementQuery(String tableName) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(tableName).append(" ")
                .append("(").append(getColumnNames()).append(")").append(" ")
                .append("VALUES").append(" ")
                .append("(?,?,?,?,?,?,?,?)")
                .append(";");

        return query.toString();
    }

    @Override
    public WECMention resultSetToObject(ResultSet rs) throws SQLException {
        final int corefId = rs.getInt("coreChainId");
        final String mentionText = rs.getString("mentionText");
        final int tokenStart = rs.getInt("tokenStart");
        final int tokenEnd = rs.getInt("tokenEnd");
        final String extractedFromPage = rs.getString("extractedFromPage");
        final String context = rs.getString("context");

        WECMention mention = new WECMention();
        mention.setCorefId(corefId);
        mention.setMentionText(mentionText);
        mention.setTokenStart(tokenStart);
        mention.setTokenEnd(tokenEnd);
        mention.setExtractedFromPage(extractedFromPage);

        JsonArray asJsonObject = new JsonParser().parse(context).getAsJsonArray();
        mention.setContext(asJsonObject);

        return mention;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WECMention mention = (WECMention) o;
        return mentionId == mention.mentionId &&
                tokenStart == mention.tokenStart &&
                tokenEnd == mention.tokenEnd &&
                Objects.equals(mentionText, mention.mentionText) &&
                Objects.equals(coreChain, mention.coreChain) &&
                Objects.equals(extractedFromPage, mention.extractedFromPage) &&
                Objects.equals(mentionTokens, mention.mentionTokens) &&
                Objects.equals(mentionTokensPos, mention.mentionTokensPos) &&
                Objects.equals(context, mention.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mentionId, mentionText, tokenStart, tokenEnd, coreChain, extractedFromPage, mentionTokens, mentionTokensPos, context);
    }
}
