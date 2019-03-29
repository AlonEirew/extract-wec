package persistence;

import java.sql.Connection;
import java.sql.SQLException;

public interface ISQLConnection {
    Connection getConnection() throws SQLException;
}
