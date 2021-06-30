package wec.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wec.data.WECContext;
import wec.data.WECMention;

import java.util.Set;

public class ByCorefFilter implements ICorefFilter<WECContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByCorefFilter.class);

    private static Set<String> corefToFilter;

    public ByCorefFilter() { }

    @Override
    public boolean isConditionMet(WECContext input) {
        if (corefToFilter == null || corefToFilter.isEmpty()) {
            throw new IllegalStateException("corefToFilter not initialized!");
        }

        for(WECMention mention : input.getMentionList()) {
            if (corefToFilter.contains(mention.getCorefChain().getCorefValue())) {
                LOGGER.info("found context with mention coref-" + mention.getCorefChain().getCorefValue() + " will be removed");
                return false;
            }
        }

        return true;
    }

    public static void setCorefToFilter(Set<String> toFilter) {
        corefToFilter = toFilter;
    }
}
