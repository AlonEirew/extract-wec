package wikilinks;

import org.junit.Assert;
import org.junit.Test;
import persistence.SQLApi;
import persistence.SQLObject;

import java.util.*;

public class TestDataSQLApi {

    private SQLApi api = new SQLApi();

    @Test
    public void testCreateDataBase() {
        final boolean testDB = api.createDataBase("TestDB");
        Assert.assertTrue(testDB);
    }

    @Test
    public void testDeleteDB() {
        final boolean testDB = api.deleteDataBase("TestDB");
        Assert.assertTrue(testDB);
    }

    @Test
    public void testCreateTable() {
        WikiLinksMention columns = new WikiLinksMention();
        final boolean table = api.createTable("TestDB", "TestTable", columns);
        Assert.assertTrue(table);
    }

    @Test
    public void testDeleteTable() {
        final boolean testTable = api.deleteTable("TestDB", "TestTable");
        Assert.assertTrue(testTable);
    }

    @Test
    public void testInsertToTable() {
        List<SQLObject> objList = new ArrayList<>();
        String[] context = { "bolb", "bolo", "blogb", "blob"};
        objList.add(new WikiLinksMention(
                WikiLinksCoref.getCorefChain("test1"),
                "test",
                0,
                1,
                "Test",
                Arrays.asList(context)));

        objList.add(new WikiLinksMention(
                WikiLinksCoref.getCorefChain("test1"),
                "test",
                0,
                1,
                "Test",
                Arrays.asList(context)));

        final boolean b = api.insertRowsToTable("TestDB", "TestTable", objList);
        Assert.assertTrue(b);
    }
}
