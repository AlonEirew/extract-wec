package persistence;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.*;
import java.util.List;

public class SQLApi {

    private static final String CONNECTION_URL = "jdbc:sqlserver://localhost:1433;databaseName=WikiLinks;";
    private static final String USER = "wikilink";
    private static final String PASSWORD = "Pa5$W0rdA1#nE1r@w";

    private static BasicDataSource ds = new BasicDataSource();

    static {
        ds.setUrl(CONNECTION_URL);
        ds.setUsername(USER);
        ds.setPassword(PASSWORD);
        ds.setMinIdle(5);
        ds.setMaxIdle(10);
        ds.setInitialSize(10);
        ds.setMaxOpenPreparedStatements(50);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public boolean useDataBase(String dbName) {
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
            String sqlUse = "USE " + dbName;
            stmt.execute(sqlUse);
            System.out.println("Now using-" + dbName);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createDataBase(String dbName) {
        if(dbName != null && !dbName.isEmpty()) {
            try (Connection con = getConnection(); Statement stmt = con.createStatement()) {

                if (isDbExists(dbName, con)) {
                    System.out.println("Database already exists");
                    return true;
                }

                String sql = "CREATE DATABASE " + dbName;
                stmt.executeUpdate(sql);
                System.out.println("Database created successfully...");
                return true;
            }
            catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        System.out.println("Provided name is null or empty!");
        return false;
    }

    public boolean deleteDataBase(String dbName) {
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {

            if (!isDbExists(dbName, con)) {
                System.out.println("No such data base exists");
                return true;
            }

            String sql = "DROP DATABASE " + dbName;
            stmt.executeUpdate(sql);
            System.out.println("Database deleted successfully...");
            return true;
        }
        // Handle any errors that may have occurred.
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public <T extends SQLObject> boolean createTable(String dbName, String tableName, T objectRep) {
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {

            if (!isDbExists(dbName, con)) {
                System.out.println("No such data base exists");
                return false;
            }

            if(!isTableExists(tableName, con)) {
                StringBuilder createTableSql = new StringBuilder();
                createTableSql.append("CREATE TABLE " + tableName + " (").append(objectRep.getColumnNamesAndValues()).append(")");
                createTableSql.delete(createTableSql.length() - 1, createTableSql.length());
                createTableSql.append(");");
                stmt.executeUpdate(createTableSql.toString());
                System.out.println("Table created successfully...");
                return true;
            } else {
                System.out.println("Table already exists");
                return true;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteTable(String dbName, String tableName) {
        try (Connection con = getConnection(); Statement stmt = con.createStatement()) {

            if (!isDbExists(dbName, con)) {
                System.out.println("No such data base exists");
                return false;
            }

            String createTableSql = "DROP TABLE " + tableName;
            stmt.executeUpdate(createTableSql);
            System.out.println("Table deleted successfully...");
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertRowsToTable(String dbName, String tableName, List<? extends SQLObject> insertRows, Connection con) {
        if(insertRows != null && !insertRows.isEmpty()) {
            try (PreparedStatement stmt = con.prepareStatement(insertRows.get(0).getPrepareInsertStatementQuery(tableName))) {

                if (!isDbExists(dbName, con)) {
                    System.out.println("No such data base exists");
                    return false;
                }

                for (SQLObject sqlObject : insertRows) {
                    sqlObject.setPrepareInsertStatementValues(stmt);
                    stmt.addBatch();
                }

                stmt.executeBatch();
                return true;
            } catch (BatchUpdateException bue) {
                bue.printStackTrace();
                return false;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public boolean insertRowsToTable(String dbName, String tableName, List<? extends SQLObject> insertRows) {
        if(insertRows != null && !insertRows.isEmpty()) {
            try (Connection con = getConnection(); PreparedStatement stmt =
                    con.prepareStatement(insertRows.get(0).getPrepareInsertStatementQuery(tableName))) {

                if (!isDbExists(dbName, con)) {
                    System.out.println("No such data base exists");
                    return false;
                }

                for (SQLObject sqlObject : insertRows) {
                    sqlObject.setPrepareInsertStatementValues(stmt);
                    stmt.addBatch();
                }

                stmt.executeBatch();
                return true;
            } catch (BatchUpdateException bue) {
                bue.printStackTrace();
                return false;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private boolean isDbExists(String dbName, Connection con) throws SQLException {
        ResultSet resultSet = con.getMetaData().getCatalogs();

        while (resultSet.next()) {
            String curDbName = resultSet.getString(1);
            if(curDbName.equalsIgnoreCase(dbName)) {
                return true;
            }
        }

        resultSet.close();
        return false;
    }

    private boolean isTableExists(String tableName, Connection con) throws SQLException {
        ResultSet resultSet = con.getMetaData().getTables(null, null, "%", null);

        while (resultSet.next()) {
            String curTableName = resultSet.getString(3);
            if(curTableName.equalsIgnoreCase(tableName)) {
                return true;
            }
        }

        resultSet.close();
        return false;
    }
}
