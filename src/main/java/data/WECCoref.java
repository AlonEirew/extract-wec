package data;

import persistence.ISQLObject;
import wec.DefaultInfoboxExtractor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WECCoref implements ISQLObject<WECCoref> {
    public static final String TABLE_COREF = "CorefChains";

    private static volatile AtomicInteger runningId = new AtomicInteger();
    private static final ConcurrentHashMap<String, WECCoref> globalCorefIds = new ConcurrentHashMap<>();

    private int corefId = runningId.incrementAndGet();;
    private String corefValue;
    private AtomicInteger mentionsCount = new AtomicInteger(0);
    private String corefType = DefaultInfoboxExtractor.NA;
    private String corefSubType = DefaultInfoboxExtractor.NA;
    private boolean markedForRemoval = false;
    private boolean wasRetrived = false;

    public WECCoref() {}

    public WECCoref(String corefValue) {
        this.corefValue = corefValue;
    }

    public static synchronized WECCoref getAndSetIfNotExist(String corefValue) {
        if(!globalCorefIds.containsKey(corefValue)) {
            globalCorefIds.put(corefValue, new WECCoref(corefValue));
        }

        return globalCorefIds.get(corefValue);
    }

    public static void removeKey(String keyToRemove) {
        if(globalCorefIds.contains(keyToRemove)) {
            globalCorefIds.remove(keyToRemove);
        }
    }

    public static Map<String, WECCoref> getGlobalCorefMap() {
        return globalCorefIds;
    }

    public int getCorefId() {
        return corefId;
    }

    public String getCorefValue() {
        return corefValue;
    }

    public void setCorefValue(String corefValue) {
        this.corefValue = corefValue;
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

    public String getCorefType() {
        return corefType;
    }

    public void setCorefType(String corefType) {
        this.corefType = corefType;
    }

    public void setCorefSubType(String corefSubType) {
        this.corefSubType = corefSubType;
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    public void setMarkedForRemoval(boolean markedForRemoval) {
        this.markedForRemoval = markedForRemoval;
    }

    public boolean wasAlreadyRetrived() {
        return wasRetrived;
    }

    public void setWasAlreadyRetrived(boolean wasRetrived) {
        this.wasRetrived = wasRetrived;
    }

    @Override
    public String getColumnNames() {
        return "corefId, corefValue, mentionsCount, corefType, corefSubType";
    }

    @Override
    public String getColumnNamesAndValues() {
        return "" +
                "corefId BIGINT," +
                "corefValue VARCHAR(500), " +
                "mentionsCount INT," +
                "corefType INT," +
                "corefSubType INT," +
                "PRIMARY KEY (corefId)";
    }

    @Override
    public String getValues() {
        return corefId + "," +
                "'" + this.corefValue + "'" +
                "," + this.mentionsCount.get() +
                "," + this.corefType +
                "," + this.corefSubType;
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
        preparedStatement.setString(4, this.corefType);
        preparedStatement.setString(5, this.corefSubType);
    }

    @Override
    public String getPrepareInsertStatementQuery(String tableName) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(tableName).append(" ")
                .append("(").append(getColumnNames()).append(")").append(" ")
                .append("VALUES").append(" ")
                .append("(?,?,?,?,?)")
                .append(";");

        return query.toString();
    }

    @Override
    public WECCoref resultSetToObject(ResultSet rs) throws SQLException {
        final int corefId = rs.getInt("corefId");
        final String corefValue = rs.getString("corefValue");
        final int mentionsCount = rs.getInt("mentionsCount");
        final String corefType = rs.getString("corefType");
        final String corefSubType = rs.getString("corefSubType");

        WECCoref extractedCoref = new WECCoref(corefValue);
        extractedCoref.corefId = corefId;
        extractedCoref.mentionsCount = new AtomicInteger(mentionsCount);
        extractedCoref.corefType = corefType;
        extractedCoref.corefSubType = corefSubType;

        return extractedCoref;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WECCoref wecCoref = (WECCoref) o;
        return corefId == wecCoref.corefId &&
                markedForRemoval == wecCoref.markedForRemoval &&
                wasRetrived == wecCoref.wasRetrived &&
                Objects.equals(corefValue, wecCoref.corefValue) &&
                Objects.equals(mentionsCount, wecCoref.mentionsCount) &&
                Objects.equals(corefType, wecCoref.corefType) &&
                Objects.equals(corefSubType, wecCoref.corefSubType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corefId, corefValue, mentionsCount, corefType, corefSubType, markedForRemoval, wasRetrived);
    }
}
