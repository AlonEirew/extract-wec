package wec.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import wec.config.Configuration;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "CONTEXTS")
public class WECContext {

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long contextId;
    @Lob
    private String context;
    @Transient
    private List<WECMention> mentionList;

    protected WECContext() {
    }

    public WECContext(List<WECMention> mentionList) {
        this.mentionList = mentionList;
    }

    public WECContext(JsonArray contextAsArray, List<WECMention> mentionList) {
        this.context = Configuration.GSON.toJson(contextAsArray);
        this.mentionList = mentionList;
    }

    public long getContextId() {
        return contextId;
    }

    public void setContextId(long contextId) {
        this.contextId = contextId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public List<String> getContextAsArray() {
        JsonArray jsonElements = Configuration.GSON.fromJson(this.context, JsonArray.class);
        List<String> contextList = new ArrayList<>();
        for(JsonElement tok : jsonElements) {
            for (Map.Entry<String, JsonElement> entry : tok.getAsJsonObject().entrySet()) {
                contextList.add(entry.getKey());
            }
        }

        return contextList;
    }

    public List<WECMention> getMentionList() {
        return mentionList;
    }

    public void setMentionList(List<WECMention> mentionList) {
        this.mentionList = mentionList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WECContext that = (WECContext) o;
        return contextId == that.contextId && context.equals(that.context) && Objects.equals(mentionList, that.mentionList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId, context, mentionList);
    }
}
