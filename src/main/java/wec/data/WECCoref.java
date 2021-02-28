package wec.data;

import wec.validators.DefaultInfoboxValidator;

import javax.persistence.*;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Table(name = "COREFS")
public class WECCoref {
    @Transient
    private static final ConcurrentHashMap<String, WECCoref> globalCorefIds = new ConcurrentHashMap<>();

    @Id @GeneratedValue(strategy= GenerationType.IDENTITY)
    private int corefId;
    private String corefValue;
    @Transient
    private AtomicInteger mentionsCount = new AtomicInteger(0);
    private String corefType = DefaultInfoboxValidator.NA;
    private String corefSubType = DefaultInfoboxValidator.NA;
    @Transient
    private boolean markedForRemoval = false;
    @Transient
    private boolean wasRetrieved = false;

    protected WECCoref() {
    }

    public WECCoref(String corefValue) {
        this.corefValue = corefValue;
    }

    public static synchronized WECCoref getAndSetIfNotExist(String corefValue) {
        if(!globalCorefIds.containsKey(corefValue)) {
            globalCorefIds.put(corefValue, new WECCoref(corefValue));
        }

        return globalCorefIds.get(corefValue);
    }

    public static void removeKey(String keyToRemove) {
        globalCorefIds.remove(keyToRemove);
    }

    public static Map<String, WECCoref> getGlobalCorefMap() {
        return globalCorefIds;
    }

    public int getCorefId() {
        return corefId;
    }

    public void setCorefId(int corefId) {
        this.corefId = corefId;
    }

    public String getCorefValue() {
        return corefValue;
    }

    public void setCorefValue(String corefValue) {
        this.corefValue = corefValue;
    }

    public void incMentionsCount() {
        this.mentionsCount.incrementAndGet();
    }

    public int addAndGetMentionCount(int delta) {
        return this.mentionsCount.addAndGet(delta);
    }

    public int getMentionsCount() {
        return this.mentionsCount.get();
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
        return corefId == wecCoref.corefId &&
                markedForRemoval == wecCoref.markedForRemoval &&
                wasRetrieved == wecCoref.wasRetrieved &&
                Objects.equals(corefValue, wecCoref.corefValue) &&
                Objects.equals(mentionsCount, wecCoref.mentionsCount) &&
                Objects.equals(corefType, wecCoref.corefType) &&
                Objects.equals(corefSubType, wecCoref.corefSubType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corefId, corefValue, mentionsCount, corefType, corefSubType, markedForRemoval, wasRetrieved);
    }
}
