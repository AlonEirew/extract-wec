package persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface ISQLObject<T> {

    String getColumnNames();
    String getColumnNamesAndValues();
    String getValues();
    String getTableName();
    void setPrepareInsertStatementValues(PreparedStatement statement) throws SQLException;
    String getPrepareInsertStatementQuery(String tableName);
    T resultSetToObject(ResultSet rs) throws SQLException;
}
