package experimentscripts;

import data.WikiLinksMention;
import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ValidationMention implements ISQLObject {

    private static WikiLinksMention mention = new WikiLinksMention();
    private MentionResultSet mentionResultSet;

    public ValidationMention() {
    }

    public ValidationMention(MentionResultSet mentionResultSet) {
        this.mentionResultSet = mentionResultSet;
    }

    @Override
    public String getColumnNames() {
        return mention.getColumnNames();
    }

    @Override
    public String getColumnNamesAndValues() {
        return mention.getColumnNamesAndValues();
    }

    @Override
    public String getValues() {
        return this.mentionResultSet.getMentionId() + "," +
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
        return "Validation";
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement statement) throws SQLException {
        statement.setLong(1, this.mentionResultSet.getMentionId());
        statement.setInt(2, this.mentionResultSet.getCorefId());
        statement.setString(3, this.mentionResultSet.getMentionString());
        statement.setInt(4, this.mentionResultSet.getTokenStart());
        statement.setInt(5, this.mentionResultSet.getTokenEnd());
        statement.setString(6, this.mentionResultSet.getExtractedFromPage());
        statement.setString(7, this.mentionResultSet.getContext());
        statement.setString(8, String.join(", ", this.mentionResultSet.getPos()));
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
}
