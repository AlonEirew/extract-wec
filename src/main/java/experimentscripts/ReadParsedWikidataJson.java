package experimentscripts;

import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.gson.*;
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
import java.util.concurrent.atomic.AtomicInteger;

public class ReadParsedWikidataJson {
    private final static Logger LOGGER = LogManager.getLogger(ReadParsedWikidataJson.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String[] KEYS = {"Effect", "Cause", "HasPart", "PartOf", "ImmCauseOf", "HasImmCause"};

    public static void main(String[] args) throws Exception {
        MyJsonWikidataParser parser = new MyJsonWikidataParser();
//        runFilterInitialCrudJson(parser);
        readFilterdJsonAndCreateAdditionalStats(parser);
        LOGGER.info("Done!");
    }

    private static void readFilterdJsonAndCreateAdditionalStats(MyJsonWikidataParser parser) throws Exception {
        File file = new File("output/wikidata_filtered.json");
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        List<WECEventWithRelMention> filteredPageList = parser.parse(inputStreamReader);
        LOGGER.info("Found relevant pages-" + filteredPageList.size());

        String sqlConUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/EnWikiLinksAllEvents_v10.db";
//        String sqlConUrl = "jdbc:sqlite:EnWikiLinksAllEvents_v10.db";
        SQLQueryApi queryApi = new SQLQueryApi(new SQLiteConnections(sqlConUrl));
        List<WECCoref> allCorefs = queryApi.readTable(WECCoref.TABLE_COREF, new WECCoref(null));

        List<WECCoref> mergedCorefs = mergeLists(filteredPageList, allCorefs);
        Map<String, WECCoref> allCorefMap = fromListToMap(mergedCorefs);

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

        int limit = 10000;
        Map<Integer, List<WECMention>> allCoreToMentions = queryApi.readMentionsByCorefIds(allCorefIds, limit);
        int totalMentions = 0;
        for(List<WECMention> mentionsList : allCoreToMentions.values()) {
            totalMentions += mentionsList.size();
        }
        System.out.println("Total Mentions extracted=" + totalMentions);

        int stopCriteria = 100;
        Set<Map.Entry<WECMention, WECMention>> sharingDocumentPairs = new HashSet<>();
        Set<Map.Entry<WECMention, WECMention>> sharingContextPairs = new HashSet<>();
        Set<Map.Entry<WECMention, WECMention>> sharingSentencePairs = new HashSet<>();
        Set<EndpointPair<Integer>> edges = eventGraph.edges();
        for (EndpointPair<Integer> edge : edges) {
            if(sharingContextPairs.size() > stopCriteria) {
                break;
            }
            int U = edge.nodeU();
            int V = edge.nodeV();
            if(allCoreToMentions.containsKey(U) && allCoreToMentions.containsKey(V)) {
                for (WECMention mentU : allCoreToMentions.get(U)) {
                    for (WECMention mentV : allCoreToMentions.get(V)) {
                        if (mentU.getExtractedFromPage().equals(mentV.getExtractedFromPage())) {
                            AbstractMap.SimpleEntry<WECMention, WECMention> entry = new AbstractMap.SimpleEntry<>(mentU, mentV);
                            sharingDocumentPairs.add(entry);
                            if (mentU.getContext().equals(mentV.getContext())) {
                                sharingContextPairs.add(entry);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Sharing document=" + sharingDocumentPairs.size());
        System.out.println("Sharing context=" + sharingContextPairs.size());
//        GSON.toJson(sharingContextPairs, new FileWriter("output/sub_events_sharing_context.json"));
        printShareParagraph(sharingContextPairs);
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

//        boolean b = eventsGraph.allowsSelfLoops();
//        Set<EndpointPair<WECCoref>> edges = eventsGraph.edges();
//        for(EndpointPair<WECCoref> edge : edges) {
//            Set<WECCoref> wecCorefs = eventsGraph.adjacentNodes(edge.nodeU());
//            int degree = eventsGraph.degree(edge.nodeU());
//            int inDegree = eventsGraph.inDegree(edge.nodeU());
//            int outDegree = eventsGraph.outDegree(edge.nodeU());
//        }

        System.out.println("Done generating ");
        return eventsGraph;
    }

    private static void runFilterInitialCrudJson(MyJsonWikidataParser parser) throws Exception {
        File file = new File("output/wikidata_parsed_out_orig.json");
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        List<WECEventWithRelMention> pageList = parser.parse(inputStreamReader);

        String sqlConUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/EnWikiLinksAllEvents_v10.db";
//        String sqlConUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksExperiment.db";
        SQLQueryApi queryApi = new SQLQueryApi(new SQLiteConnections(sqlConUrl));
        List<WECCoref> allCorefs = queryApi.readTable(WECCoref.TABLE_COREF, new WECCoref(null));
        Map<String, WECCoref> allCorefMap = fromListToMap(allCorefs);

        LOGGER.info("Found relevant pages-" + pageList.size());

        Map<Integer, WECEventWithRelMention> wecEventWithRelMentions = filterAndPrintDist(pageList, allCorefMap);
        parser.write(new File("output/wikidata_filtered.json"), Lists.newArrayList(wecEventWithRelMentions.values()));
    }

    private static Map<String, WECCoref> fromListToMap(List<WECCoref> allCorefs) {
        Map<String, WECCoref> allCorefMap = new HashMap<>();
        for(WECCoref coref : allCorefs) {
            allCorefMap.put(coref.getCorefValue(), coref);
        }

        return allCorefMap;
    }


    private static Map<Integer, WECEventWithRelMention> filterAndPrintDist(List<WECEventWithRelMention> pageList,
                                                                           Map<String, WECCoref> allCorefMap) {
        Map<Integer, WECEventWithRelMention> finalResultList = new HashMap<>();

        Map<String, AtomicInteger> relationDistributionMap = new HashMap<>();
        for (String key : KEYS) {
            relationDistributionMap.put(key, new AtomicInteger());
        }

        Map<String, AtomicInteger> subTypeDistributionMap = new HashMap<>();
        Map<String, AtomicInteger> typeDistributionMap = new HashMap<>();


        for (WECEventWithRelMention page : pageList) {
            Map<String, Set<String>> eventToMap = EventObjToMap(page);
            boolean found = false;
            // First check if wikidata page relation is part of wikipedia events
            if(allCorefMap.containsKey(page.getWikipediaLangPageTitle())) {
                WECCoref wecCoref = allCorefMap.get(page.getWikipediaLangPageTitle());
                page.setEventType(wecCoref.getCorefType());
                page.setSubEventType(wecCoref.getCorefSubType());
                page.setCorefId(wecCoref.getCorefId());
                for (String key : KEYS) {
                    if(key.equals(KEYS[2]) || key.equals(KEYS[3])) {
                        Set<String> validatedSet = validateFixAndSet(eventToMap.get(key), allCorefMap, finalResultList);
                        eventToMap.replace(key, validatedSet);
                        setInEventsList(key, page, validatedSet);
                        if (!validatedSet.isEmpty()) {
                            if (!typeDistributionMap.containsKey(page.getEventType())) {
                                typeDistributionMap.put(page.getEventType(), new AtomicInteger());
                            }

                            if (!subTypeDistributionMap.containsKey(page.getSubEventType())) {
                                subTypeDistributionMap.put(page.getSubEventType(), new AtomicInteger());
                            }

                            found = true;
                            relationDistributionMap.get(key).addAndGet(validatedSet.size());
                            subTypeDistributionMap.get(page.getSubEventType()).addAndGet(validatedSet.size());
                            typeDistributionMap.get(page.getEventType()).addAndGet(validatedSet.size());
                        }
                    }
                }
            }

            if (found) {
                if(finalResultList.containsKey(page.getCorefId())) {
                    WECEventWithRelMention wecEventWithRelMention = finalResultList.get(page.getCorefId());
                    wecEventWithRelMention.setElasticPageId(page.getElasticPageId());
                    wecEventWithRelMention.setWikidataPageId(page.getWikidataPageId());

                    wecEventWithRelMention.getAliases().addAll(page.getAliases());
                    wecEventWithRelMention.getPartOf().addAll(page.getPartOf());
                    wecEventWithRelMention.getHasPart().addAll(page.getHasPart());
                    wecEventWithRelMention.getHasCause().addAll(page.getHasCause());
                    wecEventWithRelMention.getHasEffect().addAll(page.getHasEffect());
                    wecEventWithRelMention.getHasImmediateCause().addAll(page.getHasImmediateCause());
                    wecEventWithRelMention.getImmediateCauseOf().addAll(page.getImmediateCauseOf());

                } else {
                    finalResultList.put(page.getCorefId(), page);
                }
            }
        }

        LOGGER.info(GSON.toJson(relationDistributionMap));
        LOGGER.info(GSON.toJson(typeDistributionMap));
        LOGGER.info(GSON.toJson(subTypeDistributionMap));
        return finalResultList;
    }

    private static void setInEventsList(String key, WECEventWithRelMention page, Set<String> newSet) {
        switch (key) {
            case "Effect":
                page.setHasEffect(newSet);
                break;
            case "Cause":
                page.setHasCause(newSet);
                break;
            case "HasPart":
                page.setHasPart(newSet);
                break;
            case "PartOf":
                page.setPartOf(newSet);
                break;
            case "ImmCauseOf":
                page.setImmediateCauseOf(newSet);
                break;
            case "HasImmCause":
                page.setHasImmediateCause(newSet);
                break;
            default:
                break;
        }
    }

    private static Map<String, Set<String>> EventObjToMap(WECEventWithRelMention page) {
        Map<String, Set<String>> distributionMap = new HashMap<>();
        distributionMap.put(KEYS[0], page.getHasEffect());
        distributionMap.put(KEYS[1], page.getHasCause());
        distributionMap.put(KEYS[2], page.getHasPart());
        distributionMap.put(KEYS[3], page.getPartOf());
        distributionMap.put(KEYS[4], page.getImmediateCauseOf());
        distributionMap.put(KEYS[5], page.getHasImmediateCause());
        return distributionMap;
    }

    private static Set<String> validateFixAndSet(Set<String> toValidate, Map<String, WECCoref> allCorefMap,
                                                 Map<Integer, WECEventWithRelMention> finalResultList) {
        Set<String> newSet = new HashSet<>();
        if (toValidate != null && !toValidate.isEmpty()) {
            for (String str : toValidate) {
                if (allCorefMap.containsKey(str)) {
                    WECCoref wecCoref = allCorefMap.get(str);
                    newSet.add(str);
                    if(!finalResultList.containsKey(wecCoref.getCorefId())) {
                        finalResultList.put(wecCoref.getCorefId(), new WECEventWithRelMention(wecCoref));
                    }
                }
            }
        }
        return newSet;
    }
}
