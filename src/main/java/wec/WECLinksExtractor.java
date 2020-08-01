package wec;

import data.RawElasticResult;
import data.WECMention;
import data.WikiNewsMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import utils.StanfordNlpApi;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WECLinksExtractor {

    private static final String LINK_REGEX_2 = "\\[\\[([^\\[]*?)\\|?([^\\|]*?)\\]\\]";
    private static final Pattern LINK_PATTERN_2 = Pattern.compile(LINK_REGEX_2);

    public static List<WikiNewsMention> extractFromWikiNews(String pageName, String text) {
        List<WikiNewsMention> finalResults = new ArrayList<>();
        text = cleanTextField(text);
        String[] textLines = text.split("\\.\n\n");
        for (String context : textLines) {
            final List<WECMention> WECMentions = extractTextBodyMentions(pageName, context);
            for (WECMention WECMention : WECMentions) {
                finalResults.add(new WikiNewsMention(WECMention));
            }
        }

        for (WikiNewsMention mention : finalResults) {
            String corefValue = mention.getCorefChain().getCorefValue();
            if (corefValue.contains("w:")) {
                corefValue = corefValue.replace("w:", "").trim();
                mention.getCorefChain().setCorefValue(corefValue);
            }
        }

        return finalResults;
    }

    public static List<WECMention> extractFromWikipedia(RawElasticResult rowResult) {
        List<WECMention> finalResults = new ArrayList<>();

        String pageName = rowResult.getTitle();
        String text = rowResult.getText();

        if(pageName.toLowerCase().startsWith("file:") ||
                pageName.toLowerCase().startsWith("wikipedia:") || pageName.toLowerCase().startsWith("category:") ||
                pageName.toLowerCase().contains("list of") || pageName.toLowerCase().contains("lists of") ||
                pageName.toLowerCase().contains("listings")) {
            return finalResults;
        }

        if (text.toLowerCase().contains("[[category:opinion polling") || text.toLowerCase().contains("[[category:years in") ||
                text.toLowerCase().contains("[[category:lists of") || text.toLowerCase().contains("[[category:list of")) {
            return finalResults;
        }

        String textClean = cleanTextField(text);

        String relText;
        int firstSentenceStartIndex = textClean.indexOf("'''");
        if (firstSentenceStartIndex >= 0) {
            relText = textClean.substring(firstSentenceStartIndex);
            String[] textLines = relText.split("\\.\n\n");
            for (String paragraph : textLines) {
                finalResults.addAll(extractTextBodyMentions(pageName, paragraph));
            }
        }

        return finalResults;
    }

    private static List<WECMention> extractTextBodyMentions(String pageName, String paragraph) {
        String[] paragraphLines = paragraph.split("\n");
        for (int i = 0; i < paragraphLines.length; i++) {
            if (paragraphLines[i] != null && !paragraphLines[i].isEmpty() && isValidLine(paragraphLines[i])) {
                paragraphLines[i] = paragraphLines[i]
                        .replaceAll("\\*.*?\n", "")
                        .replaceAll("\n", " ")
                        .replaceAll("\\<.*?>", "")
                        .replaceAll("\\s+", " ").trim();
            } else {
                paragraphLines[i] = "";
            }
        }

        String fixedParagraph = String.join("\n", paragraphLines);
        return extractFromParagraph(pageName, fixedParagraph);
    }

    private static String cleanTextField(String text) {
        text = text.replaceAll("==.*?==\n", "\n");
        Pattern pat1 = Pattern.compile("(?s)\\{\\{[^{]*?\\}\\}");
        Matcher match1 = pat1.matcher(text);
        while (match1.find()) {
            text = match1.replaceAll("");
            match1 = pat1.matcher(text);
        }
        text = text.replaceAll("<ref[\\s\\S][^<]*?/>", "");
        text = text.replaceAll("(?s)<ref[\\s\\S]*?</ref>", "");
        text = text.replaceAll("(?s)\\{\\|\\s?class=\\\"?wikitable.*?\n\\|\\}", "");
        text = text.replaceAll("(?s)<gallery.*?</gallery>", "");
        text = text.replaceAll("(?s)<timeline.*?</timeline>", "");
        text = text.replaceAll("\\[\\[([cC]ategory|[fF]ile|[iI]mage).*", "");
        text = text.replaceAll("\\*.*?\n", "\n");
        return text;
    }

    public static String extractPageInfoBox(String pageText) {
        return extractPageInfoBox(pageText, false);
    }

    public static String extractPageInfoBox(String pageText, boolean toLowerForm) {
//        String text = pageText.toLowerCase().replaceAll(" ", "");
        StringBuilder infoBoxFinal = new StringBuilder();

        final int beginIndex = pageText.indexOf("{{Infobox");
        if (beginIndex != -1) {
            final String infoboxSubstring = pageText.substring(beginIndex);
            int infoBarCount = 0;
            for (int i = 0; i < infoboxSubstring.length(); i++) {
                final char c = infoboxSubstring.charAt(i);
                if (c == '}') {
                    infoBarCount--;
                    if (infoBarCount == 0) {
                        infoBoxFinal.append(c);
                        break;
                    }
                } else if (c == '{') {
                    infoBarCount++;
                }

                infoBoxFinal.append(c);
            }
        }

        if(toLowerForm) {
            return infoBoxFinal.toString().toLowerCase().replaceAll(" ", "");
        }

        return infoBoxFinal.toString();
    }

    static List<WECMention> extractFromParagraph(String pageName, String paragraphToExtractFrom) {
        List<WECMention> mentions = new ArrayList<>();

        Matcher linkMatcher = LINK_PATTERN_2.matcher(paragraphToExtractFrom);
        while (linkMatcher.find()) {
            String match1 = linkMatcher.group(1);
            String match2 = linkMatcher.group(2);
            if (!match1.contains("#")) {
                WECMention mention = new WECMention(pageName);
                if (!match1.isEmpty()) {
                    mention.setMentionText(match2);
                    mention.setCorefChain(match1);
                } else {
                    mention.setMentionText(match2);
                    mention.setCorefChain(match2);
                }

                mentions.add(mention);
            }
        }

        String context = linkMatcher
                .replaceAll("$2")
                .replaceAll("\\s+", " ").trim();
        if (context.matches("'''(.*?)'''(.*?)")) {
            context = context.replaceAll("'''(.*?)'''(.*?)", "$1");
        }

        setMentionsContext(mentions, context);
        return mentions;
    }

    private static <T extends WECMention> void setMentionsContext(List<T> mentions, String context) {
        final List<List<Map.Entry<String, Integer>>> contextAsStringList = new ArrayList<>();
        CoreDocument doc = StanfordNlpApi.noPosAnnotate(context);
        int runningId = 0;
        if (doc.sentences().size() > 0) {
            for (CoreSentence sentence : doc.sentences()) {
                List<Map.Entry<String, Integer>> sentenceTokens = new ArrayList<>();
                final List<CoreLabel> tokens = sentence.tokens();
                for (CoreLabel token : tokens) {
                    if (token.originalText().matches("[|\\[\\]\\*^\\+]")) {
                        continue;
                    }
                    sentenceTokens.add(new AbstractMap.SimpleEntry<>(token.originalText(), runningId));
                    runningId++;
                }

                contextAsStringList.add(sentenceTokens);
            }

            Set<Integer> usedStartIndexes = new HashSet<>();
            Iterator<T> iterator = mentions.iterator();
            while (iterator.hasNext()) {
                final T mention = iterator.next();
                mention.setContext(contextAsStringList);
                CoreDocument mentionCoreDoc = StanfordNlpApi.withPosAnnotate(mention.getMentionText());
                if (mentionCoreDoc.sentences().size() > 0) {
                    final List<CoreLabel> mentTokens = mentionCoreDoc.sentences().get(0).tokens();
                    for (CoreLabel label : mentTokens) {
                        mention.addMentionToken(label.originalText(), label.get(CoreAnnotations.PartOfSpeechAnnotation.class));
                    }

                    setMentionStartEndTokenIndex(mention, usedStartIndexes, mentTokens);

                    if (!mention.isValid()) {
                        iterator.remove();
                    }

                } else {
                    iterator.remove();
                }
            }
        } else {
            mentions.clear();
        }
    }

    private static <T extends WECMention> void setMentionStartEndTokenIndex(T mention, Set<Integer> usedStartIndexes, List<CoreLabel> mentTokens) {
        int index = 0;
        for(int i = 0 ; i < mention.getContext().size() ; i++) {
            List<Map.Entry<String, Integer>> sentenceToken = mention.getContext().get(i);
            for (int j = 0; j < sentenceToken.size(); j++) {
                if (!usedStartIndexes.contains(index)) {
                    if (sentenceToken.get(j).getKey().equals(mentTokens.get(0).originalText())) {
                        mention.setTokenStart(sentenceToken.get(j).getValue());
                        usedStartIndexes.add(index);
                        if (mentTokens.size() == 1) {
                            mention.setTokenEnd(index);
                            break;
                        }
                    } else if (mention.getTokenStart() != -1 && sentenceToken.get(j).getKey()
                            .equals(mentTokens.get(mentTokens.size() - 1).originalText())) {
                        mention.setTokenEnd(sentenceToken.get(j).getValue());
                        return;
                    }
                }
                index++;
            }
        }
    }

    static boolean isValidLine(String line) {
        line = line.toLowerCase();
        return !(line.startsWith("|") || line.startsWith("*") || line.startsWith("=") || line.startsWith("#") ||
                line.startsWith(";") || line.startsWith(":"));
    }
}
