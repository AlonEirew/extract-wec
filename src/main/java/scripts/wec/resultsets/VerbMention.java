package scripts.wec.resultsets;

import data.WECMention;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class VerbMention extends WECMention {

    private static final String tableName = "VerbsMentions";
    private MentionResultSet mentionResultSet;

    public VerbMention() {}

    public VerbMention(MentionResultSet mentionResultSet) {
        this.mentionResultSet = mentionResultSet;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public String getValues() {
        return mentionResultSet.getMentionId() + "," +
                mentionResultSet.getCorefId() + "," +
                "'" + mentionResultSet.getMentionString() + "'" + "," +
                mentionResultSet.getTokenStart() + "," +
                mentionResultSet.getTokenEnd() + "," +
                "'" + mentionResultSet.getExtractedFromPage() + "'" +  "," +
                "'" + mentionResultSet.getContext() + "'" +
                "'" + mentionResultSet.getPos() + "'";
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setLong(1, this.mentionResultSet.getMentionId());
        preparedStatement.setInt(2, mentionResultSet.getCorefId());
        preparedStatement.setString(3, mentionResultSet.getMentionString());
        preparedStatement.setInt(4, mentionResultSet.getTokenStart());
        preparedStatement.setInt(5, mentionResultSet.getTokenEnd());
        preparedStatement.setString(6, mentionResultSet.getExtractedFromPage());
        preparedStatement.setString(7, mentionResultSet.getContext());
        preparedStatement.setString(8, mentionResultSet.getPos());
    }
}
