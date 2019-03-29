package wikilinks;

import org.junit.Assert;
import org.junit.Test;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import persistence.WikiLinksMention;

import java.sql.SQLException;

public class TestSQLiteApi {

    @Test
    public void testSQLiteApi() throws SQLException {
        TestSQLQueryApi sqlTest = new TestSQLQueryApi();
        SQLQueryApi api = new SQLQueryApi(new SQLiteConnections());
        sqlTest.testInsertToTable(api);
    }

    @Test
    public void testDeleteTable() throws SQLException {
        SQLQueryApi api = new SQLQueryApi(new SQLiteConnections());
        WikiLinksMention columns = new WikiLinksMention();
        final boolean testTable = api.deleteTable(columns);
        Assert.assertTrue(testTable);
    }
}
