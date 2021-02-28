package wec.data;

import java.util.Objects;

public class EventSubEventPair {
    private final BaseMention event;
    private final BaseMention subEvent;
    private final int contextId;

    public EventSubEventPair(BaseMention event, BaseMention subEvent, int contextId) {
        this.event = event;
        this.subEvent = subEvent;
        this.contextId = contextId;
    }

    public BaseMention getEvent() {
        return event;
    }

    public BaseMention getSubEvent() {
        return subEvent;
    }

    public int getContextId() {
        return contextId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventSubEventPair that = (EventSubEventPair) o;
        return contextId == that.contextId && Objects.equals(event, that.event) && Objects.equals(subEvent, that.subEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, subEvent, contextId);
    }
}
