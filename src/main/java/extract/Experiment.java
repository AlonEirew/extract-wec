package extract;

import com.google.gson.Gson;
import org.apache.commons.text.similarity.LevenshteinDistance;
import persistence.SQLiteConnections;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class Experiment {
    private static final int TYPES_NUM = 8;

    private static Gson gson = new Gson();

    public static void main(String[] args) throws IOException, SQLException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEvent_v5.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);

        final List<Map<Integer, CorefResultSet>> countPerType = countClustersString(sqLiteConnections);
        countWithinDocMentions(countPerType);
//        createCountReport(countPerType);
//        createUniqueStringReport(countPerType);
//        createLevinshteinDistanceReport(countPerType);
//        createMentionsIntersectionReport(countPerType);
    }

    private static void countWithinDocMentions(List<Map<Integer, CorefResultSet>> countPerType) {
        int[] wdPerType = new int[TYPES_NUM];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            Map<Integer, CorefResultSet> corefResultSetMap = countPerType.get(i);
            int wdCount = 0;
            for(CorefResultSet coreResultSet : corefResultSetMap.values()) {
                final Map<String, List<MentionResultSet>> withinDocCoref = coreResultSet.getWithinDocCoref();
                final List<MentionResultSet> duplicates = coreResultSet.countDuplicates();
                if(duplicates.size() > 0) {
                    System.out.println(gson.toJson(duplicates));
                }
                for(List<MentionResultSet> wdCorefs : withinDocCoref.values()) {
                    if(wdCorefs.size() > 1) {
                        wdCount += wdCorefs.size();
                    }
                }
            }

            wdPerType[i] = wdCount;
        }

        System.out.println(gson.toJson(wdPerType));
     }

    private static void createCountReport(List<Map<Integer, CorefResultSet>> corefResultsType) {
        List<Map<Integer, Collection<String>>> countPerType = new ArrayList<>();
        for(Map<Integer, CorefResultSet> map : corefResultsType) {
            Map<Integer, Collection<String>> newMap = new HashMap<>();
            for(int corefId : map.keySet()) {
                CorefResultSet corefResultSet = map.get(corefId);
                Collection<String> clusterMentions = corefResultSet.getMentionsAsStringList();
                newMap.put(corefId, clusterMentions);
            }

            countPerType.add(newMap);
        }
        int[] mentionsCountList = countMentions3_30Plus(countPerType);
        int[][] tableResult = createResultTable(countPerType);
        System.out.println(gson.toJson(tableResult) + " PURE");
        System.out.println("Mentions=" + gson.toJson(mentionsCountList));
    }

    private static void createUniqueStringReport(List<Map<Integer, CorefResultSet>> countPerType) {
        final List<Map<Integer, Collection<String>>> clustersUniqueString = countClustersUniqueString(countPerType);
        int[] mentionsCountList = countMentions3_30Plus(clustersUniqueString);
        int[][] tableResult = createResultTable(clustersUniqueString);
        System.out.println(gson.toJson(tableResult) + " UNIQUE");
        System.out.println("Mentions=" + gson.toJson(mentionsCountList));
    }

    private static void createLevinshteinDistanceReport(List<Map<Integer, CorefResultSet>> countPerType) {
        final List<Map<Integer, Collection<String>>> clustersUniqueString = countClustersUniqueString(countPerType);
        final Map[] levSimilar = calcLevenshteinDistance(clustersUniqueString);
        int[][] tableResult = createResultTableMinusSimilar(clustersUniqueString, levSimilar);
        System.out.println(gson.toJson(tableResult) + " LEVINSHTEIN");
    }

    private static void createMentionsIntersectionReport(List<Map<Integer, CorefResultSet>> countPerType) {
        final List<Map<Integer, Collection<String>>> clustersUniqueString = countClustersUniqueString(countPerType);
        final Map[] calcContains = calcContains(clustersUniqueString);
        int[][] tableResult = createResultTableMinusSimilar(clustersUniqueString, calcContains);
        System.out.println(gson.toJson(tableResult) + " NON-INTERSECTING");
    }

    private static Map[] calcLevenshteinDistance(List<Map<Integer, Collection<String>>> countPerType) {
        Map[] clusterByTypeLevenCount = new Map[8];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            Map<Integer, Collection<String>> thisTypeCount = countPerType.get(i);
            Map<Integer, Integer> clusterLevenshteinCount = new HashMap<>();
            for(int corefId : thisTypeCount.keySet()) {
                Collection<String> clusterMentionsSet = thisTypeCount.get(corefId);

                int similar = 0;
                if(clusterMentionsSet.size() == 1) {
                    clusterLevenshteinCount.put(corefId, similar);
                    continue;
                }

                ArrayList<String> curSetAsList = new ArrayList<>();
                curSetAsList.addAll(clusterMentionsSet);
                for(int x = 0 ; x < clusterMentionsSet.size() ; x++) {
                    for(int y = x + 1; y < clusterMentionsSet.size() ; y++ ) {
                        if(x != y) {
                            final Integer apply = LevenshteinDistance.getDefaultInstance()
                                    .apply(curSetAsList.get(x), curSetAsList.get(y));
                            if(apply <= 2) {
                                similar++;
                            }
                        }
                    }
                }

                clusterLevenshteinCount.put(corefId, similar);
            }

            clusterByTypeLevenCount[i] = clusterLevenshteinCount;
        }

        return clusterByTypeLevenCount;
    }

    private static Map[] calcContains(List<Map<Integer, Collection<String>>> countPerType) {
        Map[] clusterByTypeLevenCount = new Map[8];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            Map<Integer, Collection<String>> thisTypeCount = countPerType.get(i);
            Map<Integer, Integer> clusterLevenshteinCount = new HashMap<>();
            for(int corefId : thisTypeCount.keySet()) {
                Collection<String> clusterMentionsSet = thisTypeCount.get(corefId);

                int similar = 0;
                if(clusterMentionsSet.size() == 1) {
                    clusterLevenshteinCount.put(corefId, similar);
                    continue;
                }

                ArrayList<String> curSetAsList = new ArrayList<>();
                curSetAsList.addAll(clusterMentionsSet);
                for(int x = 0 ; x < clusterMentionsSet.size() ; x++) {
                    for(int y = x + 1; y < clusterMentionsSet.size() ; y++ ) {
                        if(x != y) {
                            List<String> m1 = new ArrayList<>(Arrays.asList(curSetAsList.get(x).split("\\s")));
                            List<String> m2 = new ArrayList<>(Arrays.asList(curSetAsList.get(y).split("\\s")));
                            m1.retainAll(m2);
                            if(m1.size() >= 1) {
                                similar++;
                            }
                        }
                    }
                }

                clusterLevenshteinCount.put(corefId, similar);
            }

            clusterByTypeLevenCount[i] = clusterLevenshteinCount;
        }

        return clusterByTypeLevenCount;
    }

    private static int[] countMentions3_30Plus(List<Map<Integer, Collection<String>>> countPerType) {
        int[] mentionCountsList = new int[TYPES_NUM];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            Map<Integer, Collection<String>> thisTypeCount = countPerType.get(i);
            int clusterMentionCount = 0;
            for(Collection<String> curSet : thisTypeCount.values()) {
                if(curSet.size() >= 3) {
                    clusterMentionCount += curSet.size();
                }
            }

            mentionCountsList[i] = clusterMentionCount;
        }
        return mentionCountsList;
    }

    private static int[][] createResultTable(List<Map<Integer, Collection<String>>> countPerType) {
        int[][] tableResult = new int[TYPES_NUM][6];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            int[] resultTable = new int[6]; // represent ranges for table in presentation
            Map<Integer, Collection<String>> thisTypeCount = countPerType.get(i);
            for(Collection<String> curSet : thisTypeCount.values()) {
                int count = curSet.size();
                if(count == 1) {
                    resultTable[0]++;
                } else if(count == 2) {
                    resultTable[1]++;
                } else if(count >= 3 && count <= 5) {
                    resultTable[2]++;
                } else if(count >= 6 && count <= 10) {
                    resultTable[3]++;
                } else if(count >= 11 && count <= 30) {
                    resultTable[4]++;
                } else {
                    resultTable[5]++;
                }
            }

            tableResult[i] = resultTable;
        }
        return tableResult;
    }

    private static int[][] createResultTableMinusSimilar(List<Map<Integer, Collection<String>>> countPerType, Map[] similarByTypeMap) {
        int[][] tableResult = new int[TYPES_NUM][6];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            int[] resultTable = new int[6]; // represent ranges for table in presentation
            Map<Integer, Collection<String>> thisTypeCount = countPerType.get(i);
            Map<Integer, Integer> similarityMap = similarByTypeMap[i];
            for(int corefId : thisTypeCount.keySet()) {
                Collection<String> curSet = thisTypeCount.get(corefId);
                int count = curSet.size();
                count = count - similarityMap.get(corefId);

                if(count <= 1) {
                    resultTable[0]++;
                } else if(count == 2) {
                    resultTable[1]++;
                } else if(count >= 3 && count <= 5) {
                    resultTable[2]++;
                } else if(count >= 6 && count <= 10) {
                    resultTable[3]++;
                } else if(count >= 11 && count <= 30) {
                    resultTable[4]++;
                } else {
                    resultTable[5]++;
                }
            }

            tableResult[i] = resultTable;
        }
        return tableResult;
    }

    private static List<Map<Integer, Collection<String>>> countClustersUniqueString(List<Map<Integer, CorefResultSet>> lowerStringByTypeMap) {
        List<Map<Integer, Collection<String>>> countPerType = new ArrayList<>();
        for(Map<Integer, CorefResultSet> map : lowerStringByTypeMap) {
            Map<Integer, Collection<String>> newMap = new HashMap<>();
            for(int corefId : map.keySet()) {
                CorefResultSet corefResultSet = map.get(corefId);
                Collection<String> clusterMentions = corefResultSet.getMentionsAsSet();
                newMap.put(corefId, clusterMentions);
            }

            countPerType.add(newMap);
        }

        return countPerType;
    }

    private static List<Map<Integer, CorefResultSet>> countClustersString(SQLiteConnections sqlConnection) throws SQLException {
        ArrayList<Map<Integer, CorefResultSet>> countPerType = new ArrayList<>();
        System.out.println("Preparing to select all coref mentions by types");
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            for (int i = 0 ; i < TYPES_NUM ; i++) {
                System.out.println("Preparing to extract unique text for type=" + (i+1));
                Map<Integer, CorefResultSet> countMapString = new HashMap<>();

                String query = "SELECT coreChainId, mentionText, extractedFromPage, tokenStart, tokenEnd, context from Mentions INNER JOIN " +
                        "CorefChains ON Mentions.coreChainId=CorefChains.corefId where corefType=" + (i+1);

                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    final int corefId = rs.getInt("coreChainId");
                    final String mentionText = rs.getString("mentionText").toLowerCase();
                    final String extractedFromPage = rs.getString("extractedFromPage");
                    final int tokenStart = rs.getInt("tokenStart");
                    final int tokenEnd = rs.getInt("tokenEnd");
                    final String context = rs.getString("context");
                    if(!countMapString.containsKey(corefId)) {
                        countMapString.put(corefId, new CorefResultSet(corefId));
                    }

                    countMapString.get(corefId).addMention(new MentionResultSet(corefId, mentionText, extractedFromPage,
                            tokenStart, tokenEnd, context));
                }

                countPerType.add(i, countMapString);
                System.out.println("Done extracting for type=" + (i+1));
            }
        }

        return countPerType;
    }

    private static List[] selectCorefData(SQLiteConnections sqlConnection) throws SQLException {
        List[] corefIdsPerType = new List[TYPES_NUM];
        System.out.println("Preparing to select all coref by types");
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            for (int i = 0; i < corefIdsPerType.length; i++) {
                List<Integer> corefIds = new ArrayList<>();
                System.out.println("Getting type=" + (i+1));
                String query = "Select corefId from CorefChains WHERE corefType=" + (i+1) + ";";
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    corefIds.add(rs.getInt("corefId"));
                }
                corefIdsPerType[i] = corefIds;
                System.out.println("Done with type=" + (i+1));
            }
        }

        return corefIdsPerType;
    }
}
