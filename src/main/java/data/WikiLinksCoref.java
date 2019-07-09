package data;

import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WikiLinksCoref implements ISQLObject {
    public static final String TABLE_COREF = "CorefChains";

    private static volatile AtomicInteger runningId = new AtomicInteger();
    private static final ConcurrentHashMap<String, WikiLinksCoref> globalCorefIds = new ConcurrentHashMap<>();

    private int corefId = runningId.incrementAndGet();;
    private String corefValue;
    private AtomicInteger mentionsCount = new AtomicInteger(0);
    private CorefType corefType = CorefType.NA;
    private boolean markedForRemoval = false;

    private WikiLinksCoref(String corefValue) {
        this.corefValue = corefValue;
    }

    public static synchronized WikiLinksCoref getAndSetIfNotExistCorefChain(String corefValue) {
        if(!globalCorefIds.containsKey(corefValue)) {
            globalCorefIds.put(corefValue, new WikiLinksCoref(corefValue));
        }

        return globalCorefIds.get(corefValue);
    }

    public static void removeKey(String keyToRemove) {
        if(globalCorefIds.contains(keyToRemove)) {
            globalCorefIds.remove(keyToRemove);
        }
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

    public CorefType getCorefType() {
        return corefType;
    }

    public void setCorefType(CorefType corefType) {
        this.corefType = corefType;
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    public void setMarkedForRemoval(boolean markedForRemoval) {
        this.markedForRemoval = markedForRemoval;
    }

    @Override
    public String getColumnNames() {
        return "corefId, corefValue, mentionsCount, corefType";
    }

    @Override
    public String getColumnNamesAndValues() {
        return "" +
                "corefId BIGINT," +
                "corefValue VARCHAR(500), " +
                "mentionsCount INT," +
                "corefType INT," +
                "PRIMARY KEY (corefId)";
    }

    @Override
    public String getValues() {
        return corefId + "," +
                "'" + corefValue + "'" +
                "," + mentionsCount.get() +
                "," + corefType.ordinal();
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
        preparedStatement.setInt(4, this.corefType.ordinal());
    }

    @Override
    public String getPrepareInsertStatementQuery(String tableName) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(tableName).append(" ")
                .append("(").append(getColumnNames()).append(")").append(" ")
                .append("VALUES").append(" ")
                .append("(?,?,?,?)")
                .append(";");

        return query.toString();
    }

    public static WikiLinksCoref resultSetToObject(ResultSet rs) throws SQLException {
        final int corefId = rs.getInt("corefId");
        final String corefValue = rs.getString("corefValue");
        final int mentionsCount = rs.getInt("mentionsCount");
        final int corefType = rs.getInt("corefType");

        WikiLinksCoref extractedCoref = new WikiLinksCoref(corefValue);
        extractedCoref.corefId = corefId;
        extractedCoref.mentionsCount = new AtomicInteger(mentionsCount);
        extractedCoref.corefType = CorefType.values()[corefType];

        return extractedCoref;
    }
}
