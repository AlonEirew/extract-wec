package workers;

import data.RawElasticResult;
import data.WikiLinksMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ElasticQueryApi;
import persistence.SQLQueryApi;
import wikilinks.ICorefFilter;
import wikilinks.WikiLinksExtractor;
import wikinews.WikiNewsToWikiLinksMain;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

class ParseAndExtractMentionsWorker extends AWorker {
    private final static Logger LOGGER = LogManager.getLogger(ParseAndExtractMentionsWorker.class);

    private static final ReentrantLock sLock = new ReentrantLock();
    private static final int COMMIT_MAX_SIZE = 1000000;

    private static final List<WikiLinksMention> finalToCommit = new ArrayList<>();

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
        List<WikiLinksMention> mentions = new ArrayList<>();
        for(RawElasticResult rowResult : this.rawElasticResults) {
            List<WikiLinksMention> wikiLinksMentions = WikiLinksExtractor.extractFromWikipedia(rowResult.getTitle(), rowResult.getText());
            wikiLinksMentions.stream().forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
            mentions.addAll(wikiLinksMentions);
        }

        this.handle(mentions, false);
    }

    public void handle(List<WikiLinksMention> mentions, boolean forceCommit) {
        sLock.lock();
        finalToCommit.addAll(mentions);
        if(finalToCommit.size() >= COMMIT_MAX_SIZE || forceCommit) {
            List<WikiLinksMention> localNewList = new ArrayList<>(finalToCommit);
            finalToCommit.clear();
            sLock.unlock();
            localNewList = extractFromWikiAndCleanNoneRelevant(localNewList);
            commitCurrent(localNewList);
        } else {
            sLock.unlock();
        }
    }

    private List<WikiLinksMention> extractFromWikiAndCleanNoneRelevant(List<WikiLinksMention> localNewList) {
        LOGGER.info("Handle all worker mentions...in total-" + localNewList.size() + " will be handled");
        Set<String> corefTitleSet = new HashSet<>();
        for(WikiLinksMention mention : localNewList) {
            corefTitleSet.add(mention.getCorefChain().getCorefValue());
        }

        Map<String, String> allPagesText = null;
        try {
            allPagesText = this.elasticApi.getAllWikiPagesTitleAndText(corefTitleSet);
        } catch (Exception e) {
            LOGGER.error(e);
        }

        if(allPagesText != null) {
            final Iterator<WikiLinksMention> iterator = localNewList.iterator();
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
        }

        return localNewList;
    }

    private void commitCurrent(List<WikiLinksMention> localNewList) {
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