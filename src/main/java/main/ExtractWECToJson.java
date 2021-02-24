package main;

import com.google.gson.*;
import config.WECConfigurations;
import data.MergedCorefMention;
import data.WECCoref;
import data.WECMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ExtractWECToJson {
    private final static Logger LOGGER = LogManager.getLogger(ExtractWECToJson.class);
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void main(String[] args) throws SQLException, IOException {
        LOGGER.info("Starting process to generate WEC dataset json");
        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections(WECConfigurations.getConfig().getSqlConnectionUrl()));
        JsonArray corefs = extractClusters(sqlApi);
        try(FileWriter fw = new FileWriter("output/corefs.json")) {
            GSON.toJson(corefs, fw);
        }
        LOGGER.info("process complete!");
    }

    private static JsonArray extractClusters(SQLQueryApi sqlApi) {
        List<MergedCorefMention> mergedCorefMentions = sqlApi.readJoinedMentionCorefTable(WECMention.TABLE_MENTIONS, WECCoref.TABLE_COREF, new MergedCorefMention());
        JsonArray root = new JsonArray();
        for(MergedCorefMention mergedCorefMention : mergedCorefMentions) {
            WECMention mention = mergedCorefMention.getMention();
            WECCoref coref = mergedCorefMention.getCoref();
            JsonObject jo = new JsonObject();
            jo.addProperty("coref_chain", mention.getCorefId());
            jo.addProperty("coref_link", coref.getCorefValue());
            jo.addProperty("doc_id", mention.getExtractedFromPage());
            jo.addProperty("tokens_str", mention.getMentionText());
            jo.addProperty("mention_type", coref.getCorefType());
            jo.addProperty("mention_id", mention.getMentionId());

            JsonArray tokNum = new JsonArray();
            IntStream.range(mention.getTokenStart(), mention.getTokenEnd() + 1).forEachOrdered(tokNum::add);
            jo.add("tokens_number", tokNum);

            JsonArray context = new JsonArray();
            for(JsonElement tok : mention.getContext()) {
                for (Map.Entry<String, JsonElement> entry : tok.getAsJsonObject().entrySet()) {
                    context.add(entry.getKey());
                }
            }
            jo.add("mention_context", context);
            root.add(jo);
        }

        return root;
    }
}
