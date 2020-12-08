package data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class WECMention extends BaseMention implements ISQLObject<WECMention> {
    private static final String TABLE_MENTIONS = "Mentions";

    private static final AtomicInteger runningId = new AtomicInteger();

    private WECCoref coreChain;
    private String mentionText;
    private final List<String> mentionTokens = new ArrayList<>();
    private final List<String> mentionTokensPos = new ArrayList<>();

    public WECMention() {
        super(runningId.incrementAndGet());
    }

    public WECMention(WECCoref coref, String mentionText,
                      int tokenStart, int tokenEnd, String extractedFromPage, JsonArray context) {
        super(runningId.incrementAndGet(), coref.getCorefId(), tokenStart, tokenEnd, extractedFromPage, context);
        this.coreChain = coref;
        this.mentionText = mentionText;
        this.coreChain.incMentionsCount();
    }

    public String getMentionText() {
        return mentionText;
    }

    public void setMentionText(String mentionText) {
        this.mentionText = mentionText;
    }

    public WECCoref getCorefChain() {
        return this.coreChain;
    }

    public void setCorefChain(String corefChainValue) {
        this.coreChain = WECCoref.getAndSetIfNotExist(corefChainValue);
        this.setCorefId(coreChain.getCorefId());
    }

    public void setCorefChain(WECCoref corefChainValue) {
        this.coreChain = corefChainValue;
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
        return getMentionId() + "," +
                this.coreChain.getCorefId() + "," +
                "'" + mentionText + "'" + "," +
                getTokenStart() + "," +
                getTokenEnd() + "," +
                "'" + getExtractedFromPage() + "'" +  "," +
                "'" + getContextAsSQLBlob() + "'" +
                "'" + String.join(", ", this.mentionTokensPos) + "'";
    }

    @Override
    public String getTableName() {
        return TABLE_MENTIONS;
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setLong(1, this.getMentionId());
        preparedStatement.setInt(2, this.coreChain.getCorefId());
        preparedStatement.setString(3, this.mentionText);
        preparedStatement.setInt(4, this.getTokenStart());
        preparedStatement.setInt(5, this.getTokenEnd());
        preparedStatement.setString(6, this.getExtractedFromPage());
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
        if (!super.equals(o)) return false;
        WECMention that = (WECMention) o;
        return Objects.equals(coreChain, that.coreChain) &&
                Objects.equals(mentionText, that.mentionText) &&
                Objects.equals(mentionTokens, that.mentionTokens) &&
                Objects.equals(mentionTokensPos, that.mentionTokensPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coreChain, mentionText, mentionTokens, mentionTokensPos);
    }
}
