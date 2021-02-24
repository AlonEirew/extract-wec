package experimentscripts.wec;

import experimentscripts.wec.resultsets.CorefResultSet;
import experimentscripts.wec.resultsets.MentionResultSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SQLiteConnections;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckNominalization {
    private final static Logger LOGGER = LogManager.getLogger(CheckNominalization.class);

    public static void main(String[] args) throws SQLException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/WikiLinksPersonEventFull_v9.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);

        final Map<Integer, CorefResultSet> mentionResultSets = countVerbs(sqLiteConnections);

        List<Map<Integer, CorefResultSet>> corefsMapList = new ArrayList<>();
        corefsMapList.add(mentionResultSets);

        Experiment.printResults(corefsMapList, "AS IS");
        final List<Map<Integer, CorefResultSet>> uniqueCorefMap = Experiment.countClustersUniqueString(corefsMapList);
        Experiment.printResults(uniqueCorefMap, "UNIQUE");

        Experiment.createLevinshteinDistanceReport(uniqueCorefMap);
        Experiment.createMentionsIntersectionReport(uniqueCorefMap);
        Experiment.createMentionsLevinshteinAndIntersectionReport(uniqueCorefMap);

        LOGGER.info("Average Mentions span=" + averageMentionSpanSize(uniqueCorefMap.get(0)));
    }

    private static float averageMentionSpanSize(Map<Integer, CorefResultSet> corefsMap) {
        float sumAvgs = 0;
        float clusterCount = 0;
        for(CorefResultSet corefResultSet : corefsMap.values()) {
            if(corefResultSet.getMentions().size() > 0) {
                sumAvgs += corefResultSet.getAverageMentionsSpan();
                clusterCount++;
            }
        }

        return sumAvgs/clusterCount;
    }

    private static Map<Integer, CorefResultSet> countVerbs(SQLiteConnections sqlConnection) throws SQLException {
        LOGGER.info("Preparing to select all coref mentions by types");
        Map<Integer, CorefResultSet> corefMap = new HashMap<>();
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.info("Preparing to extract");

            String query = "SELECT coreChainId, mentionText, extractedFromPage, tokenStart, tokenEnd, PartOfSpeech " +
                    "from Validation INNER JOIN CorefChains ON Validation.coreChainId=CorefChains.corefId " +
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

                if(!corefMap.containsKey(corefId)) {
                    corefMap.put(corefId, new CorefResultSet(corefId));
                }

                if (mentionTokens.length == 1) {
                    if (posList[0].matches("VB[DGNPZ]?")) {
                        MentionResultSet ment = new MentionResultSet(corefId, mentionText, extractedFromPage,
                                tokenStart, tokenEnd, null, partOfSpeech);
                        corefMap.get(corefId).addMention(ment);
                    }
                    continue;
                }

                for (int i = 0, j = 0; i < mentionTokens.length; i++, j++) {
                    try {
                        if (mentionTokens[i].isEmpty()) {
                            i++;
                        }

                        if (posList[j].matches("VB[DGNPZ]?") && !Character.isUpperCase(mentionTokens[i].charAt(0))) {
                            MentionResultSet ment = new MentionResultSet(corefId, mentionText, extractedFromPage,
                                    tokenStart, tokenEnd, null, partOfSpeech);
                            corefMap.get(corefId).addMention(ment);
                        }
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        break;
                    } catch (StringIndexOutOfBoundsException ex) {
                        LOGGER.error(ex);
                    }
                }

            }
        }

        return corefMap;
    }
}
