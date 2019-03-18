package wikilinks;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WikiLinksMention {
    private String mentionText = "";
    private int tokenStart = 0;
    private int tokenEnd = 0;
    private String corefChain = "";
    private String extractedFromPage = "";
    private List<String> context;

    public WikiLinksMention(String extractedFromPage) {
        this.extractedFromPage = extractedFromPage;
    }

    public String getMentionText() {
        return mentionText;
    }

    public void setMentionText(String mentionText) {
        this.mentionText = mentionText;
    }

    public int getTokenStart() {
        return tokenStart;
    }

    public void setTokenStart(int tokenStart) {
        this.tokenStart = tokenStart;
    }

    public int getTokenEnd() {
        return tokenEnd;
    }

    public void setTokenEnd(int tokenEnd) {
        this.tokenEnd = tokenEnd;
    }

    public String getCorefChain() {
        return corefChain;
    }

    public void setCorefChain(String corefChain) {
        this.corefChain = corefChain;
    }

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public String getExtractedFromPage() {
        return extractedFromPage;
    }

    public void setExtractedFromPage(String extractedFromPage) {
        this.extractedFromPage = extractedFromPage;
    }

    public boolean isValid() {
        if(this.corefChain.isEmpty() ||
            this.mentionText.length() <= 1 || this.corefChain.toLowerCase().startsWith("file:") ||
                this.corefChain.toLowerCase().startsWith("wikipedia:")) {
            return false;
        }

        return true;
    }
}
