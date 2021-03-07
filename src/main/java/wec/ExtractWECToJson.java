package wec;

import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import wec.config.Configuration;
import wec.data.WECCoref;
import wec.data.WECMention;
import wec.persistence.MentionsRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Component
@Transactional
public class ExtractWECToJson {
    private final static Logger LOGGER = LogManager.getLogger(ExtractWECToJson.class);

    @Autowired private MentionsRepository mentionsRepository;

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
            Iterable<WECMention> mergedCorefMentions = mentionsRepository.findAll();
            JsonArray corefs = CleanAndConvertToJson(mergedCorefMentions);
            Configuration.GSONPretty.toJson(corefs, fw);
        }
        LOGGER.info("process complete!");
    }

    private JsonArray CleanAndConvertToJson(Iterable<WECMention> wecMentions) {
        int sizeBefore = Iterables.size(wecMentions);
        Iterator<WECMention> iterator = wecMentions.iterator();
        LOGGER.info("Total Mentions Extracted=" + sizeBefore);
        int contextRemove = 0;
        int nerRemoved = 0;
        JsonArray root = new JsonArray();
        Map<Long, Map<String, Integer>> lexicalDiversity = new HashMap<>();
        while(iterator.hasNext()) {
            WECMention wecMention = iterator.next();
            if (!isContextValid(wecMention) || !fillAndCheckIsMentionValid(wecMention)) {
                iterator.remove();
                contextRemove++;
                continue;
            }

            if(!lexicalDiversity.containsKey(wecMention.getCorefChain().getCorefId())) {
                lexicalDiversity.put(wecMention.getCorefChain().getCorefId(), new HashMap<>());
            }

            JsonObject jsonObject = convertMentionToJson(wecMention);
            root.add(jsonObject);
        }

        LOGGER.info("Total of " + contextRemove + " mentions with problematic context");
        LOGGER.info("Total of " + nerRemoved + " mentions with suspicious NER removed");
        LOGGER.info("Mentions remaining=" + Iterables.size(wecMentions));

        return root;
    }

    private boolean fillAndCheckIsMentionValid(WECMention mention) {
        mention.fillMentionNerPosLemma();
        String mentionNer = mention.getMentionNer();
        return !mentionNer.equals("PERSON") && !mentionNer.equals("LOCATION") && !mentionNer.equals("DATE") &&
                !mentionNer.equals("NATIONALITY");
    }

    private boolean isContextValid(WECMention mention) {
//        List<String> contextAsList = mention.getContextId().getContextAsArray();
//        String contextAsString = String.join(" ", contextAsList);
//        return !contextAsString.contains("colspan") && !contextAsString.contains("http");
        return true;
    }

    private JsonObject convertMentionToJson(WECMention mention) {
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

//        JsonElement element = Configuration.GSON.toJsonTree(mention.getContextId().getContextAsArray(),
//                new TypeToken<List<String>>() {}.getType());
//        jo.add("mention_context", element.getAsJsonArray());

        return jo;
    }
}
