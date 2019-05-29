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

        final Map[] countPerType = countClustersString(sqLiteConnections);
        createCountReport(countPerType);
        createUniqueStringReport(countPerType);
        createLevinshteinDistanceReport(countPerType);
        createMentionsIntersectionReport(countPerType);
    }

    private static void createCountReport(Map[] countPerType) {
        int[][] tableResult = createResultTable(countPerType);
        System.out.println(gson.toJson(tableResult) + " PURE");
    }

    private static void createUniqueStringReport(Map[] countPerType) {
        final Map[] clustersUniqueString = countClustersUniqueString(countPerType);
        int[][] tableResult = createResultTable(clustersUniqueString);
        System.out.println(gson.toJson(tableResult) + " UNIQUE");
    }

    private static void createLevinshteinDistanceReport(Map[] countPerType) {
        final Map[] clustersUniqueString = countClustersUniqueString(countPerType);
        final Map[] levSimilar = calcLevenshteinDistance(clustersUniqueString);
        int[][] tableResult = createResultTableMinusSimilar(clustersUniqueString, levSimilar);
        System.out.println(gson.toJson(tableResult) + " LEVINSHTEIN");
    }

    private static void createMentionsIntersectionReport(Map[] countPerType) {
        final Map[] clustersUniqueString = countClustersUniqueString(countPerType);
        final Map[] calcContains = calcContains(clustersUniqueString);
        int[][] tableResult = createResultTableMinusSimilar(clustersUniqueString, calcContains);
        System.out.println(gson.toJson(tableResult) + " NON-INTERSECTING");
    }

    private static Map[] calcLevenshteinDistance(Map[] countPerType) {
        Map[] clusterByTypeLevenCount = new Map[8];
        for(int i = 0 ; i < countPerType.length ; i++) {
            Map<Integer, Collection<String>> thisTypeCount = countPerType[i];
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

    private static Map[] calcContains(Map[] countPerType) {
        Map[] clusterByTypeLevenCount = new Map[8];
        for(int i = 0 ; i < countPerType.length ; i++) {
            Map<Integer, Collection<String>> thisTypeCount = countPerType[i];
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

    private static int[][] createResultTable(Map[] countPerType) {
        int[][] tableResult = new int[8][6];
        for(int i = 0 ; i < countPerType.length ; i++) {
            int[] resultTable = new int[6]; // represent ranges for table in presentation
            Map<Integer, Collection<String>> thisTypeCount = countPerType[i];
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

    private static int[][] createResultTableMinusSimilar(Map[] countPerType, Map[] similarByTypeMap) {
        int[][] tableResult = new int[8][6];
        for(int i = 0 ; i < countPerType.length ; i++) {
            int[] resultTable = new int[6]; // represent ranges for table in presentation
            Map<Integer, Collection<String>> thisTypeCount = countPerType[i];
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

    private static Map[] countClustersUniqueString(Map[] lowerStringByTypeMap) {
        Map[] retSetMap = new Map[TYPES_NUM];
        for (int i = 0; i < lowerStringByTypeMap.length; i++) {
            Map<Integer, List<String>> thisStringByType = lowerStringByTypeMap[i];
            Map<Integer, Set<String>> thisStringByTypeAsSetMap = new HashMap<>();
            for(int corefId : thisStringByType.keySet()) {
                final List<String> clusterStrings = thisStringByType.get(corefId);
                Set<String> clusterStringsSet = new HashSet<>(clusterStrings);
                thisStringByTypeAsSetMap.put(corefId, clusterStringsSet);
            }
            retSetMap[i] = thisStringByTypeAsSetMap;
        }

        return retSetMap;
    }

    private static Map[] countClustersString(SQLiteConnections sqlConnection) throws SQLException {
        Map[] countPerType = new Map[TYPES_NUM];
        System.out.println("Preparing to select all coref mentions by types");
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            for (int i = 0; i < countPerType.length; i++) {
                System.out.println("Preparing to extract unique text for type=" + (i+1));
                Map<Integer, List<String>> countMapString = new HashMap<>();

                String query = "SELECT coreChainId, mentionText from Mentions INNER JOIN " +
                        "CorefChains ON Mentions.coreChainId=CorefChains.corefId where corefType=" + (i+1);

                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    final String mentionText = rs.getString("mentionText").toLowerCase();
                    final int corefId = rs.getInt("coreChainId");
                    if(!countMapString.containsKey(corefId)) {
                        countMapString.put(corefId, new ArrayList<>());
                    }

                    countMapString.get(corefId).add(mentionText);
                }

                countPerType[i] = countMapString;
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
