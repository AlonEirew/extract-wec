package wec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import wec.config.Configuration;
import wec.data.WECCoref;
import wec.data.WECMention;
import wec.persistence.WECResources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

@Component
@Transactional
public class ExtractWECToJson {
    private final static Logger LOGGER = LogManager.getLogger(ExtractWECToJson.class);

    public void generateJson() throws IOException {
        String jsonOutputDir = Configuration.getConfiguration().getJsonOutputDir();
        File folder = new File(jsonOutputDir);
        if (!folder.exists()) {
            if(!folder.mkdir()) {
                LOGGER.error("Could not create output folder!");
                return;
            }
        }

        String jsonOutputFile = jsonOutputDir + File.separator + Configuration.getConfiguration().getJsonOutputFile();
        try(FileWriter fw = new FileWriter(jsonOutputFile)) {
            Iterable<WECMention> mergedCorefMentions = WECResources.getMentionsRepository().findAll();
            fillAndCleanNoise(mergedCorefMentions);
            JsonArray corefs = extractClusters(mergedCorefMentions);
            Configuration.GSONPretty.toJson(corefs, fw);
        }
        LOGGER.info("process complete!");
    }

    private void fillAndCleanNoise(Iterable<WECMention> wecMentions) {
        Iterator<WECMention> iterator = wecMentions.iterator();
        while(iterator.hasNext()) {
            WECMention wecMention = iterator.next();
            List<String> contextAsList = wecMention.getContext().getContextAsArray();
            String contextAsString = String.join(" ", contextAsList);
            if (contextAsString.contains("colspan") || contextAsString.contains("http")) {
                iterator.remove();
            }

            wecMention.fillMentionNerPosLemma();
            String mentionNer = wecMention.getMentionNer();
            if(mentionNer.equals("PERSON") || mentionNer.equals("LOCATION") || mentionNer.equals("DATE") ||
                    mentionNer.equals("NATIONALITY")) {
                iterator.remove();
            }
        }
    }

    private JsonArray extractClusters(Iterable<WECMention> wecMentions) {
        JsonArray root = new JsonArray();
        for(WECMention mention : wecMentions) {
            WECCoref coref = mention.getCorefChain();
            JsonObject jo = new JsonObject();
            jo.addProperty("coref_chain", mention.getCorefChain().getCorefId());
            jo.addProperty("coref_link", coref.getCorefValue());
            jo.addProperty("doc_id", mention.getExtractedFromPage());
            jo.addProperty("tokens_str", mention.getMentionText());
            jo.addProperty("mention_type", coref.getCorefType());
            jo.addProperty("mention_id", mention.getMentionId());
            jo.addProperty("mention_head", mention.getMentionHead());
            jo.addProperty("mention_head_lemma", mention.getMentionLemma());
            jo.addProperty("mention_head_pos", mention.getMentionPos());
            jo.addProperty("mention_ner", mention.getMentionNer());

            JsonArray tokNum = new JsonArray();
            IntStream.range(mention.getTokenStart(), mention.getTokenEnd() + 1).forEachOrdered(tokNum::add);
            jo.add("tokens_number", tokNum);

            JsonElement element = Configuration.GSON.toJsonTree(mention.getContext().getContextAsArray(),
                    new TypeToken<List<String>>() {}.getType());
            jo.add("mention_context", element.getAsJsonArray());
            root.add(jo);
        }

        return root;
    }
}
