package experimentscripts;

import com.google.gson.Gson;
import persistence.SQLiteConnections;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Experiment {
    private static final int TYPES_START = 2;
    private static final int TYPES_END = 8;

    private static Gson gson = new Gson();

    public static void main(String[] args) throws SQLException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v5.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);

        final List<Map<Integer, CorefResultSet>> countPerType = countClustersString(sqLiteConnections);
        final List<Map<Integer, CorefResultSet>> clustersUniqueString = countClustersUniqueString(countPerType);

        final Map<String, AtomicInteger> exactStringAmongClusters = countExactStringAmongClusters(clustersUniqueString);
        final int[] resultTableForStringAmongClusters = createResultTableForStringAmongClusters(exactStringAmongClusters);
        System.out.println(gson.toJson(resultTableForStringAmongClusters) + "UNIQUE");
//
//        printResults(countPerType, "PURE");
//        printResults(clustersUniqueString, "UNIQUE");
//
//        createLevinshteinDistanceReport(clustersUniqueString);
//        createMentionsIntersectionReport(clustersUniqueString);
//        createMentionsLevinshteinAndIntersectionReport(clustersUniqueString);
    }

    private static void createLevinshteinDistanceReport(List<Map<Integer, CorefResultSet>> clustersUniqueString) {
        final List<Map<Integer, CorefResultSet>> levSimilar = calcLevenshteinDistance(clustersUniqueString);
        printResults(levSimilar, "LEVINSHTEIN");
    }

    private static void createMentionsIntersectionReport(List<Map<Integer, CorefResultSet>> clustersUniqueString) {
        final List<Map<Integer, CorefResultSet>> calcContains = calcIntersecting(clustersUniqueString);
        printResults(calcContains, "NON-INTERSECTING");
    }

    private static void createMentionsLevinshteinAndIntersectionReport(List<Map<Integer, CorefResultSet>> clustersUniqueString) {
        final List<Map<Integer, CorefResultSet>> levSimilar = calcLevenshteinDistance(clustersUniqueString);
        final List<Map<Integer, CorefResultSet>> calcContains = calcIntersecting(levSimilar);
        printResults(calcContains, "LEVINSHTEIN+INTERSECTING");
    }

    private static Map<String, AtomicInteger> countExactStringAmongClusters(List<Map<Integer, CorefResultSet>> clustersUniqueString) {
        Map<String, AtomicInteger> uniqueStringAmongClusterCount = new HashMap<>();
        for(int i = 0 ; i < clustersUniqueString.size() ; i++) {
            Map<Integer, CorefResultSet> corefResultSetMap = clustersUniqueString.get(i);
            for (CorefResultSet coreResultSet : corefResultSetMap.values()) {
                final List<MentionResultSet> mentions = coreResultSet.getMentions();
                for (MentionResultSet ment : mentions) {
                    if(uniqueStringAmongClusterCount.containsKey(ment.getMentionString())) {
                        uniqueStringAmongClusterCount.get(ment.getMentionString()).incrementAndGet();
                    } else {
                        uniqueStringAmongClusterCount.put(ment.getMentionString(), new AtomicInteger(1));
                    }
                }
            }
        }

        return uniqueStringAmongClusterCount;
    }

    private static void printResults(List<Map<Integer, CorefResultSet>> resultsToPrint, String message) {
        int[] mentionsCountList = countMentions1_30Plus(resultsToPrint);
        int[][] tableResult = createResultTable(resultsToPrint);

        countWithinDocMentions(resultsToPrint);
        System.out.println(gson.toJson(tableResult) + " " + message);
        System.out.println(message + " Mentions=" + gson.toJson(mentionsCountList));
    }

    private static void countWithinDocMentions(List<Map<Integer, CorefResultSet>> countPerType) {
        int[] wdPerType = new int[TYPES_END];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            Map<Integer, CorefResultSet> corefResultSetMap = countPerType.get(i);
            int wdCount = 0;
            for(CorefResultSet coreResultSet : corefResultSetMap.values()) {
                final Map<String, List<MentionResultSet>> withinDocCoref = coreResultSet.getWithinDocCoref();
//                final List<MentionResultSet> duplicates = coreResultSet.countDuplicates();
//                if(duplicates.size() > 0) {
//                    System.out.println(gson.toJson(duplicates));
//                }
                for(List<MentionResultSet> wdCorefs : withinDocCoref.values()) {
                    if(wdCorefs.size() > 1) {
                        wdCount += wdCorefs.size();
                    }
                }
            }

            wdPerType[i] = wdCount;
        }

        System.out.println("WD MENTIONS" + gson.toJson(wdPerType));
    }

    private static List<Map<Integer, CorefResultSet>> calcLevenshteinDistance(List<Map<Integer, CorefResultSet>> clustersUniqueString) {
        List<Map<Integer, CorefResultSet>> clusterByTypeLevenCount = new ArrayList<>();
        for(int i = 0 ; i < clustersUniqueString.size() ; i++) {
            Map<Integer, CorefResultSet> thisTypeCount = clustersUniqueString.get(i);
            Map<Integer, CorefResultSet> clusterLevenshteinCount = new HashMap<>();

            for(int corefId : thisTypeCount.keySet()) {
                CorefResultSet clusterMentionsSet = thisTypeCount.get(corefId);
                CorefResultSet levDistCluster = clusterMentionsSet.getLevenshteinDistanceMentions();
                clusterLevenshteinCount.put(corefId, levDistCluster);
            }

            clusterByTypeLevenCount.add(clusterLevenshteinCount);
        }

        return clusterByTypeLevenCount;
    }

    private static List<Map<Integer, CorefResultSet>> calcIntersecting(List<Map<Integer, CorefResultSet>> clustersUniqueString) {
        List<Map<Integer, CorefResultSet>> clusterByTypeLevenCount = new ArrayList<>();
        for(int i = 0 ; i < clustersUniqueString.size() ; i++) {
            Map<Integer, CorefResultSet> thisTypeCount = clustersUniqueString.get(i);
            Map<Integer, CorefResultSet> clusterLevenshteinCount = new HashMap<>();

            for(int corefId : thisTypeCount.keySet()) {
                CorefResultSet clusterMentionsSet = thisTypeCount.get(corefId);
                CorefResultSet levDistCluster = clusterMentionsSet.getNonIntersectingMentions();
                clusterLevenshteinCount.put(corefId, levDistCluster);
            }

            clusterByTypeLevenCount.add(clusterLevenshteinCount);
        }

        return clusterByTypeLevenCount;
    }

    private static int[] countMentions1_30Plus(final List<Map<Integer, CorefResultSet>> countPerType) {
        int[] mentionCountsList = new int[TYPES_END];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            Map<Integer, CorefResultSet> thisTypeCount = countPerType.get(i);
            int clusterMentionCount = 0;
            for(CorefResultSet coref : thisTypeCount.values()) {
                int corefSize = coref.getMentions().size();
                if(corefSize >= 3) {
                    clusterMentionCount += corefSize;
                }
            }

            mentionCountsList[i] = clusterMentionCount;
        }
        return mentionCountsList;
    }

    private static int[][] createResultTable(final List<Map<Integer, CorefResultSet>> countPerType) {
        int[][] tableResult = new int[TYPES_END][6];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            int[] resultTable = new int[6]; // represent ranges for table in presentation
            Map<Integer, CorefResultSet> thisTypeCount = countPerType.get(i);
            for(CorefResultSet coref : thisTypeCount.values()) {
                countTableColumnValues(resultTable, coref.getMentions().size());
            }

            tableResult[i] = resultTable;
        }
        return tableResult;
    }

    static <T> int[] createResultTableForStringAmongClusters(final Map<T, AtomicInteger> stringAmongClusterCountByType) {
        int[] resultTable = new int[6]; // represent ranges for table in presentation
        for(AtomicInteger stringCount : stringAmongClusterCountByType.values()) {
            countTableColumnValues(resultTable, stringCount.get());
        }

        return resultTable;
    }

    private static void countTableColumnValues(int[] resultTable, int count) {
        if (count == 1) {
            resultTable[0]++;
        } else if (count == 2) {
            resultTable[1]++;
        } else if (count >= 3 && count <= 5) {
            resultTable[2]++;
        } else if (count >= 6 && count <= 10) {
            resultTable[3]++;
        } else if (count >= 11 && count <= 30) {
            resultTable[4]++;
        } else {
            resultTable[5]++;
        }
    }

    private static List<Map<Integer, CorefResultSet>> countClustersUniqueString(List<Map<Integer, CorefResultSet>> lowerStringByTypeMap) {
        List<Map<Integer, CorefResultSet>> countPerType = new ArrayList<>();
        for(Map<Integer, CorefResultSet> map : lowerStringByTypeMap) {
            Map<Integer, CorefResultSet> newMap = new HashMap<>();
            for(int corefId : map.keySet()) {
                CorefResultSet corefResultSet = map.get(corefId);
                CorefResultSet clusterMentions = corefResultSet.getMentionsOnlyUniques();
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
            for (int i = TYPES_START; i <= TYPES_END; i++) {
                System.out.println("Preparing to experimentscripts unique text for type=" + i);
                Map<Integer, CorefResultSet> countMapString = new HashMap<>();

                String query = "SELECT coreChainId, mentionText, extractedFromPage, tokenStart, tokenEnd from Mentions INNER JOIN " +
                        "CorefChains ON Mentions.coreChainId=CorefChains.corefId where corefType=" + i;

                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    final int corefId = rs.getInt("coreChainId");
                    final String mentionText = rs.getString("mentionText").toLowerCase();
                    final String extractedFromPage = rs.getString("extractedFromPage");
                    final int tokenStart = rs.getInt("tokenStart");
                    final int tokenEnd = rs.getInt("tokenEnd");
                    final String context = null; //rs.getString("context");
                    if(!countMapString.containsKey(corefId)) {
                        countMapString.put(corefId, new CorefResultSet(corefId));
                    }

                    countMapString.get(corefId).addMention(new MentionResultSet(corefId, mentionText, extractedFromPage,
                            tokenStart, tokenEnd, context, null));
                }

                countPerType.add(countMapString);
                System.out.println("Done extracting for type=" + i);
            }
        }

        return countPerType;
    }

    private static List[] selectCorefData(SQLiteConnections sqlConnection) throws SQLException {
        List[] corefIdsPerType = new List[TYPES_END];
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

    private static int[][] createResultTableMinusSimilar(List<Map<Integer, CorefResultSet>> countPerType, Map[] similarByTypeMap) {
        int[][] tableResult = new int[TYPES_END][6];
        for(int i = 0 ; i < countPerType.size() ; i++) {
            int[] resultTable = new int[6]; // represent ranges for table in presentation
            Map<Integer, CorefResultSet> thisTypeCount = countPerType.get(i);
            Map<Integer, Integer> similarityMap = similarByTypeMap[i];
            for(int corefId : thisTypeCount.keySet()) {
                CorefResultSet curSet = thisTypeCount.get(corefId);
                int count = curSet.getMentions().size();
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
}
