package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnections implements ISQLConnection {

    private String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksTestDB.db";

    public SQLiteConnections() { }

    public SQLiteConnections(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.connectionUrl);
    }
}
