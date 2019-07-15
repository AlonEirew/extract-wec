package experimentscripts;

import com.google.gson.Gson;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import persistence.SQLiteConnections;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class ExtractTopicsInfo {
    private static Gson gson = new Gson();

    public static void main(String[] args) throws SQLException, IOException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v6.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);

        final Map<Integer, CorefResultSet> allCorefs = getAllCorefs(sqLiteConnections);
        final Map<String, List<MentionResultSet>> topicsMap = countTopicsMentions(allCorefs);

        List<Pair<String, Integer>> printTopicsBySize = new ArrayList<>();
        for(String key : topicsMap.keySet()) {
            final int size = topicsMap.get(key).size();
            if(size > 1) {
                printTopicsBySize.add(new Pair<>(key, size));
            }
        }

        Collections.sort(printTopicsBySize, Comparator.comparingInt(Pair::getValue));

        FileUtils.writeLines(new File("output/topics.txt"), "UTF-8", printTopicsBySize);
    }

    private static Map<String, List<MentionResultSet>> countTopicsMentions(Map<Integer, CorefResultSet> allCorefs) {
        Map<String, List<MentionResultSet>> topics = new HashMap<>();
        for(CorefResultSet corefResultSet : allCorefs.values()) {
            for(MentionResultSet mentionResultSet : corefResultSet.getMentions()) {
                if(!topics.containsKey(mentionResultSet.getExtractedFromPage())) {
                    topics.put(mentionResultSet.getExtractedFromPage(), new ArrayList<>());
                }

                topics.get(mentionResultSet.getExtractedFromPage()).add(mentionResultSet);
            }
        }

        return topics;
    }

    private static Map<Integer, CorefResultSet> getAllCorefs(SQLiteConnections sqlConnection) throws SQLException {
        System.out.println("Preparing to select all coref mentions by types");
        Map<Integer, CorefResultSet> corefMap = new HashMap<>();
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            System.out.println("Preparing to extract");

            String query = "SELECT coreChainId, mentionText, extractedFromPage, tokenStart, tokenEnd, PartOfSpeech, context " +
                    "from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId " +
                    "where corefType>=2 and corefType<=8";

            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                final int corefId = rs.getInt("coreChainId");
                final String mentionText = rs.getString("mentionText").toLowerCase();
                final String extractedFromPage = rs.getString("extractedFromPage");
                final int tokenStart = rs.getInt("tokenStart");
                final int tokenEnd = rs.getInt("tokenEnd");
                final String partOfSpeech = rs.getString("PartOfSpeech");
                final String context = rs.getString("context");

                if(!corefMap.containsKey(corefId)) {
                    corefMap.put(corefId, new CorefResultSet(corefId));
                }

                corefMap.get(corefId).addMention(new MentionResultSet(corefId, mentionText, extractedFromPage,
                        tokenStart, tokenEnd, context, partOfSpeech));
            }
        }

        return corefMap;
    }
}
