package data;

import com.google.gson.JsonArray;

import java.util.Objects;

public class EventSubEventPair {
    private final BaseMention event;
    private final BaseMention subEvent;
    private final JsonArray context;

    public EventSubEventPair(BaseMention event, BaseMention subEvent, JsonArray context) {
        this.event = event;
        this.subEvent = subEvent;
        this.context = context;
    }

    public BaseMention getEvent() {
        return event;
    }

    public BaseMention getSubEvent() {
        return subEvent;
    }

    public JsonArray getContext() {
        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventSubEventPair that = (EventSubEventPair) o;
        return Objects.equals(event, that.event) &&
                Objects.equals(subEvent, that.subEvent) &&
                Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, subEvent, context);
    }
}
