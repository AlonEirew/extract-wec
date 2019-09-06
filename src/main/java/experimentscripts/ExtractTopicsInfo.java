package experimentscripts;

import com.google.gson.Gson;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLiteConnections;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class ExtractTopicsInfo {
    private final static Logger LOGGER = LogManager.getLogger(ExtractTopicsInfo.class);

    public static void main(String[] args) throws SQLException, IOException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v7.db";
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

        FileUtils.writeLines(new File("output/topics2.txt"), "UTF-8", printTopicsBySize);
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

    static Map<Integer, CorefResultSet> getAllCorefs(SQLiteConnections sqlConnection) throws SQLException {
        LOGGER.info("Preparing to select all coref mentions by types");
        Map<Integer, CorefResultSet> corefMap = new HashMap<>();
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.info("Preparing to extract");

            String query = "SELECT coreChainId, mentionText, extractedFromPage, tokenStart, " +
                    "tokenEnd, corefValue, corefType, PartOfSpeech, context " +
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
                final int corefType = rs.getInt("corefType");
                final String corefValue = rs.getString("corefValue");

                if(!corefMap.containsKey(corefId)) {
                    corefMap.put(corefId, new CorefResultSet(corefId, corefType, corefValue));
                }

                corefMap.get(corefId).addMention(new MentionResultSet(corefId, mentionText, extractedFromPage,
                        tokenStart, tokenEnd, context, partOfSpeech));
            }
        }

        return corefMap;
    }
}
