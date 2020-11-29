package utils;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import data.WECEventWithRelMention;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyJsonWikidataParser {
    private final static Logger LOGGER = LogManager.getLogger(MyJsonWikidataParser.class);
    private final static Gson GSON = new Gson();

    public List<WECEventWithRelMention> parse(InputStreamReader inputStreamReader) throws Exception {
        List<WECEventWithRelMention> allParsedWikidataPages;
        try(JsonReader reader = GSON.newJsonReader(inputStreamReader)) {
            allParsedWikidataPages = read(reader);
        } catch (Exception ex) {
            LOGGER.error("Failed to parse JSON file", ex);
            throw ex;
        }

        LOGGER.info("Done Loading Json!");
        return allParsedWikidataPages;
    }

    public List<WECEventWithRelMention> read(JsonReader reader) throws IOException {
        reader.setLenient(true);
        final List<WECEventWithRelMention> allParsedWikidataPages = new ArrayList<>();
        try {
            int counter = 0;
            reader.beginArray();
            while (reader.hasNext()) {
                counter++;
                if(counter % 100000 == 0) {
                    LOGGER.info("Pages parsed-" + counter);
                }

                WECEventWithRelMention wikidataPage = GSON.fromJson(reader, WECEventWithRelMention.class);
                allParsedWikidataPages.add(wikidataPage);
            }

            reader.endArray();
        } catch (IOException e) {
            LOGGER.error("Failed to run JSON parser instance", e);
            throw e;
        }

        return allParsedWikidataPages;
    }

    private boolean isEmpty(WECEventWithRelMention wikidataPage) {
        return ((wikidataPage.getPartOf()) == null || wikidataPage.getPartOf().isEmpty()) &&
                (wikidataPage.getHasPart() == null || wikidataPage.getHasPart().isEmpty()) &&
                (wikidataPage.getHasEffect() == null || wikidataPage.getHasEffect().isEmpty()) &&
                (wikidataPage.getHasCause() == null || wikidataPage.getHasCause().isEmpty()) &&
                (wikidataPage.getImmediateCauseOf() == null || wikidataPage.getImmediateCauseOf().isEmpty()) &&
                (wikidataPage.getHasImmediateCause() == null || wikidataPage.getHasImmediateCause().isEmpty());
    }

    public void write(File outFile, List<WECEventWithRelMention> wikidataPages) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
        writer.setIndent("    ");
        writePages(writer, wikidataPages);
        writer.close();
    }

    private void writePages(JsonWriter writer, List<WECEventWithRelMention> wikidataPages) throws IOException {
        int totalPage = wikidataPages.size();
        LOGGER.info("Start writing " + totalPage + " wikidataPages");
        writer.beginArray();
        for(int i = 0 ; i < wikidataPages.size() ; i++) {
            WECEventWithRelMention page = wikidataPages.get(i);
            writePage(writer, page);
            totalPage--;

            if (i % 1000 == 0) {
                LOGGER.info(totalPage + "- Pages remaining...");
            }
        }
        writer.endArray();
    }

    private void writePage(JsonWriter writer, WECEventWithRelMention page) throws IOException {
        writer.beginObject();
        writer.name("corefId").value(page.getCorefId());
        writer.name("wikidataPageId").value(page.getWikidataPageId());
        writer.name("wikipediaLangPageTitle").value(page.getWikipediaLangPageTitle());
        writer.name("elasticPageId").value(page.getElasticPageId());
        writer.name("eventType").value(page.getEventType());
        writer.name("subEventType").value(page.getSubEventType());

        writer.name("aliases");
        writeSet(writer, page.getAliases());

        writer.name("hasPart");
        writeSet(writer, page.getHasPart());

        writer.name("partOf");
        writeSet(writer, page.getPartOf());

        writer.name("hasCause");
        writeSet(writer, page.getHasCause());

        writer.name("hasEffect");
        writeSet(writer, page.getHasEffect());

        writer.name("hasImmediateCause");
        writeSet(writer, page.getHasImmediateCause());

        writer.name("immediateCauseOf");
        writeSet(writer, page.getImmediateCauseOf());

        writer.endObject();
    }

    private void writeSet(JsonWriter writer, Set<String> setToWriter) throws IOException {
        writer.beginArray();
        for (String value : setToWriter) {
            writer.value(value);
        }
        writer.endArray();
    }
}
