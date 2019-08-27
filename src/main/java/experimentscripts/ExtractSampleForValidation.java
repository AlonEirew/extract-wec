package experimentscripts;

import persistence.SQLQueryApi;
import persistence.SQLiteConnections;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class ExtractSampleForValidation {
    public static void main(String[] args) throws SQLException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v7.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);
        SQLQueryApi sqlApi = new SQLQueryApi(sqLiteConnections);

        sqlApi.createTable(new ValidationMention());

        final Map<Integer, CorefResultSet> countPerType = getAllCorefs(sqLiteConnections);
        countPerType.entrySet().removeIf(entry -> entry.getValue().getMentions().size() < 3);

        List<ValidationMention> toCommit = fromMapToList(countPerType);

        commitCurrent(toCommit, sqlApi);

        System.out.println();
    }

    private static void commitCurrent(List<ValidationMention> localNewList, SQLQueryApi sqlApi) {
        System.out.println("Prepare to inset-" + localNewList.size() + " mentions to SQL");
        try {
            if (!sqlApi.insertRowsToTable(localNewList)) {
                System.out.println("Failed to insert mentions Batch!!!!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<ValidationMention> fromMapToList(Map<Integer, CorefResultSet> countPerType) {
        List<CorefResultSet>[] perType = new List[6];
        for(int i = 0 ; i < perType.length ; i++) {
            perType[i] = new ArrayList();
        }

        for(CorefResultSet resultSet : countPerType.values()) {
            switch (resultSet.getCorefType()) {
                case 2:
                    perType[0].add(resultSet);
                    break;
                case 3:
                    perType[1].add(resultSet);
                    break;
                case 4:
                    perType[2].add(resultSet);
                    break;
                case 6:
                    perType[3].add(resultSet);
                    break;
                case 7:
                    perType[4].add(resultSet);
                    break;
                case 8:
                    perType[5].add(resultSet);
                    break;
            }
        }

        List<ValidationMention> validationMentions = new ArrayList<>();
        for(int i = 0 ; i < perType.length ; i++) {
            Collections.shuffle(perType[i]);
            perType[i] = perType[i].stream().limit(10).collect(Collectors.toList());
            for(CorefResultSet resultSet : perType[i]) {
                for(MentionResultSet mentionResultSet : resultSet.getMentions()) {
                    validationMentions.add(new ValidationMention(mentionResultSet));
                }
            }
        }

        return validationMentions;
    }

    static Map<Integer, CorefResultSet> getAllCorefs(SQLiteConnections sqlConnection) throws SQLException {
        System.out.println("Preparing to select all coref mentions by types");
        Map<Integer, CorefResultSet> corefMap = new HashMap<>();
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            System.out.println("Preparing to extract");

            String query = "SELECT mentionId, coreChainId, mentionText, extractedFromPage, tokenStart, " +
                    "tokenEnd, corefValue, corefType, PartOfSpeech, context " +
                    "from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId " +
                    "where corefType in (2,3,4,6,7,8)";

            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                final int mentionId = rs.getInt("mentionId");
                final int corefId = rs.getInt("coreChainId");
                final String mentionText = rs.getString("mentionText").toLowerCase();
                final String extractedFromPage = rs.getString("extractedFromPage");
                final int tokenStart = rs.getInt("tokenStart");
                final int tokenEnd = rs.getInt("tokenEnd");
                final String partOfSpeech = rs.getString("PartOfSpeech");
                final int corefType = rs.getInt("corefType");
                final String corefValue = rs.getString("corefValue");
                final String context = rs.getString("context");

                final MentionResultSet mention = new MentionResultSet(corefId, mentionId, mentionText, extractedFromPage,
                        tokenStart, tokenEnd, context, partOfSpeech);

                if(!corefMap.containsKey(corefId)) {
                    final CorefResultSet corefResultSet = new CorefResultSet(corefId, corefType, corefValue);
                    corefResultSet.addMention(mention);
                    corefMap.put(corefId, corefResultSet);
                } else {
                    final CorefResultSet corefResultSet = corefMap.get(corefId);
                    corefResultSet.addNoneIntersectionUniqueMention(mention);
                }
            }
        }

        return corefMap;
    }
}
