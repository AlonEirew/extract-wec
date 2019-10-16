package experimentscripts;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class ExtractSampleForValidation {
    private final static Logger LOGGER = LogManager.getLogger(ExtractSampleForValidation.class);

    private static final int CLUSTER_SIZE_LIMIT = 1;
    private static final int MAX_CLUSTER_NONE_UNIQE = 3;

    public static void main(String[] args) throws SQLException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v9.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);
        SQLQueryApi sqlApi = new SQLQueryApi(sqLiteConnections);

        sqlApi.createTable(new ValidationMention());

        final Map<Integer, CorefResultSet> countPerType = getAllCorefs(sqLiteConnections);
        countPerType.entrySet().removeIf(entry -> entry.getValue().getMentions().isEmpty());
        countPerType.entrySet().removeIf(entry -> entry.getValue().getCorefType() == 6 &&
                entry.getValue().getMentions().size() < 3);

        List<ValidationMention> toCommit = fromMapToList(countPerType);

        commitCurrent(toCommit, sqlApi);

        LOGGER.info("");
    }

    private static void commitCurrent(List<ValidationMention> localNewList, SQLQueryApi sqlApi) {
        LOGGER.info("Prepare to inset-" + localNewList.size() + " mentions to SQL");
        try {
            if (!sqlApi.insertRowsToTable(localNewList)) {
                LOGGER.error("Failed to insert mentions Batch!!!!");
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
    }

    private static List<ValidationMention> fromMapToList(Map<Integer, CorefResultSet> countPerType) {
        List<ValidationMention> validationMentions = new ArrayList<>();
        int marker = 0;
        ValidationMention.SPLIT split;
        List<CorefResultSet> corefResultSetList = new ArrayList<>(countPerType.values());
        Collections.shuffle(corefResultSetList);
        for(CorefResultSet corefResultSet : corefResultSetList) {
            if(marker < 3000) {
                split = ValidationMention.SPLIT.VALIDATION;
            } else if(marker >= 3000 && marker < 6000) {
                split = ValidationMention.SPLIT.TEST;
            } else {
                split = ValidationMention.SPLIT.TRAIN;
            }

            for(MentionResultSet mentionResultSet : corefResultSet.getMentions()) {
                validationMentions.add(new ValidationMention(mentionResultSet, split));
            }
            marker ++;
        }

        return validationMentions;
    }

    static Map<Integer, CorefResultSet> getAllCorefs(SQLiteConnections sqlConnection) throws SQLException {
        LOGGER.info("Preparing to select all coref mentions by types");
        Map<Integer, CorefResultSet> corefMap = new HashMap<>();
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.info("Preparing to extract");

            String query = "SELECT mentionId, coreChainId, mentionText, extractedFromPage, tokenStart, " +
                    "tokenEnd, corefValue, corefType, PartOfSpeech, context " +
                    "from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId " +
                    "where corefType in (2,3,4,6,7,8)";

            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                final int mentionId = rs.getInt("mentionId");
                final int corefId = rs.getInt("coreChainId");
                final String mentionText = rs.getString("mentionText");
                final String extractedFromPage = rs.getString("extractedFromPage");
                final int tokenStart = rs.getInt("tokenStart");
                final int tokenEnd = rs.getInt("tokenEnd");
                final String partOfSpeech = rs.getString("PartOfSpeech");
                final int corefType = rs.getInt("corefType");
                final String corefValue = rs.getString("corefValue");
                final String context = rs.getString("context");

                if(mentionText.trim().isEmpty() || mentionText.matches("\\d+") || !mentionText.matches("[A-Za-z0-9\\s]+") ||
                        mentionText.equals(corefValue)) {
                    continue;
                }

                final MentionResultSet mention = new MentionResultSet(corefId, mentionId, mentionText, extractedFromPage,
                        tokenStart, tokenEnd, context, partOfSpeech);

                if(!corefMap.containsKey(corefId)) {
                    final CorefResultSet corefResultSet = new CorefResultSet(corefId, corefType, corefValue);
                    corefResultSet.addMention(mention);
                    corefMap.put(corefId, corefResultSet);
                } else {
                    final CorefResultSet corefResultSet = corefMap.get(corefId);
                    corefResultSet.addNoneIntersectionUniqueMention(mention, MAX_CLUSTER_NONE_UNIQE);
                }
            }
        }

        return corefMap;
    }
}
