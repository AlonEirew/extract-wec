package experimentscripts;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.*;

public class CorefResultSet {
    private int corefId;
    private int corefType;
    private String corefValue;
    private List<MentionResultSet> mentions = new ArrayList<>();

    public CorefResultSet(int corefId) {
        this.corefId = corefId;
    }

    public CorefResultSet(int corefId, int corefType, String corefValue) {
        this.corefId = corefId;
        this.corefType = corefType;
        this.corefValue = corefValue;
    }

    public void addMention(MentionResultSet mention) {
        this.mentions.add(mention);
    }

    public void addMentionCopy(MentionResultSet mention) {
        MentionResultSet newMention = new MentionResultSet(mention);
        this.mentions.add(newMention);
    }

    public int getMentionsSize() {
        return this.mentions.size();
    }

    public void addMentionsCollection(Collection<MentionResultSet> mentColl) {
        this.mentions.addAll(mentColl);
    }

    public void addMentionsCopyCollection(Collection<MentionResultSet> mentColl) {
        for(MentionResultSet mention : mentColl) {
            addMentionCopy(mention);
        }
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

    public float getAverageMentionsSpan() {
        float sum = 0;
        if(this.mentions.size() > 0) {
            for (MentionResultSet mention : this.mentions) {
                sum += mention.getMentionString().split(" ").length;
            }

            return sum / mentions.size();
        }

        return 0;
    }

    public int getCorefId() {
        return corefId;
    }

    public CorefResultSet getMentionsOnlyUniques() {
        CorefResultSet retResultSet = new CorefResultSet(this.corefId, this.corefType, this.corefValue);
        Set<String> clusterMentionsAsString = new HashSet<>();
        for(MentionResultSet mentionResultSet : this.mentions) {
            if(clusterMentionsAsString.add(mentionResultSet.getMentionString())) {
                retResultSet.addMentionCopy(mentionResultSet);
            }
        }

        return retResultSet;
    }

    public CorefResultSet getMentionsOnlyTextual() {
        CorefResultSet retResultSet = new CorefResultSet(this.corefId, this.corefType, this.corefValue);
        for(MentionResultSet mentionResultSet : this.mentions) {
            if(!mentionResultSet.getMentionString().matches(".*\\d+.*")) {
                retResultSet.addMentionCopy(mentionResultSet);
            }
        }

        return retResultSet;
    }

    public CorefResultSet getMentionsNoOnlyNumbers() {
        CorefResultSet retResultSet = new CorefResultSet(this.corefId, this.corefType, this.corefValue);
        for(MentionResultSet mentionResultSet : this.mentions) {
            if(!mentionResultSet.getMentionString().matches("\\d+")) {
                retResultSet.addMentionCopy(mentionResultSet);
            }
        }

        return retResultSet;
    }

    public CorefResultSet getLevenshteinDistanceMentions() {
        CorefResultSet retResultSet = new CorefResultSet(this.corefId, this.corefType, this.corefValue);
        if(this.mentions.size() == 1) {
            retResultSet.addMentionCopy(this.mentions.get(0));
        } else {
            retResultSet.addMentionsCopyCollection(this.mentions);
            for (int x = 0; x < retResultSet.mentions.size(); x++) {
                for (int y = x + 1; y < retResultSet.mentions.size(); y++) {
                    if(!retResultSet.mentions.get(y).isMarkedForDelete()) {
                        final Integer apply = LevenshteinDistance.getDefaultInstance()
                                .apply(retResultSet.mentions.get(x).getMentionString(),
                                        retResultSet.mentions.get(y).getMentionString());

                        if (apply <= 2) {
                            retResultSet.mentions.get(y).setMarkedForDelete(true);
                        }
                    }
                }
            }
        }

        retResultSet.cleanMentionsMarkedForDeletion();
        return retResultSet;
    }

    public CorefResultSet getNonIntersectingMentions() {
        CorefResultSet retResultSet = new CorefResultSet(this.corefId, this.corefType, this.corefValue);
        if(this.mentions.size() == 1) {
            retResultSet.addMentionCopy(this.mentions.get(0));
        } else {
            retResultSet.addMentionsCopyCollection(this.mentions);
            for (int x = 0; x < retResultSet.mentions.size(); x++) {
                for (int y = x + 1; y < retResultSet.mentions.size(); y++) {
                    if(!retResultSet.mentions.get(y).isMarkedForDelete()) {
                        List<String> m1 = new ArrayList<>(Arrays.asList(retResultSet.mentions.get(x)
                                .getMentionString().split("\\s")));
                        List<String> m2 = new ArrayList<>(Arrays.asList(retResultSet.mentions.get(y)
                                .getMentionString().split("\\s")));

                        m1.retainAll(m2);
                        if (m1.size() >= 1) {
                            retResultSet.mentions.get(y).setMarkedForDelete(true);
                        }
                    }
                }
            }
        }

        retResultSet.cleanMentionsMarkedForDeletion();
        return retResultSet;
    }

    public void cleanMentionsMarkedForDeletion() {
        final Iterator<MentionResultSet> iterator = this.mentions.iterator();
        while(iterator.hasNext()) {
            final MentionResultSet next = iterator.next();
            if(next.isMarkedForDelete()) {
                iterator.remove();
            }
        }
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

    @Override
    public String toString() {
        return "corefId=" + corefId +", corefValue=" + corefValue +", mentions=" + mentions.size() +
                ", corefType=" + corefType;
    }
}
