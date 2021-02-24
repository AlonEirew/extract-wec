package scripts.experiment;

import com.google.gson.Gson;
import scripts.wec.resultsets.CorefResultSet;
import scripts.wec.resultsets.MentionResultSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLiteConnections;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedAmongClusters {
    private final static Logger LOGGER = LogManager.getLogger(SharedAmongClusters.class);
    private final static Gson GSON = new Gson();

    public static void main(String[] args) throws SQLException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v7.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);

        final List<Map<Integer, CorefResultSet>> countPerType = Experiment.extractClustersString(sqLiteConnections);
        final List<Map<Integer, CorefResultSet>> clustersUniqueString = Experiment.countClustersUniqueString(countPerType);
        final Map<String, AtomicInteger> exactStringAmongClusters = countExactStringAmongClusters(clustersUniqueString);

        final int[] resultTableForStringAmongClusters = createResultTableForStringAmongClusters(exactStringAmongClusters);
        LOGGER.info(GSON.toJson(resultTableForStringAmongClusters) + "UNIQUE");

        final List<Map<Integer, CorefResultSet>> mapOnlyTextual1 = countClustersOnlyTextual(clustersUniqueString, true);
        final Map<String, AtomicInteger> onlyTextualAmongClusters1 = countExactStringAmongClusters(mapOnlyTextual1);

        final int[] resultTableForTextualOnlyAmongClusters1 = createResultTableForStringAmongClusters(onlyTextualAmongClusters1);
        LOGGER.info(GSON.toJson(resultTableForTextualOnlyAmongClusters1) + "Textual (without Numbers)");

        final List<Map<Integer, CorefResultSet>> mapOnlyTextual2 = countClustersOnlyTextual(clustersUniqueString, false);
        final Map<String, AtomicInteger> onlyTextualAmongClusters2 = countExactStringAmongClusters(mapOnlyTextual2);

        final int[] resultTableForTextualOnlyAmongClusters2 = createResultTableForStringAmongClusters(onlyTextualAmongClusters2);
        LOGGER.info(GSON.toJson(resultTableForTextualOnlyAmongClusters2) + "Textual (No Only Numbers)");
    }

    private static <T> int[] createResultTableForStringAmongClusters(final Map<T, AtomicInteger> stringAmongClusterCountByType) {
        int[] resultTable = new int[6]; // represent ranges for table in presentation
        for(AtomicInteger stringCount : stringAmongClusterCountByType.values()) {
            Experiment.countTableColumnValues(resultTable, stringCount.get());
        }

        return resultTable;
    }

    private static List<Map<Integer, CorefResultSet>> countClustersOnlyTextual(
            List<Map<Integer, CorefResultSet>> lowerStringByTypeMap, boolean noNumAllowed) {
        List<Map<Integer, CorefResultSet>> countPerType = new ArrayList<>();
        for(Map<Integer, CorefResultSet> map : lowerStringByTypeMap) {
            Map<Integer, CorefResultSet> newMap = new HashMap<>();
            for(int corefId : map.keySet()) {
                CorefResultSet corefResultSet = map.get(corefId);
                if(noNumAllowed) {
                    CorefResultSet clusterMentions = corefResultSet.getMentionsOnlyTextual();
                    newMap.put(corefId, clusterMentions);
                } else {
                    CorefResultSet clusterMentions = corefResultSet.getMentionsNoOnlyNumbers();
                    newMap.put(corefId, clusterMentions);
                }
            }

            countPerType.add(newMap);
        }

        return countPerType;
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
}
