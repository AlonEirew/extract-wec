package wikilinks;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLinksExtractor {

    private static final String LINK_REGEX_2 = "\\[\\[([^\\[]*?)\\|?([^\\|]*?)\\]\\]";
    private static final Pattern LINK_PATTERN_2 = Pattern.compile(LINK_REGEX_2);

    private static StanfordCoreNLP pipeline;

    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit");
        pipeline = new StanfordCoreNLP(props);
    }

    public static List<WikiLinksMention> extractFromFile(String pageName, String text) {
        List<WikiLinksMention> finalResults = new ArrayList<>();

        String relText = "";
        int firstSentenceStartIndex = text.indexOf("'''");
        if(firstSentenceStartIndex >= 0) {
            relText = text.substring(firstSentenceStartIndex);
            String[] textLines = relText.split("\n");
            for (String line : textLines) {
                if(line != null && !line.isEmpty() && isValidLine(line)) {
                    line = line
                            .replaceAll("\\(.*?\\)", "")
                            .replaceAll("\\{\\{.*?\\}\\}", "")
                            .replaceAll("<ref[\\s\\S]*?</ref>", "")
                            .replaceAll("<ref[\\s\\S]*?/>", "")
                            .replaceAll("\\s+", " ").trim();

                    String[] splitLineSentences = line.split("\\.");
                    for (String sentence : splitLineSentences) {
                        if(!sentence.isEmpty()) {
                            finalResults.addAll(extractFromLine(pageName, sentence));
                        }
                    }
                }
            }
        }

        return finalResults;
    }

    static List<WikiLinksMention> extractFromLine(String pageName, String lineToExtractFrom) {
        List<WikiLinksMention> mentions = new ArrayList<>();

        Matcher linkMatcher = LINK_PATTERN_2.matcher(lineToExtractFrom);
        while (linkMatcher.find()) {
            String match1 = linkMatcher.group(1);
            String match2 = linkMatcher.group(2);
            if (!match1.contains("#")) {
                WikiLinksMention mention = new WikiLinksMention(pageName);
                if (!match1.isEmpty()) {
                    mention.setMentionText(match2);
                    mention.setCorefChain(match1);
                } else {
                    mention.setMentionText(match2);
                    mention.setCorefChain(match2);
                }

                if(mention.isValid()) {
                    mentions.add(mention);
                }
            }
        }

        String context = linkMatcher
                .replaceAll("$2")
                .replaceAll("\\s+", " ").trim();

        setMentionsContext(mentions, context);
        return mentions;
    }

    private static void setMentionsContext(List<WikiLinksMention> mentions, String context) {
        final List<String> mentContext = new ArrayList<>();
        CoreDocument doc = new CoreDocument(context);
        pipeline.annotate(doc);
        if(doc.sentences().size() > 0) {
            final List<CoreLabel> tokens = doc.sentences().get(0).tokens();
            for (CoreLabel token : tokens) {
                if(token.originalText().matches("[|\\[\\]\\*^\\+]")) {
                    continue;
                }
                mentContext.add(token.originalText());
            }

            Iterator<WikiLinksMention> iterator = mentions.iterator();
            while (iterator.hasNext()) {
                final WikiLinksMention mention = iterator.next();
                mention.setContext(mentContext);
                CoreDocument mentionCoreDoc = new CoreDocument(mention.getMentionText());
                pipeline.annotate(mentionCoreDoc);
                if (mentionCoreDoc.sentences().size() > 0) {
                    final List<CoreLabel> mentTokens = mentionCoreDoc.sentences().get(0).tokens();
                    for (int i = 0; i < mentContext.size(); i++) {
                        if (mentContext.get(i).equals(mentTokens.get(0).originalText())) {
                            mention.setTokenStart(i);
                            if (mentTokens.size() == 1) {
                                mention.setTokenEnd(i);
                                break;
                            }
                        } else if (mentContext.get(i).equals(mentTokens.get(mentTokens.size() - 1).originalText())) {
                            mention.setTokenEnd(i);
                            break;
                        }
                    }
                    if((mention.getTokenEnd() - mention.getTokenStart() + 1) != mentTokens.size()) {
                        iterator.remove();
                    }
                } else {
                    iterator.remove();
                }
            }
        }
    }

    static boolean isValidLine(String line) {
        return !(line.startsWith("{{") || line.startsWith("}}") || line.startsWith("|") || line.startsWith("*") || line.startsWith("===") ||
                line.startsWith("[[Category") || line.startsWith("[[category"));
    }
}
