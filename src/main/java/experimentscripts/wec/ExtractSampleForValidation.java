package experimentscripts.wec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ISQLObject;
import persistence.SQLQueryApi;
import persistence.SQLiteConnections;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtractSampleForValidation {
    private final static Logger LOGGER = LogManager.getLogger(ExtractSampleForValidation.class);

    private static final int MAX_NONE_UNIQE_BEGIN = 3;
    private static final int MAX_NONE_UNIQE_END = 3;
    private static final String VALIDATION_TABLE = "Validation";

    public static void main(String[] args) throws SQLException {
        String connectionUrl = "jdbc:sqlite:/Users/aeirew/workspace/DataBase/EnWikiLinks_v9.db";
        SQLiteConnections sqLiteConnections = new SQLiteConnections(connectionUrl);
        SQLQueryApi sqlApi = new SQLQueryApi(sqLiteConnections);

        runCreateValidation(sqlApi, sqLiteConnections, 1500);
    }

    private static void runCreateVerbMentions(SQLQueryApi sqlApi, SQLiteConnections sqLiteConnections) throws SQLException {
        String query = "SELECT * from Mentions INNER JOIN CorefChains ON " +
                "Mentions.coreChainId=CorefChains.corefId where corefType in (2,3,4,8,9,10) " +
                "and PartOfSpeech like '%VB%';";

        List<VerbMention> allVerbMentions = getAllVerbMentions(sqLiteConnections, query);
        sqlApi.createTable(new VerbMention());
        commitCurrent(allVerbMentions, sqlApi);
    }

    private static void runCreateValidation(SQLQueryApi sqlApi, SQLiteConnections sqLiteConnections, int limitClusters) throws SQLException {
        String query = "SELECT mentionId, coreChainId, mentionText, extractedFromPage, tokenStart, " +
                "tokenEnd, corefValue, corefType, PartOfSpeech, context " +
                "from Mentions INNER JOIN CorefChains ON Mentions.coreChainId=CorefChains.corefId " +
                "where corefType in (2,3,4,6,7,8)";

        for(int i = MAX_NONE_UNIQE_BEGIN; i <= MAX_NONE_UNIQE_END; i++) {
            final Map<Integer, CorefResultSet> countPerType = getAllCorefs(sqLiteConnections, query, i);
            sqlApi.createTable(new ValidationMention(VALIDATION_TABLE + i));
            countPerType.entrySet().removeIf(entry -> entry.getValue().getMentions().isEmpty());
            List<ValidationMention> toCommit = fromMapToList(countPerType, String.valueOf(i), limitClusters);

            commitCurrent(toCommit, sqlApi);
        }

        LOGGER.info("Done!");
    }

    private static <T extends ISQLObject> void commitCurrent(List<T> localNewList, SQLQueryApi sqlApi) {
        LOGGER.info("Prepare to inset-" + localNewList.size() + " mentions to SQL");
        try {
            if (!sqlApi.insertRowsToTable(localNewList)) {
                LOGGER.error("Failed to insert mentions Batch!!!!");
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
    }

    private static List<ValidationMention> fromMapToList(Map<Integer, CorefResultSet> countPerType, String validationTableSuffix, int limitClusters) {
        List<ValidationMention> validationMentions = new ArrayList<>();
        int marker = 0;
        ValidationMention.SPLIT split;
        Map<Integer, AtomicInteger> corefTypesCounter = new HashMap<>();
        List<CorefResultSet> corefResultSetList = new ArrayList<>(countPerType.values());
        for(CorefResultSet corefResultSet : corefResultSetList) {
            if (corefTypesCounter.containsKey(corefResultSet.getCorefType())) {
                if(corefTypesCounter.get(corefResultSet.getCorefType()).getAndIncrement() > limitClusters) {
                    continue;
                }
            } else {
                corefTypesCounter.put(corefResultSet.getCorefType(), new AtomicInteger(1));
            }

            if(marker < 3000) {
                split = ValidationMention.SPLIT.VALIDATION;
            } else if(marker >= 3000 && marker < 6000) {
                split = ValidationMention.SPLIT.TEST;
            } else {
                split = ValidationMention.SPLIT.TRAIN;
            }

            for(MentionResultSet mentionResultSet : corefResultSet.getMentions()) {
                validationMentions.add(new ValidationMention(mentionResultSet, split, VALIDATION_TABLE + validationTableSuffix));
            }
            marker ++;
        }

        return validationMentions;
    }

    private static Map<Integer, CorefResultSet> getAllCorefs(SQLiteConnections sqlConnection, String query, int maxNoneUnique) throws SQLException {
        LOGGER.info("Preparing to select all coref mentions by types");
        Map<Integer, CorefResultSet> corefMap = new HashMap<>();
        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.info("Preparing to extract");

            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                MentionResultSet mention = fromResultSetToMention(rs);
                final int corefType = rs.getInt("corefType");
                final String corefValue = rs.getString("corefValue");

                if (mention != null) {
                    if (!corefMap.containsKey(mention.getCorefId())) {
                        final CorefResultSet corefResultSet = new CorefResultSet(mention.getCorefId(), corefType, corefValue);
                        corefResultSet.addMention(mention);
                        corefMap.put(mention.getCorefId(), corefResultSet);
                    } else {
                        final CorefResultSet corefResultSet = corefMap.get(mention.getCorefId());
                        if(maxNoneUnique > 0) {
                            corefResultSet.addNoneIntersectionUniqueMention(mention, maxNoneUnique);
                        }
                    }
                }
            }
        }

        return corefMap;
    }

    private static List<VerbMention> getAllVerbMentions(SQLiteConnections sqlConnection, String query) throws SQLException {
        LOGGER.info("Preparing to select all verb mentions by types");
        List<VerbMention> mentions = new ArrayList<>();

        try (Connection conn = sqlConnection.getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.info("Preparing to extract");

            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                MentionResultSet mention = fromResultSetToMention(rs);
                if(mention != null) {
                    mentions.add(new VerbMention(mention));
                }
            }
        }

        return mentions;
    }

    private static MentionResultSet fromResultSetToMention(ResultSet rs) throws SQLException {
        final int mentionId = rs.getInt("mentionId");
        final int corefId = rs.getInt("coreChainId");
        final String mentionText = rs.getString("mentionText");
        final String extractedFromPage = rs.getString("extractedFromPage");
        final int tokenStart = rs.getInt("tokenStart");
        final int tokenEnd = rs.getInt("tokenEnd");
        final String partOfSpeech = rs.getString("PartOfSpeech");
        final String context = rs.getString("context");

        if(mentionText.trim().isEmpty() || mentionText.matches("\\d+") || !mentionText.matches("[A-Za-z0-9\\s]+")) {
            return null;
        }

        return new MentionResultSet(corefId, mentionId, mentionText, extractedFromPage,
                tokenStart, tokenEnd, context, partOfSpeech);
    }
}
