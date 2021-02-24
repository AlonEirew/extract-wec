package scripts.wec.resultsets;

import data.WECMention;
import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ValidationMention implements ISQLObject<ValidationMention> {
    public enum SPLIT {NA, TEST, VALIDATION, TRAIN}
    private static final WECMention mention = new WECMention();

    private MentionResultSet mentionResultSet;
    private SPLIT split = SPLIT.NA;
    private String tableName = "Validation";


    public ValidationMention() {
    }

    public ValidationMention(String tableName) {
        if (tableName != null && !tableName.isEmpty()) {
            this.tableName = tableName;
        }
    }

    public ValidationMention(MentionResultSet mentionResultSet, SPLIT split, String tableName) {
        this.mentionResultSet = mentionResultSet;
        this.split = split;
        this.tableName = tableName;
    }

    @Override
    public String getColumnNames() {
        return "split," + mention.getColumnNames();
    }

    @Override
    public String getColumnNamesAndValues() {
        return "split VARCHAR(10)," + mention.getColumnNamesAndValues();
    }

    @Override
    public String getValues() {
        return this.split.name() + "," +
                this.mentionResultSet.getMentionId() + "," +
                this.mentionResultSet.getCorefId() + "," +
                "'" + mentionResultSet.getMentionString() + "'" + "," +
                mentionResultSet.getTokenStart() + "," +
                mentionResultSet.getTokenEnd() + "," +
                "'" + mentionResultSet.getExtractedFromPage() + "'" +  "," +
                "'" + mentionResultSet.getContext() + "'" +
                "'" + String.join(", ", mentionResultSet.getPos()) + "'";
    }

    @Override
    public String getTableName() {
        return this.tableName;
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement statement) throws SQLException {
        statement.setString(1, this.split.name());
        statement.setLong(2, this.mentionResultSet.getMentionId());
        statement.setInt(3, this.mentionResultSet.getCorefId());
        statement.setString(4, this.mentionResultSet.getMentionString());
        statement.setInt(5, this.mentionResultSet.getTokenStart());
        statement.setInt(6, this.mentionResultSet.getTokenEnd());
        statement.setString(7, this.mentionResultSet.getExtractedFromPage());
        statement.setString(8, this.mentionResultSet.getContext());
        statement.setString(9, String.join(", ", this.mentionResultSet.getPos()));
    }

    @Override
    public String getPrepareInsertStatementQuery(String tableName) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(tableName).append(" ")
                .append("(").append(getColumnNames()).append(")").append(" ")
                .append("VALUES").append(" ")
                .append("(?,?,?,?,?,?,?,?,?)")
                .append(";");

        return query.toString();
    }

    @Override
    public ValidationMention resultSetToObject(ResultSet rs) throws SQLException {
        return null;
    }
}
