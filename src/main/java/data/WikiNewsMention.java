package data;

public class WikiNewsMention extends WikiLinksMention {
    private static final String TABLE_NEWS_MENTIONS = "News_Mentions";

    @Override
    public String getTableName() {
        return TABLE_NEWS_MENTIONS;
    }
}
