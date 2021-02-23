package data;

import java.util.Set;

public class WECMentionSubEvent extends WECMention {
    private final Set<Integer> subEventOf;

    public WECMentionSubEvent(WECMention mention, Set<Integer> subEventOf) {
        super(mention);
        this.subEventOf = subEventOf;
    }

    public Set<Integer> getSubEventOf() {
        return subEventOf;
    }
}
