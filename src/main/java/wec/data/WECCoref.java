package wec.data;

import wec.validators.DefaultInfoboxValidator;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Entity
@Table(name = "COREFS")
public class WECCoref {
    private static final ConcurrentHashMap<String, WECCoref> globalCorefIds = new ConcurrentHashMap<>();

    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long corefId;
    private String corefValue;
    private int mentionsCount;
    private String corefType = DefaultInfoboxValidator.NA;
    private String corefSubType = DefaultInfoboxValidator.NA;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "corefChain")
    private final List<WECMention> mentionList = new ArrayList<>();

    @Transient
    private boolean markedForRemoval = false;
    @Transient
    private boolean wasRetrieved = false;

    protected WECCoref() {
    }

    private WECCoref(String corefValue) {
        this.corefValue = corefValue;
    }

    public static synchronized WECCoref getAndSetIfNotExist(String corefValue) {
        if(!globalCorefIds.containsKey(corefValue)) {
            globalCorefIds.put(corefValue, new WECCoref(corefValue));
        }

        return globalCorefIds.get(corefValue);
    }

    public List<WECMention> getMentionList() {
        return mentionList;
    }

    public void addMention(WECMention mention) {
        this.mentionList.add(mention);
    }

    public static void removeKey(String keyToRemove) {
        globalCorefIds.remove(keyToRemove);
    }

    public static Map<String, WECCoref> getGlobalCorefMap() {
        return globalCorefIds;
    }

    public long getCorefId() {
        return corefId;
    }

    public void setCorefId(long corefId) {
        this.corefId = corefId;
    }

    public String getCorefValue() {
        return corefValue;
    }

    public void setCorefValue(String corefValue) {
        this.corefValue = corefValue;
    }

    public synchronized void incMentionsCount() {
        this.mentionsCount++;
    }

    public int getMentionsCount() {
        return this.mentionsCount;
    }

    public String getCorefType() {
        return corefType;
    }

    public void setCorefType(String corefType) {
        this.corefType = corefType;
    }

    public void setCorefSubType(String corefSubType) {
        this.corefSubType = corefSubType;
    }

    public String getCorefSubType() {
        return corefSubType;
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    public void setMarkedForRemoval(boolean markedForRemoval) {
        this.markedForRemoval = markedForRemoval;
    }

    public boolean wasAlreadyRetrived() {
        return wasRetrieved;
    }

    public void setWasAlreadyRetrived(boolean wasRetrived) {
        this.wasRetrieved = wasRetrived;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WECCoref wecCoref = (WECCoref) o;
        return corefId == wecCoref.corefId && mentionsCount == wecCoref.mentionsCount && markedForRemoval == wecCoref.markedForRemoval && wasRetrieved == wecCoref.wasRetrieved && corefValue.equals(wecCoref.corefValue) && corefType.equals(wecCoref.corefType) && corefSubType.equals(wecCoref.corefSubType) && mentionList.equals(wecCoref.mentionList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corefId, corefValue, mentionsCount, corefType, corefSubType, mentionList, markedForRemoval, wasRetrieved);
    }
}
