package wikilinks;

import data.WikiLinksCoref;
import data.WikiLinksMention;
import org.junit.Assert;
import org.junit.Test;
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
        WikiLinksMention columns = new WikiLinksMention();
        final boolean table = api.createTable( columns);
        Assert.assertTrue(table);
    }

    public void testDeleteTable(SQLQueryApi api) throws SQLException {
        WikiLinksMention columns = new WikiLinksMention();
        final boolean testTable = api.deleteTable(columns);
        Assert.assertTrue(testTable);
    }

    public void testInsertToTable(SQLQueryApi api) throws SQLException {
        testCreateTable(api);
        List<ISQLObject> objList = new ArrayList<>();
        String[] context = { "bolb", "bolo", "blogb", "blob"};
        objList.add(new WikiLinksMention(
                WikiLinksCoref.getAndSetIfNotExistCorefChain("test1"),
                "test",
                0,
                1,
                "Test",
                Arrays.asList(context)));

        objList.add(new WikiLinksMention(
                WikiLinksCoref.getAndSetIfNotExistCorefChain("test1"),
                "test",
                0,
                1,
                "Test",
                Arrays.asList(context)));

        final boolean b = api.insertRowsToTable(objList);
        Assert.assertTrue(b);
        testDeleteTable(api);
    }
}
