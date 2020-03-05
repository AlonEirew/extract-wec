package experimentscripts;

import data.CorefType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleWikiLinksExpr {
    public static void main(String[] args) {
        String wikilinksFile = "/Users/aeirew/Downloads/data-00001-of-00010";
//        HashMap<String, String> links = readGoogleWikilinksFile(wikilinksFile);
//        readPage(wikilinksFile);
        readNotExtendedData(wikilinksFile);
    }

    private static void readNotExtendedData(String wikilinksFile) {
        SQLQueryApi sqlApi = new SQLQueryApi(new SQLiteConnections("jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksExperiment.db"));
        HashMap<String, Set<String>> links = new HashMap<>();
        BufferedReader reader;
        int totalMentionsInFile = 0;
        int releveantMentionsInFile = 0;
        Pattern p = Pattern.compile("MENTION\t([a-zA-Z0-9\\s]+)\t\\d+\thttp://en.wikipedia.org/wiki/(\\w+)");
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(wikilinksFile), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = p.matcher(line);
                while (m.find()) {
                    totalMentionsInFile++;
//                    System.out.println("Evaluated-" + totalMentionsInFile);
                    String corefId = m.group(2).replace("_", " ");
                    WECCoref corefByText = sqlApi.getCorefByText(corefId, WECCoref.TABLE_COREF);
                    if (corefByText.getCorefType() != null && corefByText.getCorefType() != CorefType.NA &&
                            corefByText.getCorefType() != CorefType.PERSON && corefByText.getCorefType() != CorefType.ELECTION_EVENT &&
                            corefByText.getCorefType() != CorefType.EVENT_UNK) {

                        releveantMentionsInFile++;
                        System.out.println("Found Relevent mention-" + corefId + ", " + m.group(1));
                        if (!links.containsKey(m.group(1))) {
                            Set<String> set = new HashSet<>();
                            set.add(m.group(1));
                            links.put(corefId, set);
                        } else {
                            links.get(corefId).add(m.group(1));
                        }
                    }
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total Mentions in file=" + totalMentionsInFile);
        System.out.println("Total relevant mentions=" + releveantMentionsInFile);
        System.out.println("Total relevant clusters=" + links.size());
        System.out.println("Mentions:");
        for (String corefId : links.keySet()) {
            System.out.println("Mention CorefValue=" + corefId);
            for (String corefValue : links.get(corefId)) {
                System.out.println("\t" + corefValue);
            }
        }
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
