package wikilinks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import persistence.ElasticFullDataReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class WikiToWikiLinksMain {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ElasticFullDataReader elasticFullDataReader = new ElasticFullDataReader();
        final List<WikiLinksMention> wikiLinksMentions = elasticFullDataReader.readAll("localhost", 9200, "http", "enwiki_v2");
        Map<String, List<WikiLinksMention>> finalResults = new HashMap<>();
        for(WikiLinksMention mention : wikiLinksMentions) {
            if(finalResults.containsKey(mention.getCorefChain())) {
                finalResults.get(mention.getCorefChain()).add(mention);
            } else {
                finalResults.put(mention.getCorefChain(), new ArrayList<>());
                finalResults.get(mention.getCorefChain()).add(mention);
            }
        }

        finalResults.entrySet().removeIf(entry -> entry.getValue().size() == 1);

        System.out.println("Writing to file...");
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(new File("output/output.json")), "UTF-8"));
        writer.setIndent("  ");
        writer.beginArray();
        for (List<WikiLinksMention> mentions : finalResults.values()) {
            writer.beginArray();
            for(WikiLinksMention mention : mentions) {
                gson.toJson(mention, WikiLinksMention.class, writer);
            }
            writer.endArray();
        }
        writer.endArray();
        writer.close();
        System.out.println("Done, Total of-" + wikiLinksMentions.size() + " Mentions extracted\nwith total of-" +
                finalResults.keySet().size() + " Coref Chains");
    }
}
