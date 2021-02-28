//package main;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import Configuration;
//import WECConfigurations;
//import wec.data.WECCoref;
//import wec.data.WECMention;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import wec.persistence.SQLQueryApi;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.sql.SQLException;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.IntStream;
//
//public class ExtractWECToJson {
//    private final static Logger LOGGER = LogManager.getLogger(ExtractWECToJson.class);
//
//    public static void main(String[] args) throws SQLException, IOException {
//        LOGGER.info("Starting process to generate WEC dataset json");
//        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(WECConfigurations.getConfig().getSqlConnectionUrl()));
//
//        File folder = new File("output");
//        if (!folder.exists()) {
//            if(!folder.mkdir()) {
//                LOGGER.error("Could not create output folder!");
//                return;
//            }
//        }
//
//        try(FileWriter fw = new FileWriter("output/corefs.json")) {
//            List<MergedCorefMention> mergedCorefMentions = sqlApi.readJoinedMentionCorefTable(new MergedCorefMention());
//            fillAndCleanNoise(mergedCorefMentions);
//            JsonArray corefs = extractClusters(mergedCorefMentions);
//            Configuration.GSONPretty.toJson(corefs, fw);
//        }
//        LOGGER.info("process complete!");
//    }
//
//    private static void fillAndCleanNoise(List<MergedCorefMention> mergedCorefMentions) {
//        Iterator<MergedCorefMention> iterator = mergedCorefMentions.iterator();
//        while(iterator.hasNext()) {
//            MergedCorefMention coreMention = iterator.next();
//            String contextAsString = coreMention.getMention().getContext().getContextAsString();
//            if (contextAsString.contains("colspan") || contextAsString.contains("http")) {
//                iterator.remove();
//            }
//
//            coreMention.getMention().fillMentionNerPosLemma();
//            String mentionNer = coreMention.getMention().getMentionNer();
//            if(mentionNer.equals("PERSON") || mentionNer.equals("LOCATION") || mentionNer.equals("DATE") ||
//                    mentionNer.equals("NATIONALITY")) {
//                iterator.remove();
//            }
//        }
//    }
//
//    private static JsonArray extractClusters(List<MergedCorefMention> mergedCorefMentions) {
//        JsonArray root = new JsonArray();
//        for(MergedCorefMention mergedCorefMention : mergedCorefMentions) {
//            WECMention mention = mergedCorefMention.getMention();
//            WECCoref coref = mergedCorefMention.getCoref();
//            JsonObject jo = new JsonObject();
//            jo.addProperty("coref_chain", mention.getCorefId());
//            jo.addProperty("coref_link", coref.getCorefValue());
//            jo.addProperty("doc_id", mention.getExtractedFromPage());
//            jo.addProperty("tokens_str", mention.getMentionText());
//            jo.addProperty("mention_type", coref.getCorefType());
//            jo.addProperty("mention_id", mention.getMentionId());
//            jo.addProperty("mention_head", mention.getMentionHead());
//            jo.addProperty("mention_head_lemma", mention.getMentionLemma());
//            jo.addProperty("mention_head_pos", mention.getMentionPos());
//            jo.addProperty("mention_ner", mention.getMentionNer());
//
//            JsonArray tokNum = new JsonArray();
//            IntStream.range(mention.getTokenStart(), mention.getTokenEnd() + 1).forEachOrdered(tokNum::add);
//            jo.add("tokens_number", tokNum);
//
//            JsonArray context = new JsonArray();
//            for(JsonElement tok : mention.getContext().getContext()) {
//                for (Map.Entry<String, JsonElement> entry : tok.getAsJsonObject().entrySet()) {
//                    context.add(entry.getKey());
//                }
//            }
//            jo.add("mention_context", context);
//            root.add(jo);
//        }
//
//        return root;
//    }
//}
