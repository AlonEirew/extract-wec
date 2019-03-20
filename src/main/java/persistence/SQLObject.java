package persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SQLObject {
    String getColumnNames();
    String getColumnNamesAndValues();
    String getValues();
    void setPrepareInsertStatementValues(PreparedStatement statement) throws SQLException;
    String getPrepareInsertStatementQuery(String tableName);
}
