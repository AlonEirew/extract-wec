package wikilinks;

import persistence.SQLQueryApi;
import persistence.SQLServerConnection;
import persistence.SQLiteConnections;

import java.io.IOException;
import java.sql.SQLException;

public class WikiToWikiLinksMain {

    public static void main(String[] args) throws IOException, SQLException {
        CreateWikiLinks createWikiLinks = new CreateWikiLinks(new SQLQueryApi(new SQLiteConnections()));

        long start = System.currentTimeMillis();
        createWikiLinks.readAllAndPerisist();
        long end = System.currentTimeMillis();
        System.out.println("Process Done, took-" + (end - start) + "ms to run");
    }
}
