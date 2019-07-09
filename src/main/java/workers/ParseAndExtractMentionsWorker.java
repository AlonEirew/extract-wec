package workers;

import data.RawElasticResult;
import data.WikiLinksMention;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wikilinks.ICorefFilter;
import wikilinks.WikiLinksExtractor;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class ParseAndExtractMentionsWorker extends AWorker {

    private static Object sLock = new Object();
    private static final int COMMIT_MAX_SIZE = 1000000;

    private static final CopyOnWriteArrayList<WikiLinksMention> finalToCommit = new CopyOnWriteArrayList<>();

    private final List<WikiLinksMention> mentions = new ArrayList<>();
    private final SQLQueryApi sqlApi;
    private final ElasticQueryApi elasticApi;
    private final ICorefFilter filter;

    public ParseAndExtractMentionsWorker(List<RawElasticResult> rawElasticResults, SQLQueryApi sqlApi,
                                         ElasticQueryApi elasticApi, ICorefFilter filter) {
        super(rawElasticResults);
        this.sqlApi = sqlApi;
        this.elasticApi = elasticApi;
        this.filter = filter;
    }

    @Override
    public void run() {
        for(RawElasticResult rowResult : this.rawElasticResults) {
            List<WikiLinksMention> wikiLinksMentions = WikiLinksExtractor.extractFromFile(rowResult.getTitle(), rowResult.getText());
            wikiLinksMentions.stream().forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
            this.mentions.addAll(wikiLinksMentions);
        }

        this.handle();
    }

    public void handle() {
        System.out.println("Handle all worker mentions...in total-" + this.mentions.size() + " will be handled");
        Set<String> corefTitleSet = new HashSet<>();
        for(WikiLinksMention mention : this.mentions) {
            corefTitleSet.add(mention.getCorefChain().getCorefValue());
        }

        Map<String, String> allPagesText = null;
        try {
            allPagesText = this.elasticApi.getAllWikiPagesTitleAndText(corefTitleSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(allPagesText != null) {
            final Iterator<WikiLinksMention> iterator = this.mentions.iterator();
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

            synchronized (sLock) {
                finalToCommit.addAll(this.mentions);
                if(finalToCommit.size() >= COMMIT_MAX_SIZE) {
                    commitCurrent();
                }
            }
        }
    }

    void commitCurrent() {
        synchronized (sLock) {
            System.out.println("Prepare to inset-" + finalToCommit.size() + " mentions to SQL");
            try {
                if (!this.sqlApi.insertRowsToTable(finalToCommit)) {
                    System.out.println("Failed to insert mentions Batch!!!!");
                }

                finalToCommit.clear();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}