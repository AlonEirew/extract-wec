package data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class WECMention implements ISQLObject<WECMention> {
    private static final String TABLE_MENTIONS = "Mentions";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static volatile AtomicInteger runningId = new AtomicInteger();

    private final long mentionId = runningId.incrementAndGet();
    private String mentionText = "";
    private int tokenStart = -1;
    private int tokenEnd = -1;
    private WECCoref coreChain;
    private String extractedFromPage = "";
    private final List<String> mentionTokens = new ArrayList<>();
    private final List<String> mentionTokensPos = new ArrayList<>();
    private List<Map.Entry<String, Integer>> context;

    public WECMention() {
    }

    public WECMention(WECCoref coref, String mentionText,
                      int tokenStart, int tokenEnd, String extractedFromPage, List<Map.Entry<String, Integer>> context) {
        this.coreChain = coref;
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
    }

    public void setCorefChain(WECCoref corefChainValue) {
        this.coreChain = corefChainValue;
    }

    public List<Map.Entry<String, Integer>> getContext() {
        return context;
    }

    public void setContext(List<Map.Entry<String, Integer>> context) {
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
        JsonArray rootArray = new JsonArray();
        for (Map.Entry<String, Integer> entry : this.context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(entry.getKey(), entry.getValue());
            rootArray.add(jsonObject);
        }

        return GSON.toJson(rootArray);
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
        return null;
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
