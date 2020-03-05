package data;

public class WikiNewsMention extends WECMention {
    private static final String TABLE_NEWS_MENTIONS = "News_Mentions";

    public WikiNewsMention() {}

    public WikiNewsMention(WECMention WECMention) {
        super(WECMention.getCorefChain(), WECMention.getMentionText(), WECMention.getTokenStart(),
                WECMention.getTokenEnd(), WECMention.getExtractedFromPage(), WECMention.getContext());
    }

    @Override
    public String getTableName() {
        return TABLE_NEWS_MENTIONS;
    }
}
