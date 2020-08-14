package workers;

import data.RawElasticResult;
import data.WECCoref;
import data.WikiNewsMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLQueryApi;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class WikiNewsWorker extends AWorker {
    private final static Logger LOGGER = LogManager.getLogger(WikiNewsWorker.class);
    private final static  int COMMIT_MAX_SIZE = 1000000;
    private final static  List<WikiNewsMention> finalToCommit = new ArrayList<>();

    private final SQLQueryApi sqlApi;
    private final Map<String, WECCoref> wikiLinksCorefMap;
    private final ReentrantLock sLock = new ReentrantLock();

    public WikiNewsWorker(List<RawElasticResult> rawElasticResults, SQLQueryApi sqlApi,
                          Map<String, WECCoref> corefMap) {
        super(rawElasticResults);
        this.sqlApi = sqlApi;
        this.wikiLinksCorefMap = corefMap;
    }

    @Override
    public void run() {
//        List<WikiNewsMention> mentions = new ArrayList<>();
//        for(RawElasticResult rowResult : this.rawElasticResults) {
//            List<WikiNewsMention> wikiLinksMentions = WECLinksExtractor.extractFromWikiNews(rowResult.getTitle(), rowResult.getText());
//            wikiLinksMentions.forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
//            mentions.addAll(wikiLinksMentions);
//        }
//
//        if(mentions.size() > 0) {
//            final Iterator<WikiNewsMention> menIterator = mentions.iterator();
//            while(menIterator.hasNext()) {
//                WikiNewsMention mention = menIterator.next();
//                final String corefValue = mention.getCorefChain().getCorefValue();
//                if (!wikiLinksCorefMap.containsKey(corefValue)) {
//                    menIterator.remove();
//                } else {
//                    mention.setCorefChain(wikiLinksCorefMap.get(corefValue));
//                }
//            }
//        }
//
//        handle(mentions, false);
    }

    public void handle(List<WikiNewsMention> mentions, boolean forceCommit) {

        sLock.lock();
        finalToCommit.addAll(mentions);
        if(finalToCommit.size() >= COMMIT_MAX_SIZE || forceCommit) {
            List<WikiNewsMention> localNewList = new ArrayList<>(finalToCommit);
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
        LOGGER.info("Prepare to inset-" + localNewList.size() + " mentions to SQL");
        try {
            if (!this.sqlApi.insertRowsToTable(localNewList)) {
                LOGGER.error("Failed to insert mentions Batch!!!!");
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
    }
}
