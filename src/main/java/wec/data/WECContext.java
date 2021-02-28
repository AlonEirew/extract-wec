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
public class WECContext {

    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    private long contextId;
    @Column(columnDefinition="TEXT")
    private String context;

    protected WECContext() {
    }

    public WECContext(JsonArray context) {
        this.context = getContextAsJsonString(context);
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

    public String getContextAsJsonString(JsonArray contextAsArray) {
        return Configuration.GSON.toJson(contextAsArray);
    }

    public String getContextAsString() {
        JsonArray jsonElements = Configuration.GSON.fromJson(this.context, JsonArray.class);
        List<String> contextList = new ArrayList<>();
        for(JsonElement tok : jsonElements) {
            for (Map.Entry<String, JsonElement> entry : tok.getAsJsonObject().entrySet()) {
                contextList.add(entry.getKey());
            }
        }

        return String.join(" ", contextList);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WECContext that = (WECContext) o;
        return contextId == that.contextId && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId, context);
    }
}
