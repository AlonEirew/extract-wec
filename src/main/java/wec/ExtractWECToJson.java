package wec;

import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import me.tongfei.progressbar.ProgressBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import wec.config.Configuration;
import wec.data.WECContext;
import wec.data.WECCoref;
import wec.data.WECMention;
import wec.persistence.WECResources;
import wec.utils.StanfordNlpApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

@Component
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
        StanfordNlpApi.getPipelineWithPos();
        try(JsonWriter writer = new JsonWriter(new FileWriter(jsonOutputFile))) {
            Iterable<WECMention> mergedCorefMentions = WECResources.getDbRepository().findAllMentions();
            CleanAndWriteToJson(mergedCorefMentions, writer);
        }
        LOGGER.info("process complete!");
    }

    private void CleanAndWriteToJson(Iterable<WECMention> wecMentions, JsonWriter writer) throws IOException {
        int sizeBefore = Iterables.size(wecMentions);
        Iterator<WECMention> iterator = wecMentions.iterator();
        LOGGER.info("Total Mentions Extracted=" + sizeBefore);
        int contextRemove = 0;
        int nerRemoved = 0;
        int lexicalRemove = 0;
        Map<Long, Map<String, Integer>> lexicalDiversity = new HashMap<>();
        writer.setIndent("\t");
        writer.beginArray();
        try (ProgressBar pb = new ProgressBar("Processing", sizeBefore)) {
            while (iterator.hasNext()) {
                pb.step();
                WECMention wecMention = iterator.next();
                if(!fillAndCheckIsMentionValid(wecMention)) {
                    iterator.remove();
                    nerRemoved++;
                    continue;
                }

                Optional<WECContext> retContext = WECResources.getDbRepository().findContextById(wecMention.getContextId());
                if (retContext.isEmpty() || !isContextValid(retContext.get())) {
                    iterator.remove();
                    contextRemove++;
                    continue;
                }

                if (!lexicalDiversity.containsKey(wecMention.getCorefChain().getCorefId())) {
                    lexicalDiversity.put(wecMention.getCorefChain().getCorefId(), new HashMap<>());
                }

                if(!lexicalDiversity.get(wecMention.getCorefChain().getCorefId()).containsKey(wecMention.getMentionText())) {
                    lexicalDiversity.get(wecMention.getCorefChain().getCorefId()).put(wecMention.getMentionText(), 0);
                }

                if(lexicalDiversity.get(wecMention.getCorefChain().getCorefId()).get(wecMention.getMentionText()) >=
                        Configuration.getConfiguration().getLexicalThresh()) {
                    iterator.remove();
                    lexicalRemove++;
                    continue;
                }

                Configuration.GSON.toJson(convertMentionToJson(wecMention, retContext.get()), writer);
            }
        }

        writer.endArray();
        writer.close();

        LOGGER.info("Total of " + contextRemove + " mentions with problematic context");
        LOGGER.info("Total of " + nerRemoved + " mentions with suspicious NER removed");
        LOGGER.info("Total of " + lexicalRemove + " didn't pass lexical threshold");
        LOGGER.info("Mentions remaining=" + Iterables.size(wecMentions));
    }

    private boolean fillAndCheckIsMentionValid(WECMention mention) {
        mention.fillMentionNerPosLemma();
        String mentionNer = mention.getMentionNer();
        return !mentionNer.equals("PERSON") && !mentionNer.equals("LOCATION") && !mentionNer.equals("DATE") &&
                !mentionNer.equals("NATIONALITY");
    }

    private boolean isContextValid(WECContext context) {
        List<String> contextAsArray = context.getContextAsArray();
        String contextAsString = String.join(" ", contextAsArray);
        return !contextAsString.contains("colspan") && !contextAsString.contains("http");
    }

    private JsonObject convertMentionToJson(WECMention mention, WECContext context) {
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

        JsonElement element = Configuration.GSON.toJsonTree(context.getContextAsArray(), new TypeToken<List<String>>() {}.getType());
        jo.add("mention_context", element.getAsJsonArray());

        return jo;
    }
}
