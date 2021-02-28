package wec.filters;

import wec.data.RawElasticResult;

public interface ICorefFilter {
    boolean isConditionMet(RawElasticResult input);
}
