package experimentscripts.wec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data.WECCoref;
import org.apache.commons.io.FileUtils;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleWikiLinksExpr {
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static void main(String[] args) throws IOException {
        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections("jdbc:sqlite:/Users/aeirew/workspace/DataBase/EnWikiLinks_v11.db"));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Map<String, Map<String, CorefResultSet>> allClusters = new HashMap<>();
        Map<String, CorefResultSet> corefByText = sqlApi.getAllCorefByText(WECCoref.TABLE_COREF);
        List<Future> submitted = new ArrayList<>();
        for(int i = 0 ; i < 10; i++) {
            String wikilinksFile = "/Users/aeirew/workspace/corpus/wiki/original/data-0000" + i + "-of-00010";
//        HashMap<String, String> links = readGoogleWikilinksFile(wikilinksFile);
//        readPage(wikilinksFile);
            submitted.add(pool.submit(() -> readNotExtendedData(corefByText, wikilinksFile, allClusters)));
        }

        for(Future<?> submit : submitted) {
            try {
                System.out.println("Waiting for thread to finish...");
                submit.get();
                System.out.println("Thread finished!");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        pool.shutdown();
        Map<String, CorefResultSet> mergedLinks = mergeFiles(allClusters);
        printStats(mergedLinks, "/Users/aeirew/workspace/cross-doc-coref/resources/wikilinks/wikilinks_full.json",
                "/Users/aeirew/workspace/cross-doc-coref/resources/wikilinks/wikilinks_3stem.json");
    }

    private static void printStats(Map<String, CorefResultSet> mergedLinks, String outputFileFull, String outputFileStem) throws IOException {
        int totalMentions = 0;
        int singletonClusters = 0;
        float totalUniqeMentionsInCluster = 0;

        Map<String, CorefResultSet> stemLinks = new HashMap<>();

        for (String corefId : mergedLinks.keySet()) {
            CorefResultSet corefResultSet = mergedLinks.get(corefId);
            CorefResultSet newCorefResultSet = new CorefResultSet(corefResultSet.getCorefId(), corefResultSet.getCorefType(), corefResultSet.getCorefValue());
            stemLinks.put(corefId, newCorefResultSet);
            CorefResultSet mentions = corefResultSet;
            if(mentions.getMentionsSize() == 1) {
                singletonClusters++;
            }
            totalMentions += mentions.getMentionsSize();
            totalUniqeMentionsInCluster += new HashSet<>(mentions.getMentions()).size();

            for(MentionResultSet mentionResultSet : corefResultSet.getMentions()) {
                newCorefResultSet.addNoneIntersectionUniqueMention(mentionResultSet, 3);
            }
        }

        float averageUniqueInCluster = (totalUniqeMentionsInCluster / mergedLinks.size());
        System.out.println("Total relevant clusters=" + mergedLinks.size());
        System.out.println("Total relevant mentions=" + totalMentions);

        System.out.println("From relevant clusters, singletons=" + singletonClusters);
        System.out.println("Average unique mentions in cluster=" + averageUniqueInCluster);

        System.out.println("OutputFile=" + outputFileFull);
        FileUtils.writeStringToFile(new File(outputFileFull), gson.toJson(mergedLinks), "UTF-8");

        System.out.println("OutputFileStem=" + outputFileStem);
        FileUtils.writeStringToFile(new File(outputFileStem), gson.toJson(stemLinks), "UTF-8");
    }

    private static Map<String, CorefResultSet> mergeFiles(Map<String, Map<String, CorefResultSet>> allClusters) {
        Map<String, CorefResultSet> mergedLinks = new HashMap<>();
        for(String file : allClusters.keySet()) {
            Map<String, CorefResultSet> fileClusters = allClusters.get(file);
            for(String corefId : fileClusters.keySet()) {
                CorefResultSet currentCorefResultSet = fileClusters.get(corefId);
                if(!mergedLinks.containsKey(corefId)) {
                    mergedLinks.put(corefId, new CorefResultSet(
                            currentCorefResultSet.getCorefId(), currentCorefResultSet.getCorefType(), currentCorefResultSet.getCorefValue()));
                }
                mergedLinks.get(corefId).addMentionsCollection(currentCorefResultSet.getMentions());
            }
        }
        return mergedLinks;
    }

    private static void readNotExtendedData(Map<String, CorefResultSet> corefByText, String wikilinksFile, Map<String, Map<String, CorefResultSet>> allClusters) {
        String fileName = wikilinksFile.substring(wikilinksFile.lastIndexOf("/"));
        System.out.println("Extracting from wikilink file-" + fileName);
        HashMap<String, CorefResultSet> links = new HashMap<>();
        BufferedReader reader;
        int totalMentionsInFile = 0;
        int releveantMentionsInFile = 0;
        Pattern p = Pattern.compile("MENTION\t([a-zA-Z0-9\\s]+)\t\\d+\thttp://en.wikipedia.org/wiki/(\\w+)");
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(wikilinksFile), StandardCharsets.UTF_8));
            String line;
            String url = null;
            while ((line = reader.readLine()) != null) {
                if(line.startsWith("URL")) {
                    url = line.split("\t")[1];
                    continue;
                }
                Matcher m = p.matcher(line);
                while (m.find()) {
                    totalMentionsInFile++;
                    String corefValue = m.group(2).replace("_", " ");
                    String mention = m.group(1);
                    if(corefByText.containsKey(corefValue)) {
                        CorefResultSet corefResultSet = corefByText.get(corefValue);
                        MentionResultSet mentionResultSet = new MentionResultSet(
                                corefResultSet.getCorefId(), mention, url, -1, -1, null, null);
                        releveantMentionsInFile++;
                        if (!links.containsKey(corefValue)) {
                            CorefResultSet mentList = new CorefResultSet(corefResultSet.getCorefId(), corefResultSet.getCorefType(), corefValue);
                            mentList.addMention(mentionResultSet);
                            links.put(corefValue, mentList);
                        } else {
                            links.get(corefValue).addMention(mentionResultSet);
                        }
                    }
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        allClusters.put(wikilinksFile, links);

        System.out.println("Total Mentions in " + fileName + ":" + totalMentionsInFile);
        System.out.println("Total relevant mentions in " + fileName + ":" + releveantMentionsInFile);
    }

    private static HashMap<String, String> readGoogleWikilinksFile(String wikilinksFile) {
        HashMap<String, String> links = new HashMap<>();
        BufferedReader reader;
        Pattern p = Pattern.compile("href=\"http://en.wikipedia.org/wiki/(\\w+)\"");
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(wikilinksFile), StandardCharsets.UTF_8));
            String line = null;
            while ((line = reader.readLine()) != null) {
                Matcher m = p.matcher(line);
                while (m.find()) {
                    if (!links.containsKey(m.group(1)))
                        links.put(m.group(1), m.group(1));
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return links;
    }

    private static String readPage(String wikilinksFile) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(wikilinksFile)));

            String line = null;
            boolean htmlTagFound = false;
            StringBuilder page = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if(line.contains("<body>") && !htmlTagFound) {
                    htmlTagFound = true;
                    page.append(line);
                }
                else if (htmlTagFound) {
                    page.append(line.trim());
                }

                if(line.contains("</body>") && htmlTagFound) {
                    parse(page.toString().trim());
                    page = new StringBuilder();
                    htmlTagFound = false;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void parse(String xml) {
        XMLEventReader reader = null;
        if(!xml.isEmpty()) {
            try {
                byte[] byteArray = xml.getBytes(StandardCharsets.UTF_8);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                reader = inputFactory.createXMLEventReader(inputStream);
//            XMLInputFactory factory = XMLInputFactory.newInstance();
//            XMLEventReader reader = factory.createXMLEventReader(inputStream);
                // Go over the xml element and search for <page> element
                while (reader.hasNext()) {
                    final XMLEvent event = reader.nextEvent();
                    if (event.isStartElement() && event.asStartElement().getName()
                            .getLocalPart().equals("p")) {
                        parsePage(reader);
                    }
                }

            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }
    }

    static void parsePage(final XMLEventReader reader) {
        String title = null;
        long id = -1;
        String text = null;
        String redirect = null;
        try {
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement() && event.asStartElement().getName()
                        .getLocalPart().equals("a")) {
                    final StartElement element = event.asStartElement();
                    if (element.getAttributeByName(new QName("href")) != null) {
                        String link = element.getAttributeByName(new QName("href")).getValue();
                        if (link.matches("http://en.wikipedia.org/wiki/(\\w+)\"")) {
                            String linkText = reader.getElementText();
                            System.out.println(link);
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
