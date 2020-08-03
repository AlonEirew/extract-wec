package wec;

import org.junit.Assert;
import org.junit.Test;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import data.WECMention;

import java.sql.SQLException;

public class TestSQLiteApi {

//    @Test
    public void testSQLiteApi() throws SQLException {
        TestSQLQueryApi sqlTest = new TestSQLQueryApi();
        SQLQueryApi api = new SQLQueryApi(new SQLiteConnections());
        sqlTest.testInsertToTable(api);
    }

//    @Test
    public void testDeleteTable() throws SQLException {
        SQLQueryApi api = new SQLQueryApi(new SQLiteConnections());
        WECMention columns = new WECMention();
        final boolean testTable = api.deleteTable(columns);
        Assert.assertTrue(testTable);
    }
}
