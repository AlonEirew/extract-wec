package persistence;

import com.google.common.collect.Iterables;
import data.WECCoref;
import data.WECMention;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wec.validators.DefaultInfoboxValidator;

import java.sql.*;
import java.util.*;

public class SQLQueryApi {
    private final static Logger LOGGER = LogManager.getLogger(SQLQueryApi.class);

    private static final int MAX_BULK_SIZE = 500;
    private static final int MAX_MENTIONS_ACCUMULATED_SIZE = 500000;
    private static final List<WECMention> accumulatedMentions = new ArrayList<>();

    private final ISQLConnection sqlConnection;

    public SQLQueryApi(ISQLConnection sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    public synchronized boolean useDataBase(String dbName) {
        boolean result = false;
        try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {
            String sqlUse = "USE " + dbName;
            stmt.execute(sqlUse);
            LOGGER.info("Now using-" + dbName);
            result = true;
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return result;
    }

    public synchronized boolean createDataBase(String dbName) throws SQLException {
        boolean result = false;
        if(dbName != null && !dbName.isEmpty()) {
            try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {

                if (isDbExists(dbName)) {
                    LOGGER.info("Database already exists");
                    result = true;
                }

                String sql = "CREATE DATABASE " + dbName;
                stmt.executeUpdate(sql);
                LOGGER.info("Database created successfully...");
                result = true;
            }
        }

        LOGGER.info("Provided name is null or empty!");
        return result;
    }

    public synchronized boolean deleteDataBase(String dbName) throws SQLException {
        try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {

            if (!isDbExists(dbName)) {
                LOGGER.info("No such data base exists");
            }

            String sql = "DROP DATABASE " + dbName;
            stmt.executeUpdate(sql);
            LOGGER.info("Database deleted successfully...");
        }

        return true;
    }

    public synchronized <T extends ISQLObject<T>> boolean createTable(T objectRep) throws SQLException {
        try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {
            if(!isTableExists(objectRep)) {
                StringBuilder createTableSql = new StringBuilder();
                createTableSql.append("CREATE TABLE ").append(objectRep.getTableName()).append(" (").append(objectRep.getColumnNamesAndValues()).append(")");
                createTableSql.delete(createTableSql.length() - 1, createTableSql.length());
                createTableSql.append(");");
                stmt.executeUpdate(createTableSql.toString());
                LOGGER.info("Table created successfully...");
            } else {
                LOGGER.info("Table already exists");
            }
        }

        return true;
    }

    public synchronized <T extends ISQLObject<T>> boolean deleteTable(T repObject) throws SQLException {
        try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {
            if(isTableExists(repObject)) {
                String deleteTableSql = "DROP TABLE " + repObject.getTableName();
                stmt.executeUpdate(deleteTableSql);
                LOGGER.info("Table deleted successfully...");
            } else {
                LOGGER.info("No Such Table");
            }
        }

        return true;
    }

    public synchronized <T extends ISQLObject> boolean insertRowsToTable(List<T> insertRows) throws SQLException {
        if(insertRows != null && !insertRows.isEmpty()) {
            int totalToPersist = insertRows.size();
            LOGGER.info("Praper to persist " + totalToPersist + " rows");
            final T objRep = insertRows.get(0);

            try (Connection con = this.sqlConnection.getConnection();
                 PreparedStatement stmt = con.prepareStatement(objRep.getPrepareInsertStatementQuery())) {
                for (List<T> partRows : Iterables.partition(insertRows, MAX_BULK_SIZE)) {
                    for (ISQLObject sqlObject : partRows) {
                        sqlObject.setPrepareInsertStatementValues(stmt);
                        stmt.addBatch();
                    }

                    stmt.executeBatch();
                    LOGGER.info("rows added successfully");
                    totalToPersist = totalToPersist - partRows.size();
                    LOGGER.info(totalToPersist + " rows remaining");
                }
            }
        }

        return true;
    }

    public <T extends ISQLObject<T>> List<T> readJoinedMentionCorefTable(T resultObjectRef) {
        List<T> resultList = new ArrayList<>();
        String query = "SELECT * from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId";
        try (Connection conn = this.sqlConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            try(ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    final T coref = resultObjectRef.resultSetToObject(rs);
                    resultList.add(coref);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return resultList;
    }

    public <T extends ISQLObject<T>> List<T> readTable(String table, T resultObjectRef) {
        List<T> resultList = new ArrayList<>();
        String query = "Select * from " + table;
        try (Connection conn = this.sqlConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            try(ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    final T coref = resultObjectRef.resultSetToObject(rs);
                    resultList.add(coref);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return resultList;
    }

    public Map<Integer, List<WECMention>> readMentionsByCorefIds(Set<Integer> corefIds, int limit) {
        WECMention parsingObj = new WECMention();
        Map<Integer, List<WECMention>> results = new HashMap<>();
        String query = "SELECT * from Mentions";
        if(limit > 0) {
            query += " limit " + limit;
        }
        try (Connection conn = this.sqlConnection.getConnection()) {
            conn.setAutoCommit(false);
            try(Statement stmt = conn.createStatement()) {
                stmt.setFetchSize(500);
                try (ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        WECMention mention = parsingObj.resultSetToObject(rs);
                        if (corefIds.contains(mention.getCorefId())) {
                            if (!results.containsKey(mention.getCorefId())) {
                                results.put(mention.getCorefId(), new ArrayList<>());
                            }
                            results.get(mention.getCorefId()).add(mention);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return results;
    }

    public synchronized boolean updateCorefTable(String tableName, Map<Integer, WECCoref> corefs) {
        String query = "Select * from CorefChains where corefid in";
        try (Connection conn = this.sqlConnection.getConnection();
                Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            conn.setAutoCommit(false);

            Iterator<List<Integer>> subSets = Iterables.partition(corefs.keySet(), MAX_BULK_SIZE).iterator();
            Map<Integer, WECCoref> copyCoref = new HashMap<>(corefs);

            while(subSets.hasNext()) {
                final List<Integer> next = subSets.next();
                try(ResultSet rs = stmt.executeQuery(query + " (" + StringUtils.join(next, ",") + ")")) {

                    while (rs.next()) {
                        int corefId = rs.getInt("corefId");
//                      String corefValue = rs.getString("corefValue");
                        int count = rs.getInt("mentionsCount");

                        int newCount = corefs.get(corefId).addAndGetMentionCount(count);
                        rs.updateInt("mentionsCount", newCount);
                        rs.updateRow();
                        copyCoref.remove(corefId);
                    }
                }
            }

            conn.setAutoCommit(true);

            WECCoref tmp = WECCoref.getAndSetIfNotExist("####TEMP####");

            if(!copyCoref.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(tmp.getPrepareInsertStatementQuery())) {
                    for (WECCoref remainCoref : copyCoref.values()) {
                        remainCoref.setPrepareInsertStatementValues(ps);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                }
            }

        } catch (SQLException e) {
            LOGGER.error(e);
            return false;
        }

        return true;
    }

    public boolean isDbExists(String dbName) throws SQLException {
        boolean result = false;

        try (Connection conn = this.sqlConnection.getConnection();
             ResultSet resultSet = conn.getMetaData().getCatalogs()) {
            while (resultSet.next()) {
                String curDbName = resultSet.getString(1);
                if (curDbName.equalsIgnoreCase(dbName)) {
                    result = true;
                }
            }
        }

        return result;
    }

    public <T extends ISQLObject<T>> boolean isTableExists(T objectRep) throws SQLException {
        boolean result = false;

        try(Connection conn = this.sqlConnection.getConnection();
                ResultSet resultSet = conn.getMetaData().getTables(null, null, "%", null)) {
            while (resultSet.next()) {
                String curTableName = resultSet.getString(3);
                if (curTableName.equalsIgnoreCase(objectRep.getTableName())) {
                    result = true;
                }
            }
        }

        return result;
    }

    public synchronized void persistAllCorefs() {
        LOGGER.info("Persisting corefs tables values");
        final Collection<WECCoref> allCorefs = WECCoref.getGlobalCorefMap().values();
        allCorefs.removeIf(wikiLinksCoref -> wikiLinksCoref.getMentionsCount() == 0 ||
                wikiLinksCoref.getCorefType().equals(DefaultInfoboxValidator.NA) ||
                wikiLinksCoref.isMarkedForRemoval());

        LOGGER.info("Preparing to add " + allCorefs.size() + " corefs tables values");

        try {
            if (!insertRowsToTable(new ArrayList<>(allCorefs))) {
                LOGGER.error("persistAllCorefs: Failed to insert Corefs!!!!");
            }
        } catch (SQLException e) {
            LOGGER.error("persistAllCorefs:", e);
        }
    }

    /**
     * Commit the final mentions to SQL DB (Lock is necessary for SQLite)
     */
    public synchronized void commitMentions(List<WECMention> mentions) {
        if(!mentions.isEmpty()) {
            LOGGER.info("accumulating-" + mentions.size() + " mentions before inserting to SQL");
            accumulatedMentions.addAll(mentions);
            if(accumulatedMentions.size() >= MAX_MENTIONS_ACCUMULATED_SIZE) {
                LOGGER.info("Prepare to inset-" + accumulatedMentions.size() + " mentions to SQL");
                try {
                    if (!insertRowsToTable(accumulatedMentions)) {
                        LOGGER.error("Failed to insert mentions batch!!!!");
                    }
                } catch (SQLException e) {
                    LOGGER.error("SQLException", e);
                } finally {
                    accumulatedMentions.clear();
                }
            }
        }
    }

    public synchronized void persistAllMentions() {
        LOGGER.info("Prepare to inset-" + accumulatedMentions.size() + " mentions to SQL");
        try {
            if (!insertRowsToTable(accumulatedMentions)) {
                LOGGER.error("Failed to insert mentions batch!!!!");
            }
        } catch (SQLException e) {
            LOGGER.error("SQLException", e);
        } finally {
            accumulatedMentions.clear();
        }
    }

    public WECCoref getCorefById(int corefId) {
        WECCoref wecCoref = new WECCoref(null);
        String query = "Select * from " + WECCoref.TABLE_COREF + " where corefId=" + corefId;
        try (Connection conn = this.sqlConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            try(ResultSet rs = stmt.executeQuery(query)) {
                wecCoref = wecCoref.resultSetToObject(rs);
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return wecCoref;
    }
}
