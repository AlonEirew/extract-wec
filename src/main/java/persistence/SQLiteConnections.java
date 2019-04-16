package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnections implements ISQLConnection {
    private static final String CONNECTION_URL = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEvent_v2.db";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_URL);
    }
}
