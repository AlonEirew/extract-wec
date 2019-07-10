package workers;

import data.RawElasticResult;
import data.WikiLinksCoref;
import data.WikiNewsMention;
import persistence.SQLQueryApi;
import wikilinks.WikiLinksExtractor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class WikiNewsWorker extends AWorker {

    private static final int COMMIT_MAX_SIZE = 1000000;
    private static final List<WikiNewsMention> finalToCommit = new ArrayList<>();

    private final SQLQueryApi sqlApi;
    private final Map<String, WikiLinksCoref> wikiLinksCorefMap;
    private final ReentrantLock sLock = new ReentrantLock();

    public WikiNewsWorker(List<RawElasticResult> rawElasticResults, SQLQueryApi sqlApi,
                          Map<String, WikiLinksCoref> corefMap) {
        super(rawElasticResults);
        this.sqlApi = sqlApi;
        this.wikiLinksCorefMap = corefMap;
    }

    @Override
    public void run() {
        List<WikiNewsMention> mentions = new ArrayList<>();
        for(RawElasticResult rowResult : this.rawElasticResults) {
            List<WikiNewsMention> wikiLinksMentions = WikiLinksExtractor.extractFromWikiNews(rowResult.getTitle(), rowResult.getText());
            wikiLinksMentions.stream().forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
            mentions.addAll(wikiLinksMentions);
        }

        if(mentions.size() > 0) {
            final Iterator<WikiNewsMention> menIterator = mentions.iterator();
            while(menIterator.hasNext()) {
                WikiNewsMention mention = menIterator.next();
                final String corefValue = mention.getCorefChain().getCorefValue();
                if (!wikiLinksCorefMap.containsKey(corefValue)) {
                    menIterator.remove();
                } else {
                    mention.setCorefChain(wikiLinksCorefMap.get(corefValue));
                }
            }
        }

        handle(mentions, false);
    }

    public void handle(List<WikiNewsMention> mentions, boolean forceCommit) {

        sLock.lock();
        finalToCommit.addAll(mentions);
        if(finalToCommit.size() >= COMMIT_MAX_SIZE || forceCommit) {
            List<WikiNewsMention> localNewList = new ArrayList<>();
            localNewList.addAll(finalToCommit);
            finalToCommit.clear();

            sLock.unlock();

            commitCurrent(localNewList);
        } else {
            sLock.unlock();
        }
    }

    public static List<WikiNewsMention> getFinalToCommit() {
        return finalToCommit;
    }

    private void commitCurrent(List<WikiNewsMention> localNewList) {
        System.out.println("Prepare to inset-" + localNewList.size() + " mentions to SQL");
        try {
            if (!this.sqlApi.insertRowsToTable(localNewList)) {
                System.out.println("Failed to insert mentions Batch!!!!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
