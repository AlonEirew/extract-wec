package wikilinks;

import persistence.SQLQueryApi;
import persistence.WikiLinksCoref;
import persistence.WikiLinksMention;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParseListener {
    private static final int MAX_SIZE = 1000000;

    private volatile List<WikiLinksMention> mentions = new ArrayList<>();

    private SQLQueryApi sqlApi;

    public ParseListener(SQLQueryApi sqlApi) {
        this.sqlApi = sqlApi;
    }

    public synchronized void handle(List<WikiLinksMention> mentionsToAdd) {
        mentions.addAll(mentionsToAdd);

        if(mentions.size() >= MAX_SIZE) {
            System.out.println("Limit reached starting to process-" + this.mentions.size() + " mentions");
            this.handle();
        }
    }

    public synchronized void handle() {
        final Iterator<WikiLinksMention> iterator = mentions.iterator();
        while(iterator.hasNext()) {
            WikiLinksMention ment = iterator.next();
            if(ment.getCorefChain().getMentionsCount() == 1) {
                iterator.remove();
                WikiLinksCoref.removeKey(ment.getCorefChain().getCorefValue());
            }
        }

        try {
            if (!this.sqlApi.insertRowsToTable(this.mentions)) {
                System.out.println("Failed to insert mentions Batch!!!!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        this.mentions.clear();
    }
}
