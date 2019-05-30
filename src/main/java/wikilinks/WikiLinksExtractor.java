package wikilinks;

import data.WikiLinksMention;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLinksExtractor {

    private static final int MAX_EMPLOYEES = 1000;

    private static final String LINK_REGEX_2 = "\\[\\[([^\\[]*?)\\|?([^\\|]*?)\\]\\]";
    private static final Pattern LINK_PATTERN_2 = Pattern.compile(LINK_REGEX_2);
    private static final Pattern SPORT_PATTERN = Pattern.compile(
            "\\{\\{infobox[\\w\\|]*(match|draft|racereport|championships|athleticscompetition)");
    private static final Pattern AWARD_PATTERN = Pattern.compile("\\{\\{infobox[\\w\\|]*(award)");

    private static StanfordCoreNLP pipeline;

    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit");
        pipeline = new StanfordCoreNLP(props);
    }

    public static List<WikiLinksMention> extractFromFile(String pageName, String text) {
        List<WikiLinksMention> finalResults = new ArrayList<>();

        text = text.replaceAll("==.*?==\n", "\n");
        text = text.replaceAll("\\{\\{.*?\\}\\}", "");
        text = text.replaceAll("<ref[\\s\\S]*?/>", "");
        text = text.replaceAll("(?s)\\{\\|\\s?class=\\\"?wikitable.*?\n\\|\\}", "");
        text = text.replaceAll("(?s)\\{\\{\\s?notelist.*?\\}\\}\n\\}\\}", "");
        text = text.replaceAll("(?s)<gallery.*?</gallery>", "");
        text = text.replaceAll("(?s)<timeline.*?</timeline>", "");
        text = text.replaceAll("(?s)\\{\\{\\s?(bar\\sbox|reflist|[Ss]uccession\\sbox|[Ii]nfobox|s-bef|s-ttl|s-aft|columns-list)+.*?\n\\}\\}", "");
        text = text.replaceAll("(?s)\\{\\{\\s?.*box\\|.*?\\}\\}\n\\}\\}", "");
        text = text.replaceAll("(?s)<ref[\\s\\S]*?</ref>", "");

        String relText = "";
        int firstSentenceStartIndex = text.indexOf("'''");
        if(firstSentenceStartIndex >= 0) {
            relText = text.substring(firstSentenceStartIndex);
            String[] textLines = relText.split("\\.\n\n");
            for (String context : textLines) {
                String[] contextLines = context.split("\n");
                for (int i = 0 ; i < contextLines.length ; i++) {
                    if(contextLines[i] != null && !contextLines[i].isEmpty() && isValidLine(contextLines[i])) {
                        contextLines[i] = contextLines[i]
                                .replaceAll("\\*.*?\n", "")
                                .replaceAll("\n", " ")
                                .replaceAll("\\<.*?>", "")
                                .replaceAll("\\s+", " ").trim();
                    } else {
                        contextLines[i] = "";
                    }
                }

                String fixedContext = String.join("\n", contextLines);
                finalResults.addAll(extractFromLine(pageName, fixedContext));
            }
        }

        return finalResults;
    }

    public static String extractPageInfoBox(String pageText) {
        String text = pageText.toLowerCase().replaceAll(" ", "");

        Pattern patternStart = Pattern.compile("\\{\\{infobox");
        Matcher matcherStart = patternStart.matcher(text);

        Pattern patternEnd = Pattern.compile("'''|footnotes=");

        String infoBox = null;
        // Check all occurrences
        if (matcherStart.find()) {
            final int infoStart = matcherStart.start();
            Matcher matcherEnd = patternEnd.matcher(text.substring(infoStart));
            if (matcherEnd.find()) {
                final int infoEnd = matcherEnd.end() + infoStart;
                if (infoStart < infoEnd && infoStart != -1 && infoEnd != -1) {
                    infoBox = text.substring(infoStart, infoEnd);
                }
            }
        }

        return infoBox;
    }

    public static Set<String> extractTypes(String text) {
        Set<String> finalResults = new HashSet<>();
        String relText = "";
        int firstSentenceStartIndex = text.indexOf("|type");
        if(firstSentenceStartIndex < 0) {
            firstSentenceStartIndex = text.indexOf("| type");
        }

        if(firstSentenceStartIndex >= 0) {
            relText = text.substring(firstSentenceStartIndex);
            final int endIndex = relText.indexOf("\n");
            if(endIndex != -1) {
                relText = relText.substring(0, endIndex);
            }

            Matcher linkMatcher = LINK_PATTERN_2.matcher(relText);
            while (linkMatcher.find()) {
                String match1 = linkMatcher.group(1);
                String match2 = linkMatcher.group(2);
                if (!match1.contains("#")) {
                    if (!match1.isEmpty()) {
                        finalResults.add(match1.toLowerCase());
                    }

                    finalResults.add(match2.toLowerCase());
                }
            }
        }

        return finalResults;
    }

    public static boolean isPerson(String text) {
        return text.contains("birth_name") || text.contains("birth_date") ||
                text.contains("birth_place");
    }

    public static boolean isElection(String infoBox) {
        if(infoBox.contains("\n|election_name=") && infoBox.contains("\n|election_date=")) {
            return true;
        }

        return false;
    }

    public static boolean isCivilAttack(String infoBox) {
        if(infoBox.contains("{{infoboxcivilianattack") || infoBox.contains("{{infoboxterroristattack")
            || infoBox.contains("{{infoboxmilitaryattack")) {
            return true;
        }

        return false;
    }

    public static boolean isAccident(String infoBox) {
        if(infoBox.contains("{{infoboxaircraftoccurrence") || infoBox.contains("{{infoboxrailaccident")) {
            return true;
        }

        return false;
    }

    public static boolean isDisaster(String infoBox) {
        if(infoBox.contains("{{infoboxearthquake") || infoBox.contains("{{infoboxhurricane")
                || infoBox.contains("{{infoboxwildfire")) {
            return true;
        }

        return false;
    }

    public static boolean isGeneralEvent(String infoBox) {
        if(infoBox.contains("{{infoboxevent")) {
            return true;
        }

        return false;
    }

    public static boolean isHistoricalEvent(String infoBox) {
        if(infoBox.contains("{{infoboxhistoricalevent")) {
            return true;
        }

        return false;
    }

    public static boolean isNewsEvent(String infoBox) {
        if(infoBox.contains("{{infoboxnewsevent")) {
            return true;
        }

        return false;
    }

    public static boolean isSmallCompanyEvent(String infoBox) {
        if(infoBox.contains("{{infoboxcompany")) {
            for (String line : infoBox.split("\n")) {
                if(line.startsWith("|num_employees=")) {
                    final String[] numEmpSplit = line.split("=");
                    if(numEmpSplit.length == 2) {
                        try {
                            int empAmount = Integer.parseInt(numEmpSplit[1]);
                            if (empAmount <= MAX_EMPLOYEES) {
                                return true;
                            }
                        } catch (NumberFormatException e) { }
                    }
                }
            }
        }

        return false;
    }

    public static boolean isSportEvent(String infoBox) {
        Matcher linkMatcher = SPORT_PATTERN.matcher(infoBox);
        if (linkMatcher.find()) {
            return true;
        }

        return false;
    }

    public static boolean isAwardEvent(String infoBox, String title) {
        Pattern titlePattern = Pattern.compile("(.*\\s?\\d\\d?th\\s.*|.*[12][90][0-9][0-9].*)");

        Matcher titleMatcher = titlePattern.matcher(title);
        Matcher awardMatcher = AWARD_PATTERN.matcher(infoBox);
        if (awardMatcher.find() && titleMatcher.find()) {
            return true;
        }

        return false;
    }

    public static boolean hasDateAndLocation(String infoBox) {
        if(infoBox != null) {
            String dateLine = null;
            String locationLine = null;
            for (String line : infoBox.split("\n")) {
                if (line.startsWith("|date=")) {
                    final String[] split = line.split("=");
                    if (split.length > 1) {
                        dateLine = split[1].trim();
                        if (dateLine.split("-").length != 1) {
                            dateLine = null;
                        }
                    }
                } else if (line.startsWith("|location=")) {
                    final String[] split = line.split("=");
                    if (split.length > 1) {
                        locationLine = split[1].trim();
                    }
                }
            }

            return locationLine != null && !locationLine.isEmpty()
                    && dateLine != null && !dateLine.isEmpty()
                    && !dateLine.contains("{{startandenddates")
                    && !dateLine.contains("startdate|yyyy|mm|dd");
        }

        return false;
    }

    static List<WikiLinksMention>  extractFromLine(String pageName, String lineToExtractFrom) {
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

                mentions.add(mention);
            }
        }

        String context = linkMatcher
                .replaceAll("$2")
                .replaceAll("\\s+", " ").trim();
        if(context.matches("'''(.*?)'''(.*?)")) {
            context = context.replaceAll("'''(.*?)'''(.*?)", "$1");
        }

        setMentionsContext(mentions, context);
        return mentions;
    }

    private static void setMentionsContext(List<WikiLinksMention> mentions, String context) {
        final List<String> mentContext = new ArrayList<>();
        CoreDocument doc = new CoreDocument(context);
        pipeline.annotate(doc);
        if(doc.sentences().size() > 0) {
            for(CoreSentence sentence : doc.sentences()) {
                final List<CoreLabel> tokens = sentence.tokens();
                for (CoreLabel token : tokens) {
                    if (token.originalText().matches("[|\\[\\]\\*^\\+]")) {
                        continue;
                    }
                    mentContext.add(token.originalText());
                }
            }

            Set<Integer> usedStartIndexes = new HashSet<>();
            Iterator<WikiLinksMention> iterator = mentions.iterator();
            while (iterator.hasNext()) {
                final WikiLinksMention mention = iterator.next();
                mention.setContext(mentContext);
                CoreDocument mentionCoreDoc = new CoreDocument(mention.getMentionText());
                pipeline.annotate(mentionCoreDoc);
                if (mentionCoreDoc.sentences().size() > 0) {
                    final List<CoreLabel> mentTokens = mentionCoreDoc.sentences().get(0).tokens();
                    for(CoreLabel label : mentTokens) {
                        mention.addMentionToken(label.originalText());
                    }

                    for (int i = 0; i < mentContext.size(); i++) {
                        if(!usedStartIndexes.contains(i)) {
                            if (mentContext.get(i).equals(mentTokens.get(0).originalText())) {
                                mention.setTokenStart(i);
                                usedStartIndexes.add(i);
                                if (mentTokens.size() == 1) {
                                    mention.setTokenEnd(i);
                                    break;
                                }
                            } else if (mention.getTokenStart() != -1 && mentContext.get(i).equals(mentTokens.get(mentTokens.size() - 1).originalText())) {
                                mention.setTokenEnd(i);
                                break;
                            }
                        }
                    }

                    if(!mention.isValid()) {
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

    static boolean isValidLine(String line) {
        line = line.toLowerCase();
        return !(line.startsWith("{{") || line.startsWith("}}") || line.startsWith("|") ||
                line.startsWith("*") || line.startsWith("=") || line.startsWith("#") ||
                line.startsWith(";") || line.startsWith(":") || line.startsWith("[[file") ||
                line.startsWith("[[category") || line.startsWith("[[image") ||
                (line.startsWith("'''") && line.endsWith("'''")));
    }
}
