package wikilinks;

import data.WikiLinksMention;
import data.WikiNewsMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import workers.ReadDateWorker;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLinksExtractor {

    private static final int MAX_EMPLOYEES = 1000;

    private static final String LINK_REGEX_2 = "\\[\\[([^\\[]*?)\\|?([^\\|]*?)\\]\\]";
    private static final Pattern LINK_PATTERN_2 = Pattern.compile(LINK_REGEX_2);
    private static final Pattern SPORT_PATTERN = Pattern.compile(
            "\\{\\{infobox[\\w\\|]*?(halftimeshow|match|draft|racereport|indy500|championships|athleticscompetition|sailingcompetition" +
                    "|competitionevent|paralympicevent|tennisevent|grandslamevent|tournamentevent|swimmingevent|all-stargame|grandprixevent|wrestlingevent" +
                    "|cupfinal|ncaabasketballsinglegame|baseballgame|nflgame|nflchamp|aflchamp|basketballgame|race|sportevent" +
                    "|singlegame|hockeygame|yearlygame|outdoorgame|frcgame|olympicevent|mmaevent|daytona500|gamesevent|swcevent)");

    private static final Pattern AWARD_PATTERN = Pattern.compile("\\{\\{infobox[\\w\\|]*?(award|awards|contest|beautypageant)");

    private static final Pattern ELECTION_PATTERN = Pattern.compile("\\{\\{infobox[\\w\\|]*?(election)");

    private static final Pattern CONCRETE_EVENT = Pattern.compile("\\{\\{infobox[\\w\\|]*?(solareclipse|newsevent|concert|weaponstest|explosivetest" +
            "|summit|convention|conference|summitmeeting|festival)");

    private static String[] MONTHS = {"january", "february", "march", "april", "may", "june", "july", "august",
            "september", "october", "november", "december"};

    private static StanfordCoreNLP pipelineWithPos;
    private static StanfordCoreNLP pipelineNoPos;

    static {
        Properties props1 = new Properties();
        props1.setProperty("annotators", "tokenize, ssplit, pos");
        pipelineWithPos = new StanfordCoreNLP(props1);

        Properties props2 = new Properties();
        props2.setProperty("annotators", "tokenize, ssplit");
        pipelineNoPos = new StanfordCoreNLP(props2);
    }

    public static List<WikiNewsMention> extractFromWikiNews(String pageName, String text) {
        List<WikiNewsMention> finalResults = new ArrayList<>();
        text = cleanTextField(text);
        String[] textLines = text.split("\\.\n\n");
        for (String context : textLines) {
            final List<WikiLinksMention> wikiLinksMentions = extractTextBodyMentions(pageName, context);
            for (WikiLinksMention wikiLinksMention : wikiLinksMentions) {
                finalResults.add(new WikiNewsMention(wikiLinksMention));
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

    public static List<WikiLinksMention> extractFromWikipedia(String pageName, String text) {
        List<WikiLinksMention> finalResults = new ArrayList<>();

        if (text.toLowerCase().contains("[[category:opinion polling") || text.toLowerCase().contains("[[category:years in") ||
                text.toLowerCase().contains("[[category:lists of") || text.toLowerCase().contains("[[category:list of")) {
            text = "";
        }

        String textClean = cleanTextField(text);

        String relText = "";
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

    private static List<WikiLinksMention> extractTextBodyMentions(String pageName, String paragraph) {
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
        String text = pageText.toLowerCase().replaceAll(" ", "");
        StringBuilder infoBoxFinal = new StringBuilder();

        final int beginIndex = text.indexOf("{{infobox");
        if (beginIndex != -1) {
            final String infoboxSubstring = text.substring(beginIndex);
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

        return infoBoxFinal.toString();
    }

    public static Set<String> extractTypes(String text) {
        Set<String> finalResults = new HashSet<>();
        String relText = "";
        int firstSentenceStartIndex = text.indexOf("|type");
        if (firstSentenceStartIndex < 0) {
            firstSentenceStartIndex = text.indexOf("| type");
        }

        if (firstSentenceStartIndex >= 0) {
            relText = text.substring(firstSentenceStartIndex);
            final int endIndex = relText.indexOf("\n");
            if (endIndex != -1) {
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

    public static boolean isElection(String infoBox, String title) {
        boolean titleMatch = titleNumberMatch(title);
        Matcher linkMatcher = ELECTION_PATTERN.matcher(infoBox);
        if (linkMatcher.find() && titleMatch) {
            return true;
        }

        return false;
    }

    public static boolean isCivilAttack(String infoBox) {
        final Set<String> uniqueDates = getYears(infoBox);

        if (infoBox.contains("{{infoboxcivilianattack") || infoBox.contains("{{infoboxterroristattack")
                || infoBox.contains("{{infoboxmilitaryattack") || infoBox.contains("{{infoboxcivilconflict")
                || infoBox.contains("{{infoboxmilitaryconflict")) {
            if (uniqueDates != null && uniqueDates.size() < 2) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAccident(String infoBox) {
        if (infoBox.contains("{{infoboxaircraftoccurrence") || infoBox.contains("{{infoboxairlinerincident") ||
                infoBox.contains("{{infoboxrailaccident") || infoBox.contains("{{infoboxairlineraccident") ||
                infoBox.contains("{{infoboxbusaccident") || infoBox.contains("{{infoboxaircraftcrash")
                || infoBox.contains("{{infoboxaircraftaccident") || infoBox.contains("{{infoboxaircraftincident")) {
            return true;
        }

        return false;
    }

    public static boolean isDisaster(String infoBox) {
        if (infoBox.contains("{{infoboxearthquake") || infoBox.contains("{{infoboxhurricane")
                || infoBox.contains("{{infoboxwildfire") || infoBox.contains("infoboxstorm/sandbox") ||
                infoBox.contains("infoboxflood") || infoBox.contains("infoboxoilspill")
                || infoBox.contains("infoboxstorm") || infoBox.contains("infoboxeruption")) {
            return true;
        }

        return false;
    }

    public static boolean isGeneralEvent(String infoBox) {
        if (infoBox.contains("{{infoboxevent")) {
            return true;
        }

        return false;
    }

    public static boolean isHistoricalEvent(String infoBox) {
        if (infoBox.contains("{{infoboxhistoricalevent")) {
            return true;
        }

        return false;
    }

    public static boolean isConcreteGeneralEvent(String infoBox, String title) {

        Matcher concreteMatcher = CONCRETE_EVENT.matcher(infoBox);
        boolean titleMatch = titleNumberMatch(title);
        final Set<String> years = getYears(infoBox);
        if (concreteMatcher.find() && (titleMatch || (years != null && years.size() < 2))) {
            return true;
        }

        return false;
    }

    public static boolean isSmallCompanyEvent(String infoBox) {
        if (infoBox.contains("{{infoboxcompany")) {
            for (String line : infoBox.split("\n")) {
                if (line.startsWith("|num_employees=")) {
                    final String[] numEmpSplit = line.split("=");
                    if (numEmpSplit.length == 2) {
                        try {
                            int empAmount = Integer.parseInt(numEmpSplit[1]);
                            if (empAmount <= MAX_EMPLOYEES) {
                                return true;
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        }

        return false;
    }

    public static boolean isSportEvent(String infoBox, String title) {
        Matcher linkMatcher = SPORT_PATTERN.matcher(infoBox);
        boolean titleMatch = titleNumberMatch(title);
        if (linkMatcher.find() && titleMatch) {
            return true;
        }

        return false;
    }

    public static boolean isAwardEvent(String infoBox, String title) {
        boolean titleMatch = titleNumberMatch(title);
        Matcher awardMatcher = AWARD_PATTERN.matcher(infoBox);
        if (awardMatcher.find() && titleMatch) {
            return true;
        }

        return false;
    }

    public static boolean hasDateAndLocation(String infoBox) {
        if (infoBox != null) {
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

    static List<WikiLinksMention> extractFromParagraph(String pageName, String paragraphToExtractFrom) {
        List<WikiLinksMention> mentions = new ArrayList<>();

        Matcher linkMatcher = LINK_PATTERN_2.matcher(paragraphToExtractFrom);
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

//        String context = linkMatcher
//                .replaceAll("$2")
//                .replaceAll("\\s+", " ").trim();
//        if (context.matches("'''(.*?)'''(.*?)")) {
//            context = context.replaceAll("'''(.*?)'''(.*?)", "$1");
//        }

        setMentionsContext(mentions, paragraphToExtractFrom);
        return mentions;
    }

    private static boolean titleNumberMatch(String title) {
        Pattern titlePattern = Pattern.compile("\\s?\\d\\d?th\\s|[12][90][0-9][0-9]|\\b[MDCLXVI]+\\b");
        Matcher titleMatcher = titlePattern.matcher(title);
        return titleMatcher.find();
    }

    private static Set<String> getYears(String infoBox) {
        Set<String> uniqueDates = null;
        Pattern yearPattern = Pattern.compile("[12][0-9][0-9][0-9]|[0-9][0-9][0-9]");

        Pattern dateEql = Pattern.compile("\\n\\|date=(.*)\n");
        Matcher matcher = dateEql.matcher(infoBox);

        if (matcher.find()) {
            String dateLine = matcher.group(1);
            if (!dateLine.isEmpty()) {
                uniqueDates = new HashSet<>();
            }

            Matcher yearMatch = yearPattern.matcher(dateLine);
            while (yearMatch.find()) {
                uniqueDates.add(yearMatch.group());
            }

            if (dateLine.contains("plainlist")) {
                uniqueDates.add("rej1");
                uniqueDates.add("rej2");
            }
        }

        return uniqueDates;
    }

    public static boolean isDaySpan(String infobox) {
        ReadDateWorker rdw = new ReadDateWorker();
        Date date = rdw.extractDate(infobox);
        if(date == null) {
            return false;
        }

        return true;
    }

    private static <T extends WikiLinksMention> void setMentionsContext(List<T> mentions, String context) {
        final List<List<Map.Entry<String, Integer>>> contextAsStringList = new ArrayList<>();
        CoreDocument doc = new CoreDocument(context);
        pipelineNoPos.annotate(doc);
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
                CoreDocument mentionCoreDoc = new CoreDocument(mention.getMentionText());
                pipelineWithPos.annotate(mentionCoreDoc);
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

    private static <T extends WikiLinksMention> void setMentionStartEndTokenIndex(T mention, Set<Integer> usedStartIndexes, List<CoreLabel> mentTokens) {
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
