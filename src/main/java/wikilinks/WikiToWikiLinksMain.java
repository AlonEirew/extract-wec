package wikilinks;

import java.io.IOException;

public class WikiToWikiLinksMain {

    public static void main(String[] args) throws IOException {
        CreateWikiLinks createWikiLinks = new CreateWikiLinks();

        long start = System.currentTimeMillis();
        createWikiLinks.readAllAndPerisist();
        long end = System.currentTimeMillis();
        System.out.println("Process Done, took-" + (end - start) + "ms to run");
    }
}
