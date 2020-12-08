package experimentscripts.event;

import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.gson.*;
import data.EventSubEventPair;
import data.WECCoref;
import data.WECEventWithRelMention;
import data.WECMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import utils.MyJsonWikidataParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReadFilteredJsonAndProcess {
    private final static Logger LOGGER = LogManager.getLogger(ReadFilteredJsonAndProcess.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String INPUT_JSON = "output/wikidata_filtered.json";
//    private static final String SQL_URL = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/EnWikiLinksAllEvents_v10.db";
    private static final String SQL_URL = "jdbc:sqlite:EnWikiLinksAllEvents_v10.db";

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
        Map<String, WECCoref> allCorefMap = FilterInitialJson.fromListToMap(mergedCorefs);

        MutableGraph<Integer> eventGraph = buildGraph(filteredPageList, allCorefMap);
        System.out.println("Total Pairs=" + eventGraph.edges().size());
        sharingContextStats(eventGraph, mergedCorefs, queryApi);
    }

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

    private static void sharingContextStats(MutableGraph<Integer> eventGraph, List<WECCoref> mergedCorefs, SQLQueryApi queryApi) throws IOException {
        Set<Integer> allCorefIds = new HashSet<>();
        for(WECCoref coref : mergedCorefs) {
            allCorefIds.add(coref.getCorefId());
        }
        System.out.println("Total unique Coref extracted=" + allCorefIds.size());

        int limit = -1;
        Map<Integer, List<WECMention>> allCoreToMentions = queryApi.readMentionsByCorefIds(allCorefIds, limit);
        int totalMentions = 0;
        for(List<WECMention> mentionsList : allCoreToMentions.values()) {
            totalMentions += mentionsList.size();
        }
        System.out.println("Total Mentions extracted=" + totalMentions);

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

        System.out.println("Sharing document=" + sharingDocumentPairs);
        System.out.println("Sharing context=" + sharingContextPairs);
        FileWriter writer = new FileWriter("output/sub_events_sharing_context.json");
        GSON.toJson(allPairs, writer);
        writer.flush();
        writer.close();
//        printShareParagraph(sharingContextPairs);
    }

    private static void printShareParagraph(Set<Map.Entry<WECMention, WECMention>> sharingContextPairs) {
        List<List<String>> contextsToPrint = new ArrayList<>();
        for (Map.Entry<WECMention, WECMention> pair : sharingContextPairs) {
            JsonArray context = pair.getKey().getContext();
            List<String> contextAsString = new ArrayList<>();
            for(JsonElement elem : context) {
                JsonObject asJsonObject = elem.getAsJsonObject();
                List<Map.Entry<String, JsonElement>> entries = Lists.newArrayList(asJsonObject.entrySet());
                String word = entries.get(0).getKey();
                int index = entries.get(0).getValue().getAsInt();
                if (index == pair.getKey().getTokenStart() || index == pair.getValue().getTokenStart()) {
                    contextAsString.add("[");
                }
                contextAsString.add(word);
                if (index == pair.getKey().getTokenEnd() || index == pair.getValue().getTokenEnd()) {
                    contextAsString.add("]");
                }
            }

            contextsToPrint.add(contextAsString);
        }

        for(List<String> context : contextsToPrint) {
            System.out.println(String.join(" " , context));
            System.out.println();
        }
    }

    private static MutableGraph<Integer> buildGraph(List<WECEventWithRelMention> pageList, Map<String, WECCoref> allCorefMap) {
        MutableGraph<Integer> eventsGraph = GraphBuilder.directed().allowsSelfLoops(false).build();

        for(WECEventWithRelMention event : pageList) {
            String name = event.getWikipediaLangPageTitle();
            WECCoref sourceEvent = allCorefMap.get(name);
            Set<String> partOfSet = event.getPartOf();
            if(partOfSet != null) {
                for (String partOf : partOfSet) {
                    WECCoref targetEvent = allCorefMap.get(partOf);
                    if(!sourceEvent.equals(targetEvent)) {
                        if (!eventsGraph.hasEdgeConnecting(sourceEvent.getCorefId(), targetEvent.getCorefId())) {
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
                        if (!eventsGraph.hasEdgeConnecting(targetEvent.getCorefId(), sourceEvent.getCorefId())) {
                            eventsGraph.putEdge(targetEvent.getCorefId(), sourceEvent.getCorefId());
                        }
                    }
                }
            }
        }

        System.out.println("Done generating ");
        return eventsGraph;
    }
}
