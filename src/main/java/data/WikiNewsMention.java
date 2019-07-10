package data;

public class WikiNewsMention extends WikiLinksMention {
    private static final String TABLE_NEWS_MENTIONS = "News_Mentions";

    public WikiNewsMention() {}

    public WikiNewsMention(WikiLinksMention wikiLinksMention) {
        super(wikiLinksMention.getCorefChain(), wikiLinksMention.getMentionText(), wikiLinksMention.getTokenStart(),
                wikiLinksMention.getTokenEnd(), wikiLinksMention.getExtractedFromPage(), wikiLinksMention.getContext());
    }

    @Override
    public String getTableName() {
        return TABLE_NEWS_MENTIONS;
    }
}
