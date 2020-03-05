package wec;

import data.WECCoref;
import data.WECMention;
import org.junit.Assert;
import persistence.*;

import java.sql.SQLException;
import java.util.*;

public class TestSQLQueryApi {

//    @Test
    public void testSQLServerApi() throws SQLException {
        SQLQueryApi api = new SQLQueryApi(new SQLServerConnection());
        testInsertToTable(api);
    }

    public void testCreateTable(SQLQueryApi api) throws SQLException {
        WECMention columns = new WECMention();
        final boolean table = api.createTable( columns);
        Assert.assertTrue(table);
    }

    public void testDeleteTable(SQLQueryApi api) throws SQLException {
        WECMention columns = new WECMention();
        final boolean testTable = api.deleteTable(columns);
        Assert.assertTrue(testTable);
    }

    public void testInsertToTable(SQLQueryApi api) throws SQLException {
        testCreateTable(api);
        List<ISQLObject> objList = new ArrayList<>();
        Map.Entry[] contextSent = {
                new AbstractMap.SimpleEntry<>("bolb", 0),
                new AbstractMap.SimpleEntry<>("bolb", 1),
                new AbstractMap.SimpleEntry<>("blogb", 2),
                new AbstractMap.SimpleEntry<>("blob", 3)
        };
        List<List<Map.Entry<String, Integer>>> context = new ArrayList<>();
        context.add(Arrays.asList(contextSent));
        objList.add(new WECMention(
                WECCoref.getAndSetIfNotExist("test1"),
                "test",
                0,
                1,
                "Test",
                context));

        objList.add(new WECMention(
                WECCoref.getAndSetIfNotExist("test1"),
                "test",
                0,
                1,
                "Test",
                context));

        final boolean b = api.insertRowsToTable(objList);
        Assert.assertTrue(b);
        testDeleteTable(api);
    }
}
