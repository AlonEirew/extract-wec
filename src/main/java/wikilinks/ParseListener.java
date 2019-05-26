package wikilinks;

import data.RawElasticResult;
import data.WikiLinksMention;
import persistence.SQLQueryApi;

import java.sql.SQLException;
import java.util.*;

public class ParseListener {
    private static final int MAX_SIZE = 500000;

    private volatile List<WikiLinksMention> mentions = new ArrayList<>();

    private SQLQueryApi sqlApi;
    private CreateWikiLinks wikiLinks;
    private ICorefFilter filter;

    public ParseListener(SQLQueryApi sqlApi, CreateWikiLinks wikiLinks, ICorefFilter filter) {
        this.sqlApi = sqlApi;
        this.filter = filter;
        this.wikiLinks = wikiLinks;
    }

    public synchronized void handle(List<WikiLinksMention> mentionsToAdd) {
        this.mentions.addAll(mentionsToAdd);

        if(this.mentions.size() >= MAX_SIZE) {
            System.out.println("Limit reached starting to process-" + this.mentions.size() + " mentions");
            this.handle();
        }
    }

    public synchronized void handle() {
        Set<String> corefTitleSet = new HashSet<>();
        for(WikiLinksMention mention : mentions) {
            corefTitleSet.add(mention.getCorefChain().getCorefValue());
        }

        Map<String, String> allPagesText = null;
        try {
            allPagesText = this.wikiLinks.getAllPagesTitleAndText(corefTitleSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(allPagesText != null) {
            final Iterator<WikiLinksMention> iterator = mentions.iterator();
            while (iterator.hasNext()) {
                WikiLinksMention ment = iterator.next();
                final String corefValue = ment.getCorefChain().getCorefValue();
                final String pageText = allPagesText.get(corefValue);
                RawElasticResult rawElasticResult = new RawElasticResult(corefValue, pageText);

                if (ment.getCorefChain().isMarkedForRemoval() || this.filter.isConditionMet(rawElasticResult)) {
                    iterator.remove();
                    ment.getCorefChain().setMarkedForRemoval(true);
                }
            }

            try {
                if (!this.sqlApi.insertRowsToTable(this.mentions)) {
                    System.out.println("Failed to insert mentions Batch!!!!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        this.mentions.clear();
    }
}
