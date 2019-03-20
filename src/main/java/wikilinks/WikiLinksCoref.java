package wikilinks;

import persistence.SQLObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiLinksCoref implements SQLObject {
    private static volatile AtomicInteger runningId = new AtomicInteger();

    private int corefId = runningId.incrementAndGet();
    private String corefValue;
    private AtomicInteger mentionsCount = new AtomicInteger(1);

    public WikiLinksCoref() {
    }

    public WikiLinksCoref(String corefValue) {
        this.corefValue = corefValue;
    }

    public int getCorefId() {
        return corefId;
    }

    public String getCorefValue() {
        return corefValue;
    }

    public void incMentionsCount() {
        this.mentionsCount.incrementAndGet();
    }

    @Override
    public String getColumnNames() {
        return "corefId, corefValue, mentionsCount";
    }

    @Override
    public String getColumnNamesAndValues() {
        return "" +
                "corefId BIGINT," +
                "corefValue VARCHAR(500), " +
                "mentionsCount INT," +
                "PRIMARY KEY (corefId)";
    }

    @Override
    public String getValues() {
        return corefId + "," +
                "'" + corefValue + "'" +
                "," + mentionsCount.get();
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setInt(1, this.corefId);
        preparedStatement.setString(2, this.corefValue);
        preparedStatement.setInt(3, this.mentionsCount.get());
    }

    @Override
    public String getPrepareInsertStatementQuery(String tableName) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(tableName).append(" ")
                .append("(").append(getColumnNames()).append(")").append(" ")
                .append("VALUES").append(" ")
                .append("(?,?,?)")
                .append(";");

        return query.toString();
    }
}
