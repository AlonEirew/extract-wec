package wec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import data.WECCoref;
import data.WECMention;
import org.junit.Assert;
import persistence.ISQLObject;
import persistence.SQLQueryApi;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TestSQLQueryApi {
    Gson GSON = new Gson();

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
        JsonObject o1 = new JsonObject();
        JsonObject o2 = new JsonObject();
        JsonObject o3 = new JsonObject();
        JsonObject o4 = new JsonObject();
        o1.addProperty("bolb", 0);
        o2.addProperty("bolb", 1);
        o3.addProperty("blogb", 2);
        o4.addProperty("blob", 3);

        JsonArray asJsonArray = new JsonArray();
        asJsonArray.add(o1);
        asJsonArray.add(o2);
        asJsonArray.add(o3);
        asJsonArray.add(o4);

        objList.add(new WECMention(
                WECCoref.getAndSetIfNotExist("test1"),
                "test",
                0,
                1,
                "Test",
                asJsonArray));

        objList.add(new WECMention(
                WECCoref.getAndSetIfNotExist("test1"),
                "test",
                0,
                1,
                "Test",
                asJsonArray));

        final boolean b = api.insertRowsToTable(objList);
        Assert.assertTrue(b);
        testDeleteTable(api);
    }
}
