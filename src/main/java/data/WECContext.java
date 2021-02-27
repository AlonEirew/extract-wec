package data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import config.Configuration;
import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class WECContext implements ISQLObject<WECContext> {
    private static final String TABLE_CONTEXTS = "Contexts";
    private static final AtomicInteger idGen = new AtomicInteger();

    private final int contextId;
    private transient JsonArray context;

    public WECContext(JsonArray context) {
        this.contextId = idGen.incrementAndGet();
        this.context = context;
    }

    public WECContext(int contextId) {
        this.contextId = contextId;
    }

    public WECContext(int contextId, JsonArray asJsonObject) {
        this.contextId = contextId;
        this.context = asJsonObject;
    }

    public int getContextId() {
        return contextId;
    }

    public JsonArray getContext() {
        return context;
    }

    public void setContext(JsonArray jsonArray) {
        this.context = jsonArray;
    }

    public String getContextAsJsonString() {
        return Configuration.GSON.toJson(this.context);
    }

    public String getContextAsString() {
        List<String> contextList = new ArrayList<>();
        for(JsonElement tok : this.context) {
            for (Map.Entry<String, JsonElement> entry : tok.getAsJsonObject().entrySet()) {
                contextList.add(entry.getKey());
            }
        }

        return String.join(" ", contextList);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WECContext that = (WECContext) o;
        return contextId == that.contextId && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId, context);
    }

    @Override
    public String getColumnNames() {
        return "contextId, context";
    }

    @Override
    public String getColumnNamesAndValues() {
        return "contextId INT," +
                "context TEXT," +
                "PRIMARY KEY (contextId)";
    }

    @Override
    public String getValues() {
        return getContextId() + "," +
                "'" + getContextAsJsonString() + "'";
    }

    @Override
    public String getTableName() {
        return TABLE_CONTEXTS;
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setLong(1, this.getContextId());
        preparedStatement.setString(2, getContextAsJsonString());
    }

    @Override
    public String getPrepareInsertStatementQuery() {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(TABLE_CONTEXTS).append(" ")
                .append("(").append(getColumnNames()).append(")").append(" ")
                .append("VALUES").append(" ")
                .append("(?,?,?,?,?,?,?,?)")
                .append(";");

        return query.toString();
    }

    @Override
    public WECContext resultSetToObject(ResultSet rs) throws SQLException {
        final int contextId = rs.getInt("contextId");
        final String contextStr = rs.getString("context");

        JsonArray asJsonObject = new JsonParser().parse(contextStr).getAsJsonArray();

        return new WECContext(contextId, asJsonObject);
    }
}
