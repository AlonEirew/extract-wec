package experimentscripts;

import com.google.gson.Gson;
import persistence.SQLiteConnections;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckNominalization {

    private static Gson gson = new Gson();

    public static void main(String[] args) throws SQLException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v6.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);

        final List<MentionResultSet> mentionResultSets = countVerbs(sqLiteConnections);
        final Map<Integer, AtomicInteger> clusterView = createClusterView(mentionResultSets);
        final int[] resultTableForStringAmongClusters = Experiment.createResultTableForStringAmongClusters(clusterView);
        System.out.println(gson.toJson(resultTableForStringAmongClusters) + "UNIQUE");

//        Collections.sort(mentionResultSets,  Comparator.comparingInt(MentionResultSet::getCorefId));
//
//        System.out.println("Verbs Found=" + mentionResultSets.size());
//
//        System.out.println(gson.toJson(mentionResultSets));
    }

    private static Map<Integer, AtomicInteger> createClusterView(List<MentionResultSet> mentionResultSets) {
        Map<Integer, AtomicInteger> clusterCount = new HashMap<>();
        for (MentionResultSet ment : mentionResultSets) {
            if(clusterCount.containsKey(ment.getCorefId())) {
                clusterCount.get(ment.getCorefId()).incrementAndGet();
            } else {
                clusterCount.put(ment.getCorefId(), new AtomicInteger(1));
            }
        }

        return clusterCount;
    }

    private static List<MentionResultSet> countVerbs(SQLiteConnections sqlConnection) throws
    SQLException {
        System.out.println("Preparing to select all coref mentions by types");
        List<MentionResultSet> results = new ArrayList<>();
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            System.out.println("Preparing to experimentscripts unique text for type=");
            Map<Integer, CorefResultSet> countMapString = new HashMap<>();

            String query = "SELECT coreChainId, mentionText, extractedFromPage, tokenStart, tokenEnd, PartOfSpeech " +
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

                String[] mentionTokens = mentionText.split(" ");
                String[] posList = partOfSpeech.split(",");

                if(mentionTokens.length == 1) {
                    if(posList[0].matches("VB[DGNPZ]?")) {
                        MentionResultSet ment = new MentionResultSet(corefId, mentionText, extractedFromPage,
                                tokenStart, tokenEnd, null, partOfSpeech);
                        results.add(ment);
                    }
                    continue;
                }

                for(int i = 0, j=0 ; i < mentionTokens.length ; i++, j++) {
                    try {
                        if(mentionTokens[i].isEmpty()) {
                            i++;
                        }

                        if (posList[j].matches("VB[DGNPZ]?") && !Character.isUpperCase(mentionTokens[i].charAt(0))) {
                            MentionResultSet ment = new MentionResultSet(corefId, mentionText, extractedFromPage,
                                    tokenStart, tokenEnd, null, partOfSpeech);
                            results.add(ment);
                        }
                    } catch(ArrayIndexOutOfBoundsException ex) {
                        break;
                    } catch(StringIndexOutOfBoundsException ex) {
                        System.out.println();
                    }
                }
            }
        }

        return results;
    }
}
