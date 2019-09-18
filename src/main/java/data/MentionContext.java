package data;

import persistence.ISQLObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MentionContext implements ISQLObject {
    private static final String TABLE_CONTEXTS = "Contexts";
    private static final AtomicInteger runningId = new AtomicInteger(1);

    private static final Map<Integer, MentionContext> allContexts = new HashMap<>();

    private final int contextId = runningId.getAndIncrement();
    private List<String> context;
    private AtomicInteger linkedMentions = new AtomicInteger(0);

    public static MentionContext createNewAndAddToSet(List<String> context) {
        MentionContext newContext = new MentionContext(context);
        allContexts.put(newContext.getContextId(), newContext);
        return newContext;
    }

    public static void addContextToSet(MentionContext context) {
        if(context != null) {
            allContexts.put(context.getContextId(), context);
        }
    }

    public static Map<Integer, MentionContext> getAllContexts() {
        return allContexts;
    }

    public MentionContext(List<String> context) {
        this.context = context;
    }

    public void addLinkedMention() {
        this.linkedMentions.incrementAndGet();
    }

    public void addLinkedMention(int mentionToAdd) {
        this.linkedMentions.addAndGet(mentionToAdd);
    }

    public synchronized void removeLinkedMention() {
        final int linkedMentions = this.linkedMentions.decrementAndGet();
        if(linkedMentions == 0) {
            allContexts.remove(this.getContextId());
        }
    }

    public int getContextId() {
        return contextId;
    }

    public List<String> getContext() {
        return context;
    }

    public boolean isValid() {
        if(this.context.contains("#") ||
            this.context.contains("jpg") ||
            this.context.contains("{") ||
            this.context.contains("}")) {
            return false;
        }

        return true;
    }

    public String getContextAsSQLBlob() {
        return String.join(" ", this.context);
    }

    @Override
    public String getColumnNames() {
        return "contextId, context, linkedMentions";
    }

    @Override
    public String getColumnNamesAndValues() {
        return "" +
                "contextId INT," +
                "context TEXT," +
                "linkedMentions INT," +
                "PRIMARY KEY (contextId)";
    }

    @Override
    public String getValues() {
        return contextId + "," +
                "'" + this.getContextAsSQLBlob() + "'";
    }

    @Override
    public String getTableName() {
        return TABLE_CONTEXTS;
    }

    @Override
    public void setPrepareInsertStatementValues(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setLong(1, this.getContextId());
        preparedStatement.setString(2, this.getContextAsSQLBlob());
        preparedStatement.setInt(3, this.linkedMentions.get());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MentionContext that = (MentionContext) o;
        return contextId == that.contextId &&
                Objects.equals(context, that.context) &&
                Objects.equals(linkedMentions, that.linkedMentions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId, context, linkedMentions);
    }
}
