package extract;

import java.util.*;

public class CorefResultSet {
    private int corefId;
    private List<MentionResultSet> mentions = new ArrayList<>();

    public CorefResultSet(int corefId) {
        this.corefId = corefId;
    }

    public void addMention(MentionResultSet mention) {
        this.mentions.add(mention);
    }

    public List<MentionResultSet> getMentions() {
        return mentions;
    }

    public Map<String, List<MentionResultSet>> getWithinDocCoref() {
        Map<String, List<MentionResultSet>> wdCoref = new HashMap<>();
        for(MentionResultSet mention : this.mentions) {
            final String extractedFromPage = mention.getExtractedFromPage();
            if(!wdCoref.containsKey(extractedFromPage)) {
                wdCoref.put(extractedFromPage, new ArrayList<>());
            }

            wdCoref.get(extractedFromPage).add(mention);
        }

        return wdCoref;
    }

    public int getCorefId() {
        return corefId;
    }

    public List<String> getMentionsAsStringList() {
        List<String> clusterMentionsAsString = new ArrayList<>();
        for(MentionResultSet mentionResultSet : this.mentions) {
            clusterMentionsAsString.add(mentionResultSet.getMentionString());
        }

        return clusterMentionsAsString;
    }

    public Set<String> getMentionsAsSet() {
        Set<String> clusterMentionsAsString = new HashSet<>();
        for(MentionResultSet mentionResultSet : this.mentions) {
            clusterMentionsAsString.add(mentionResultSet.getMentionString());
        }

        return clusterMentionsAsString;
    }

    public List<MentionResultSet> countDuplicates() {
        List<MentionResultSet> retList = new ArrayList<>();
        Set<MentionResultSet> dupSet = new HashSet<>();
        for (MentionResultSet mention : this.mentions) {
            if(!dupSet.add(mention)) {
                retList.add(mention);
            }
        }

        return retList;
    }
}
