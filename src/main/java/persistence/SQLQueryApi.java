package persistence;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.*;

public class SQLQueryApi {

    private static final int MAX_BULK_SIZE = 250;

    private ISQLConnection sqlConnection;

    public SQLQueryApi(ISQLConnection sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    public boolean useDataBase(String dbName) {
        boolean result = false;
        try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {
            String sqlUse = "USE " + dbName;
            stmt.execute(sqlUse);
            System.out.println("Now using-" + dbName);
            result = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public boolean createDataBase(String dbName) throws SQLException {
        boolean result = false;
        if(dbName != null && !dbName.isEmpty()) {
            try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {

                if (isDbExists(dbName, con)) {
                    System.out.println("Database already exists");
                    result = true;
                }

                String sql = "CREATE DATABASE " + dbName;
                stmt.executeUpdate(sql);
                System.out.println("Database created successfully...");
                result = true;
            }
        }

        System.out.println("Provided name is null or empty!");
        return result;
    }

    public boolean deleteDataBase(String dbName) throws SQLException {
        try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {

            if (!isDbExists(dbName, con)) {
                System.out.println("No such data base exists");
            }

            String sql = "DROP DATABASE " + dbName;
            stmt.executeUpdate(sql);
            System.out.println("Database deleted successfully...");
        }

        return true;
    }

    public <T extends ISQLObject> boolean createTable(T objectRep) throws SQLException {
        try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {
            if(!isTableExists(objectRep, con)) {
                StringBuilder createTableSql = new StringBuilder();
                createTableSql.append("CREATE TABLE " + objectRep.getTableName() + " (").append(objectRep.getColumnNamesAndValues()).append(")");
                createTableSql.delete(createTableSql.length() - 1, createTableSql.length());
                createTableSql.append(");");
                stmt.executeUpdate(createTableSql.toString());
                System.out.println("Table created successfully...");
            } else {
                System.out.println("Table already exists");
            }
        }

        return true;
    }

    public <T extends ISQLObject> boolean deleteTable(T repObject) throws SQLException {
        try (Connection con = this.sqlConnection.getConnection(); Statement stmt = con.createStatement()) {
            if(isTableExists(repObject, con)) {
                String deleteTableSql = "DROP TABLE " + repObject.getTableName();
                stmt.executeUpdate(deleteTableSql);
                System.out.println("Table deleted successfully...");
            } else {
                System.out.println("No Such Table");
            }
        }

        return true;
    }

    public <T extends ISQLObject> boolean insertRowsToTable(List<T> insertRows) throws SQLException {
        if(insertRows != null && !insertRows.isEmpty()) {
            int totalToPersist = insertRows.size();
            System.out.println("Praper to persist " + totalToPersist + " rows");
            final T objRep = insertRows.get(0);

            try (Connection con = this.sqlConnection.getConnection(); PreparedStatement stmt =
                    con.prepareStatement(objRep.getPrepareInsertStatementQuery(objRep.getTableName()))) {
                Iterator<List<T>> subSets = Iterables.partition(insertRows, MAX_BULK_SIZE).iterator();
                while(subSets.hasNext()) {
                    List<T> partRows = subSets.next();
                    for (ISQLObject sqlObject : partRows) {
                        sqlObject.setPrepareInsertStatementValues(stmt);
                        stmt.addBatch();
                    }

                    stmt.executeBatch();
                    System.out.println("rows added successfully");
                    totalToPersist = totalToPersist - partRows.size();
                    System.out.println(totalToPersist + " rows remaining");
                }
            }
        }

        return true;
    }

    public boolean updateCorefTable(String tableName, Map<Integer, WikiLinksCoref> corefs) {
        String query = "Select * from CorefChains where corefid in";
        try (Connection conn = this.sqlConnection.getConnection();
                Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            conn.setAutoCommit(false);

            Iterator<List<Integer>> subSets = Iterables.partition(corefs.keySet(), MAX_BULK_SIZE).iterator();
            Map<Integer, WikiLinksCoref> copyCoref = new HashMap<>(corefs);

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

            WikiLinksCoref tmp = WikiLinksCoref.getCorefChain("####TEMP####");

            if(!copyCoref.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(tmp.getPrepareInsertStatementQuery(tableName))) {
                    for (WikiLinksCoref remainCoref : copyCoref.values()) {
                        remainCoref.setPrepareInsertStatementValues(ps);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean isDbExists(String dbName, Connection con) throws SQLException {
        boolean result = false;

        try (ResultSet resultSet = con.getMetaData().getCatalogs()) {
            while (resultSet.next()) {
                String curDbName = resultSet.getString(1);
                if (curDbName.equalsIgnoreCase(dbName)) {
                    result = true;
                }
            }
        }

        return result;
    }

    public <T extends ISQLObject> boolean isTableExists(T objectRep, Connection con) throws SQLException {
        boolean result = false;

        try(ResultSet resultSet = con.getMetaData().getTables(null, null, "%", null)) {
            while (resultSet.next()) {
                String curTableName = resultSet.getString(3);
                if (curTableName.equalsIgnoreCase(objectRep.getTableName())) {
                    result = true;
                }
            }
        }

        return result;
    }
}
