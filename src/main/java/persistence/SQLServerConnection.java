package persistence;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLServerConnection implements ISQLConnection {

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

    @Override
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
