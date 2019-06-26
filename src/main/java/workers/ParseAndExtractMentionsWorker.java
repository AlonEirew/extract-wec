package workers;

import data.RawElasticResult;
import data.WikiLinksMention;
import wikilinks.WikiLinksExtractor;

import java.util.List;

class ParseAndExtractMentionsWorker extends AWorker {

    private ParseListener listener;

    public ParseAndExtractMentionsWorker(List<RawElasticResult> rawElasticResults, ParseListener listener) {
        super(rawElasticResults);
        this.listener = listener;
    }

    @Override
    public void run() {
        for(RawElasticResult rowResult : this.rawElasticResults) {
            List<WikiLinksMention> wikiLinksMentions = WikiLinksExtractor.extractFromFile(rowResult.getTitle(), rowResult.getText());
            wikiLinksMentions.stream().forEach(wikiLinksMention -> wikiLinksMention.getCorefChain().incMentionsCount());
            this.listener.handle(wikiLinksMentions);
        }
    }
}