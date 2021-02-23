package experimentscripts.event;

import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.gson.*;
import data.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import utils.MyJsonWikidataParser;
import utils.StanfordNlpApi;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReadFilteredJsonAndProcess {
    private final static Logger LOGGER = LogManager.getLogger(ReadFilteredJsonAndProcess.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String INPUT_JSON = "output/wikidata_filtered.json";
//    private static final String SQL_URL = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/EnWikiLinksAllEvents_v10.db";
    private static final String SQL_URL = "jdbc:sqlite:EnWikiLinksAllEvents_v10.db";
    private static final int LIMIT = -1;

    public static void main(String[] args) throws Exception {
        MyJsonWikidataParser parser = new MyJsonWikidataParser();
        readFilteredJsonAndCreateAdditionalStats(parser);
        LOGGER.info("Done!");
    }

    private static void readFilteredJsonAndCreateAdditionalStats(MyJsonWikidataParser parser) throws Exception {
        File file = new File(INPUT_JSON);
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        List<WECEventWithRelMention> filteredPageList = parser.parse(inputStreamReader);
        LOGGER.info("Found relevant pages-" + filteredPageList.size());

        SQLQueryApi queryApi = new SQLQueryApi(new SQLiteConnections(SQL_URL));
        List<WECCoref> allCorefs = queryApi.readTable(WECCoref.TABLE_COREF, new WECCoref(null));

        List<WECCoref> mergedCorefs = mergeLists(filteredPageList, allCorefs);
        MutableGraph<Integer> eventGraph = buildGraph(filteredPageList, mergedCorefs);

        LOGGER.info("Total Pairs=" + eventGraph.edges().size());
//        sharingContextStats(eventGraph, mergedCorefs, queryApi);
        allSharingContext(eventGraph, mergedCorefs, queryApi);
    }

    /*
        Return a list of all corefs events that are part of the events with relations list (from file)
     */
    private static List<WECCoref> mergeLists(List<WECEventWithRelMention> filteredPageList, List<WECCoref> allCorefs) {
        Set<Integer> allCorefIds = new HashSet<>();
        for (WECEventWithRelMention eventCoref : filteredPageList) {
            allCorefIds.add(eventCoref.getCorefId());
        }

        List<WECCoref> filteredCorefs = new ArrayList<>();
        for(WECCoref coref : allCorefs) {
            if(allCorefIds.contains(coref.getCorefId())) {
                filteredCorefs.add(coref);
            }
        }

        return filteredCorefs;
    }

    /*
       Print a list to file of all event/sub-events and none relations events mentions that share the same
       context (Negative sample)
    */
    private static void allSharingContext(MutableGraph<Integer> eventGraph, List<WECCoref> mergedCorefs, SQLQueryApi queryApi) throws IOException {
        Map<Integer, WECCoref> allCorefIds = getCorefsAsIntegers(mergedCorefs);

        Map<Integer, List<WECMention>> allCoreToMentions = queryApi.readMentionsByCorefIds(allCorefIds.keySet(), LIMIT);

        // Generate a map containing contexts and all mentions that exist within them
        Map<String, List<WECMentionSubEvent>> sharingContext = new HashMap<>();
        Map<String, JsonArray> hashToContext = new HashMap<>();
        int totalMentions = 0;

        for(List<WECMention> mentionsList : allCoreToMentions.values()) {
            totalMentions += mentionsList.size();
            LOGGER.info("So far extracted-" + totalMentions);
            LOGGER.info("Prepare to evaluate-" + mentionsList.size() + " mentions...");
            for (WECMention mention : mentionsList) {
                String md5 = DigestUtils.md5Hex(mention.getContextAsJsonString());
                if(!sharingContext.containsKey(md5)) {
                    sharingContext.put(md5, new ArrayList<>());
                    hashToContext.put(md5, mention.getContext());
                }

                Set<Integer> allSuccessors = new HashSet<>();
                if (eventGraph.nodes().contains(mention.getCorefId())) {
                    allSuccessors = getAllSuccessors(eventGraph.successors(mention.getCorefId()), eventGraph);
                }

                if(!isMentionsInList(mention, sharingContext.get(md5)) && !isMentionLocationOrDate(mention)) {
                    WECMentionSubEvent mentionSubEvent = new WECMentionSubEvent(mention, allSuccessors);
                    sharingContext.get(md5).add(mentionSubEvent);
                }
            }
        }

        LOGGER.info("Total contexts extracted before filtering=" + totalMentions);
        sharingContext.entrySet().removeIf(next -> next.getValue().size() <= 1);

        // need to calc total mentions after removing singletons mentions in context
        totalMentions = 0;
        int totalSubeventRelations = 0;
        int maxMentInContx = 0;
        for(List<WECMentionSubEvent> mentions: sharingContext.values()) {
            totalMentions += mentions.size();
            if(mentions.size() > maxMentInContx) {
                maxMentInContx = mentions.size();
            }
            for(WECMentionSubEvent mention : mentions) {
                totalSubeventRelations += mention.getSubEventOf().size();
            }
        }

        LOGGER.info("Total context extracted after filtering singletons mentions=" + totalMentions);
        LOGGER.info("Total sub-events relations=" + totalSubeventRelations);
        LOGGER.info("Total paragraphs=" + sharingContext.size());
        LOGGER.info("Avg number of mentions in single context=" + (totalMentions / sharingContext.size()));
        LOGGER.info("Context with max mentions=" + maxMentInContx);

        FileWriter writer1 = new FileWriter("output/context_mentions_file.json");
        GSON.toJson(sharingContext, writer1);
        writer1.flush();
        writer1.close();

        FileWriter writer2 = new FileWriter("output/hashcode_to_context.json");
        GSON.toJson(hashToContext, writer2);
        writer2.flush();
        writer2.close();
//        printShareParagraph(sharingContext.values());
    }

    private static boolean isMentionsInList(WECMention o1, List<WECMentionSubEvent> mentionsList) {
        for(WECMentionSubEvent o2 : mentionsList) {
            if(Objects.equals(o1.getCorefId(), o2.getCorefId()) &&
                    Objects.equals(o1.getMentionText(), o2.getMentionText()) &&
                    Objects.equals(o1.getTokenStart(), o2.getTokenStart())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isMentionLocationOrDate(WECMention mention) {
        if(!StringUtils.isNumericSpace(mention.getMentionText())) {
            CoreDocument coreDocument = StanfordNlpApi.withPosAnnotate(mention.getMentionText());
            for(CoreSentence sent : coreDocument.sentences()) {
                for(CoreLabel lab : sent.tokens()) {
                    String ner = lab.ner();
                    if (!ner.equals("CITY") && !ner.equals("COUNTRY") &&
                            !ner.equals("STATE_OR_PROVINCE") && !ner.equals("DATE") && !ner.equals("TIME") &&
                            !ner.equals("NUMBER") && !StringUtils.isNumericSpace(lab.originalText())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static Set<Integer> getAllSuccessors(Set<Integer> corefRootIds, MutableGraph<Integer> eventGraph) {
        Set<Integer> allSuccessors = new HashSet<>();
        if(corefRootIds.isEmpty()) {
            return allSuccessors;
        }

        allSuccessors.addAll(corefRootIds);
        for(Integer coref : corefRootIds) {
            if(eventGraph.nodes().contains(coref)) {
                Set<Integer> directSuccess = eventGraph.successors(coref);
                allSuccessors.addAll(getAllSuccessors(directSuccess, eventGraph));
            }
        }

        return allSuccessors;
    }

    /*
        Print a list to file of event sub-event that share the same context
     */
    private static void sharingContextStats(MutableGraph<Integer> eventGraph, List<WECCoref> mergedCorefs, SQLQueryApi queryApi) throws IOException {
        Map<Integer, WECCoref> allCorefIds = getCorefsAsIntegers(mergedCorefs);

        Map<Integer, List<WECMention>> allCoreToMentions = queryApi.readMentionsByCorefIds(allCorefIds.keySet(), LIMIT);
        int totalMentions = 0;
        for(List<WECMention> mentionsList : allCoreToMentions.values()) {
            totalMentions += mentionsList.size();
        }
        LOGGER.info("Total Mentions extracted=" + totalMentions);

//        int stopCriteria = 100;
        int sharingDocumentPairs = 0;
        int sharingContextPairs = 0;
        Set<EndpointPair<Integer>> edges = eventGraph.edges();
        List<EventSubEventPair> allPairs = new ArrayList<>();
        for (EndpointPair<Integer> edge : edges) {
//            if(sharingContextPairs.size() > stopCriteria) {
//                break;
//            }
            int U = edge.nodeU();
            int V = edge.nodeV();
            if(allCoreToMentions.containsKey(U) && allCoreToMentions.containsKey(V)) {
                for (WECMention mentU : allCoreToMentions.get(U)) {
                    for (WECMention mentV : allCoreToMentions.get(V)) {
                        if (mentU.getExtractedFromPage().equals(mentV.getExtractedFromPage())) {
                            sharingDocumentPairs ++;
                            if (mentU.getContext().equals(mentV.getContext())) {
                                allPairs.add(new EventSubEventPair(mentV, mentU, mentU.getContext()));
                                sharingContextPairs ++;
                            }
                        }
                    }
                }
            }
        }

        LOGGER.info("Sharing document=" + sharingDocumentPairs);
        LOGGER.info("Sharing context=" + sharingContextPairs);
        FileWriter writer = new FileWriter("output/sub_events_sharing_context.json");
        GSON.toJson(allPairs, writer);
        writer.flush();
        writer.close();
//        printShareParagraphEventSubEventPairs(sharingContextPairs);
    }

    private static Map<Integer, WECCoref> getCorefsAsIntegers(List<WECCoref> mergedCorefs) {
        Map<Integer, WECCoref> allCorefIds = new HashMap<>();
        for(WECCoref coref : mergedCorefs) {
            if(!allCorefIds.containsKey(coref.getCorefId())) {
                allCorefIds.put(coref.getCorefId(), coref);
            }
        }
        LOGGER.info("Total unique Coref extracted=" + allCorefIds.size());
        return allCorefIds;
    }

    private static void printShareParagraph(Collection<List<WECMentionSubEvent>> allEventsWithinContext) {
        List<List<String>> contextsToPrint = new ArrayList<>();
        for (List<WECMentionSubEvent> contextMent : allEventsWithinContext) {
            contextsToPrint.add(printSingleParagraph(contextMent));
        }

        for(List<String> context : contextsToPrint) {
            System.out.println(String.join(" " , context));
            System.out.println();
        }
    }

    static List<String> printSingleParagraph(List<WECMentionSubEvent> contextMent) {
        JsonArray context = contextMent.get(0).getContext();
        Stack<WECMentionSubEvent> startIndexMent = new Stack<>();
        contextMent.sort(Comparator.comparingInt(BaseMention::getTokenStart).reversed());
        for(WECMentionSubEvent ment : contextMent) {
            startIndexMent.push(ment);
        }

        List<String> contextAsString = new ArrayList<>();
        WECMentionSubEvent cur = startIndexMent.pop();
        for(JsonElement elem : context) {
            JsonObject asJsonObject = elem.getAsJsonObject();
            List<Map.Entry<String, JsonElement>> entries = Lists.newArrayList(asJsonObject.entrySet());
            String word = entries.get(0).getKey();
            int index = entries.get(0).getValue().getAsInt();
            if (index == cur.getTokenStart()) {
                contextAsString.add("{{");
            }
            contextAsString.add(word);
            if (index == cur.getTokenEnd()) {
                contextAsString.add("(eventId=" + cur.getCorefId() + ", subEventOf=" + cur.getSubEventOf().toString() + ") }}");
                if(!startIndexMent.empty()) {
                    cur = startIndexMent.pop();
                }
            }
        }

        return contextAsString;
    }

    private static void printShareParagraphEventSubEventPairs(List<EventSubEventPair> sharingContextPairs) {
        List<List<String>> contextsToPrint = new ArrayList<>();
        for (EventSubEventPair pair : sharingContextPairs) {
            JsonArray context = pair.getEvent().getContext();
            List<String> contextAsString = new ArrayList<>();
            for(JsonElement elem : context) {
                JsonObject asJsonObject = elem.getAsJsonObject();
                List<Map.Entry<String, JsonElement>> entries = Lists.newArrayList(asJsonObject.entrySet());
                String word = entries.get(0).getKey();
                int index = entries.get(0).getValue().getAsInt();
                if (index == pair.getEvent().getTokenStart() || index == pair.getSubEvent().getTokenStart()) {
                    contextAsString.add("{{");
                }
                contextAsString.add(word);
                if (index == pair.getEvent().getTokenEnd() || index == pair.getSubEvent().getTokenEnd()) {
                    contextAsString.add("}}");
                }
            }

            contextsToPrint.add(contextAsString);
        }

        for(List<String> context : contextsToPrint) {
            System.out.println(String.join(" " , context));
            System.out.println();
        }
    }

    private static MutableGraph<Integer> buildGraph(List<WECEventWithRelMention> pageList, List<WECCoref> mergedCorefs) {
        Map<String, WECCoref> allCorefMap = FilterInitialJson.fromListToMap(mergedCorefs);
        MutableGraph<Integer> eventsGraph = GraphBuilder.directed().allowsSelfLoops(false).build();

        for(WECEventWithRelMention event : pageList) {
            String name = event.getWikipediaLangPageTitle();
            WECCoref sourceEvent = allCorefMap.get(name);
            Set<String> partOfSet = event.getPartOf();
            if(partOfSet != null) {
                for (String partOf : partOfSet) {
                    WECCoref targetEvent = allCorefMap.get(partOf);
                    if(!sourceEvent.equals(targetEvent)) {
                        if (!(eventsGraph.hasEdgeConnecting(sourceEvent.getCorefId(), targetEvent.getCorefId()) ||
                                eventsGraph.hasEdgeConnecting(targetEvent.getCorefId(), sourceEvent.getCorefId()))) {
                            eventsGraph.putEdge(sourceEvent.getCorefId(), targetEvent.getCorefId());
                        }
                    }
                }
            }

            Set<String> hasPartSet = event.getHasPart();
            if(hasPartSet != null) {
                for (String hasPart : hasPartSet) {
                    WECCoref targetEvent = allCorefMap.get(hasPart);
                    if(!sourceEvent.equals(targetEvent)) {
                        if (!(eventsGraph.hasEdgeConnecting(sourceEvent.getCorefId(), targetEvent.getCorefId()) ||
                                eventsGraph.hasEdgeConnecting(targetEvent.getCorefId(), sourceEvent.getCorefId()))) {
                            eventsGraph.putEdge(targetEvent.getCorefId(), sourceEvent.getCorefId());
                        }
                    }
                }
            }
        }

        LOGGER.info("Done generating ");
        return eventsGraph;
    }
}
