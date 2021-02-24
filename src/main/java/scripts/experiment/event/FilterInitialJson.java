package scripts.experiment.event;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data.WECCoref;
import data.WECEventWithRelMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;
import utils.MyJsonWikidataParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
    Read the initial JSON that produced by Wikipedia-to-elastic script WikiDataFeatToFile.
    Filter from this file only events that has relevant relations associated with them and
 */
public class FilterInitialJson {
    private final static Logger LOGGER = LogManager.getLogger(FilterInitialJson.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String[] KEYS = {"Effect", "Cause", "HasPart", "PartOf", "ImmCauseOf", "HasImmCause"};

    private static final String INPUT_JSON = "output/wikidata_parsed_out_orig.json";
    private static final String SQL_URL = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/EnWikiLinksAllEvents_v10.db";
//    private static final String SQL_URL = "jdbc:sqlite:EnWikiLinksAllEvents_v10.db";

    public static void main(String[] args) throws Exception {
        MyJsonWikidataParser parser = new MyJsonWikidataParser();
        runFilterInitialCrudJson(parser);
        LOGGER.info("Done!");
    }

    private static void runFilterInitialCrudJson(MyJsonWikidataParser parser) throws Exception {
        File file = new File(INPUT_JSON);
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        List<WECEventWithRelMention> pageList = parser.parse(inputStreamReader);

        SQLQueryApi queryApi = new SQLQueryApi(new SQLiteConnections(SQL_URL));
        List<WECCoref> allCorefs = queryApi.readTable(WECCoref.TABLE_COREF, new WECCoref(null));
        Map<String, WECCoref> allCorefMap = fromListToMap(allCorefs);

        LOGGER.info("Found relevant pages-" + pageList.size());

        Map<Integer, WECEventWithRelMention> wecEventWithRelMentions = filterAndPrintDist(pageList, allCorefMap);
        parser.write(new File("output/wikidata_filtered.json"), Lists.newArrayList(wecEventWithRelMentions.values()));
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

    static Map<String, WECCoref> fromListToMap(List<WECCoref> allCorefs) {
        Map<String, WECCoref> allCorefMap = new HashMap<>();
        for(WECCoref coref : allCorefs) {
            allCorefMap.put(coref.getCorefValue(), coref);
        }

        return allCorefMap;
    }
}
