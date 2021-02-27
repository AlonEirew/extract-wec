package data;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import persistence.ISQLObject;
import utils.StanfordNlpApi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class WECMention extends BaseMention implements ISQLObject<WECMention> {
    public static final String TABLE_MENTIONS = "Mentions";

    private static final AtomicInteger runningId = new AtomicInteger();

    private WECCoref coreChain;
    private String mentionText;
    private final List<String> mentionTokens = new ArrayList<>();
    private String mentionNer;
    private String mentionLemma;
    private String mentionPos;
    private String mentionHead;

    public WECMention() {
        super(runningId.incrementAndGet());
    }

    public WECMention(WECMention mention) {
        super(mention);
        this.coreChain = mention.coreChain;
        this.mentionText = mention.mentionText;
        if(this.coreChain != null) {
            this.coreChain.incMentionsCount();
        }
    }

    public WECMention(WECCoref coref, String mentionText,
                      int tokenStart, int tokenEnd, String extractedFromPage, WECContext context) {
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
        return "mentionId, coreChainId, mentionText, tokenStart, tokenEnd, extractedFromPage, contextId, PartOfSpeech";
    }

    @Override
    public String getColumnNamesAndValues() {
        return "mentionId INT," +
                "coreChainId INT," +
                "mentionText VARCHAR(500)," +
                "tokenStart INT," +
                "tokenEnd INT," +
                "extractedFromPage VARCHAR(500)," +
                "contextId INT," +
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
                "'" + this.getContext().getContextId() + "'";
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
        preparedStatement.setInt(7, this.getContext().getContextId());
    }

    @Override
    public String getPrepareInsertStatementQuery() {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(TABLE_MENTIONS).append(" ")
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
        final int contextId = rs.getInt("contextId");

        WECMention mention = new WECMention();
        mention.setCorefId(corefId);
        mention.setMentionText(mentionText);
        mention.setTokenStart(tokenStart);
        mention.setTokenEnd(tokenEnd);
        mention.setExtractedFromPage(extractedFromPage);
        mention.setContext(new WECContext(contextId));

        return mention;
    }

    public String getMentionNer() {
        return mentionNer;
    }

    public String getMentionLemma() {
        return mentionLemma;
    }

    public String getMentionPos() {
        return mentionPos;
    }

    public String getMentionHead() {
        return mentionHead;
    }

    public void fillMentionNerPosLemma() {
        CoreDocument coreDocument = StanfordNlpApi.withPosAnnotate(this.mentionText);
        CoreLabel coreLabel = coreDocument.sentences().get(0).dependencyParse().getFirstRoot().backingLabel();
        this.mentionNer = coreLabel.ner();
        this.mentionLemma = coreLabel.lemma();
        this.mentionPos = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        this.mentionHead = coreLabel.value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WECMention mention = (WECMention) o;
        return Objects.equals(coreChain, mention.coreChain) && Objects.equals(mentionText, mention.mentionText) && Objects.equals(mentionTokens, mention.mentionTokens) && Objects.equals(mentionNer, mention.mentionNer) && Objects.equals(mentionLemma, mention.mentionLemma) && Objects.equals(mentionPos, mention.mentionPos) && Objects.equals(mentionHead, mention.mentionHead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coreChain, mentionText, mentionTokens, mentionNer, mentionLemma, mentionPos, mentionHead);
    }
}
