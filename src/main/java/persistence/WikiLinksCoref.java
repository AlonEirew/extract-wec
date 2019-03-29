package persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiLinksCoref implements ISQLObject {
    private static final String TABLE_COREF = "CorefChains";

    private static volatile AtomicInteger runningId = new AtomicInteger();
    private static final ConcurrentHashMap<String, WikiLinksCoref> globalCorefIds = new ConcurrentHashMap<>();

    private int corefId = runningId.incrementAndGet();;
    private String corefValue;
    private AtomicInteger mentionsCount = new AtomicInteger(0);

    private WikiLinksCoref(String corefValue) {
        this.corefValue = corefValue;
    }

    public static synchronized WikiLinksCoref getCorefChain(String corefValue) {
        if(!globalCorefIds.containsKey(corefValue)) {
            globalCorefIds.put(corefValue, new WikiLinksCoref(corefValue));
        }

        return globalCorefIds.get(corefValue);
    }

    public static void removeKey(String keyToRemove) {
        globalCorefIds.remove(keyToRemove);
    }

    public static Map<String, WikiLinksCoref> getGlobalCorefMap() {
        return globalCorefIds;
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

    public int addAndGetMentionCount(int delta) {
        return this.mentionsCount.addAndGet(delta);
    }

    public int getMentionsCount() {
        return this.mentionsCount.get();
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
    public String getTableName() {
        return TABLE_COREF;
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
