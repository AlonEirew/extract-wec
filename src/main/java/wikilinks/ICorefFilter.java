package wikilinks;

import data.RawElasticResult;

public interface ICorefFilter {
    boolean isConditionMet(RawElasticResult input);
}
