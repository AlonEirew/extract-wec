package scripts.experiments.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import data.EventSubEventPair;
import data.WECCoref;
import data.WECMentionSubEvent;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GenMoreEventsStats {
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String INPUT_JSON_PAIRS = "output/sub_events_sharing_context.json";
    private static final String INPUT_JSON_CONTEXTS = "output/context_mentions_file.json";
    private static final String INPUT_JSON_CONTEXTS_HASH = "output/hashcode_to_context.json";

//        private static final String SQL_URL = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/EnWikiLinksAllEvents_v10.db";
    private static final String SQL_URL = "jdbc:sqlite:EnWikiLinksAllEvents_v10.db";
    private static final int MAX_EXAMPLES = 5;


    public static void main(String[] args) throws Exception {
//        extractMorePairsStats();
//        extractContextStats();
//        genWithManyMentionsInContextExamples();
//        genOfEachSubTypeExample();
        generateCorefMentionsDist();
        System.out.println();
    }

    private static void extractMorePairsStats() throws FileNotFoundException {
        Type listType = new TypeToken<ArrayList<EventSubEventPair>>(){}.getType();
        List<EventSubEventPair> eventPairs = GSON.fromJson(new FileReader(INPUT_JSON_PAIRS), listType);
        Map<String, AtomicInteger> pairsDist = new HashMap<>();
        for(EventSubEventPair pair : eventPairs) {
            String pairId = pair.getEvent().getCorefId() + "_" + pair.getSubEvent().getCorefId();
            if(!pairsDist.containsKey(pairId)) {
                pairsDist.put(pairId, new AtomicInteger(0));
            }

            pairsDist.get(pairId).incrementAndGet();
        }

        Map<Integer, AtomicInteger> timesAppear = new HashMap<>();
        for (AtomicInteger value : pairsDist.values()) {
            if(!timesAppear.containsKey(value.get())) {
                timesAppear.put(value.get(), new AtomicInteger(0));
            }

            timesAppear.get(value.get()).incrementAndGet();
        }

        System.out.println(pairsDist.size());
        System.out.println(GSON.toJson(pairsDist));
        System.out.println(GSON.toJson(timesAppear));

    }

    private static void extractContextStats() throws FileNotFoundException {
        Type listType = new TypeToken<Map<String, List<WECMentionSubEvent>>>(){}.getType();
        Map<String, List<WECMentionSubEvent>> eventsInContext = GSON.fromJson(new FileReader(INPUT_JSON_CONTEXTS), listType);

        SQLQueryApi queryApi = new SQLQueryApi(new SQLiteConnections(SQL_URL));

        Map<Integer, AtomicInteger> contextDistributions = new HashMap<>();
        Map<String, AtomicInteger> mentionsTypeDistributions = new HashMap<>();
        Map<String, AtomicInteger> mentionsSubTypeDistributions = new HashMap<>();
        for(List<WECMentionSubEvent> contextEvents : eventsInContext.values()) {
            if(!contextDistributions.containsKey(contextEvents.size())) {
                contextDistributions.put(contextEvents.size(), new AtomicInteger());
            }
            contextDistributions.get(contextEvents.size()).incrementAndGet();

            WECMentionSubEvent mentionSubEvent = contextEvents.get(0);
            WECCoref corefById = queryApi.getCorefById(mentionSubEvent.getCorefId());
            if(!mentionsTypeDistributions.containsKey(corefById.getCorefType())) {
                mentionsTypeDistributions.put(corefById.getCorefType(), new AtomicInteger());
            }
            mentionsTypeDistributions.get(corefById.getCorefType()).addAndGet(contextEvents.size());

            if(!mentionsSubTypeDistributions.containsKey(corefById.getCorefSubType())) {
                mentionsSubTypeDistributions.put(corefById.getCorefSubType(), new AtomicInteger());
            }
            mentionsSubTypeDistributions.get(corefById.getCorefSubType()).addAndGet(contextEvents.size());
        }

        System.out.println("Context Distribution:");
        System.out.println(GSON.toJson(contextDistributions));
        System.out.println("Mentions Type Distribution:");
        System.out.println(GSON.toJson(mentionsTypeDistributions));
        System.out.println("Mentions Sub-Type Distribution:");
        System.out.println(GSON.toJson(mentionsSubTypeDistributions));
    }

    private static void genWithManyMentionsInContextExamples() throws FileNotFoundException {
        Type listType = new TypeToken<Map<String, List<WECMentionSubEvent>>>(){}.getType();
        Map<String, List<WECMentionSubEvent>> eventPairs = GSON.fromJson(new FileReader(INPUT_JSON_CONTEXTS), listType);

        Type contextType = new TypeToken<Map<String, JsonArray>>(){}.getType();
        Map<String, JsonArray> contextToHash = GSON.fromJson(new FileReader(INPUT_JSON_CONTEXTS_HASH), contextType);

        Map<Integer, Boolean> distributions = new HashMap<>();
        for(String contextKey : eventPairs.keySet()) {
            List<WECMentionSubEvent> contextEvents = eventPairs.get(contextKey);
            if(contextEvents.size() %2 == 0 && !distributions.containsKey(contextEvents.size())) {
                System.out.println("Found context " + contextKey + " with " + contextEvents.size() + " mentions");
                distributions.put(contextEvents.size(), Boolean.TRUE);
                contextEvents.get(0).getContext().setContext(contextToHash.get(contextKey));

                System.out.println(String.join(" " , ReadFilteredJsonAndProcess.printSingleParagraph(contextEvents)));
                System.out.println();
            }
        }
    }

    private static void genOfEachSubTypeExample() throws FileNotFoundException {
        SQLQueryApi queryApi = new SQLQueryApi(new SQLiteConnections(SQL_URL));
        Type listType = new TypeToken<Map<String, List<WECMentionSubEvent>>>(){}.getType();
        Map<String, List<WECMentionSubEvent>> eventPairs = GSON.fromJson(new FileReader(INPUT_JSON_CONTEXTS), listType);

        Type contextType = new TypeToken<Map<String, JsonArray>>(){}.getType();
        Map<String, JsonArray> contextToHash = GSON.fromJson(new FileReader(INPUT_JSON_CONTEXTS_HASH), contextType);

        Map<String, AtomicInteger> distributions = new HashMap<>();
        for(String contextKey : eventPairs.keySet()) {
            List<WECMentionSubEvent> contextEvents = eventPairs.get(contextKey);
            WECCoref corefById = queryApi.getCorefById(contextEvents.get(0).getCorefId());
            if(!distributions.containsKey(corefById.getCorefType())) {
                distributions.put(corefById.getCorefType(), new AtomicInteger(1));
            }

            if(distributions.get(corefById.getCorefType()).get() <=  MAX_EXAMPLES) {
                System.out.println("Found context " + corefById.getCorefType());
                contextEvents.get(0).setContext(contextToHash.get(contextKey));
                System.out.println(String.join(" ", ReadFilteredJsonAndProcess.printSingleParagraph(contextEvents)));
                System.out.println();
                distributions.get(corefById.getCorefType()).incrementAndGet();
            }
        }
    }

    private static void generateCorefMentionsDist() throws IOException {
        Type listType = new TypeToken<Map<String, List<WECMentionSubEvent>>>(){}.getType();
        Map<String, List<WECMentionSubEvent>> eventsInContext = GSON.fromJson(new FileReader(INPUT_JSON_CONTEXTS, StandardCharsets.UTF_8), listType);

        Map<Integer, AtomicInteger> corefMentionsDist = new HashMap<>();
        Map<Integer, String> corefIdToTitle = new HashMap<>();
        for(List<WECMentionSubEvent> contextEvents : eventsInContext.values()) {
            for(WECMentionSubEvent mentionSubEvent : contextEvents) {
                if (!corefMentionsDist.containsKey(mentionSubEvent.getCorefId())) {
                    corefMentionsDist.put(mentionSubEvent.getCorefId(), new AtomicInteger());
                    corefIdToTitle.put(mentionSubEvent.getCorefId(), mentionSubEvent.getMentionText().replaceAll(":", ";"));
                }
                corefMentionsDist.get(mentionSubEvent.getCorefId()).incrementAndGet();
            }
        }

        Map<String, Integer> finalDist = new HashMap<>();
        for(Integer corefId : corefMentionsDist.keySet()) {
            finalDist.put(corefIdToTitle.get(corefId), corefMentionsDist.get(corefId).get());
        }

//        System.out.println("Coref Mentions Distribution:");
//        System.out.println(GSON.toJson(finalDist));

        FileWriter writer1 = new FileWriter("output/generated_stats.json");
        GSON.toJson(corefMentionsDist, writer1);
        writer1.flush();
        writer1.close();
        System.out.println("Done!");
    }
}
