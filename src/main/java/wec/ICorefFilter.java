package wec;

import data.RawElasticResult;

public interface ICorefFilter {
    boolean isConditionMet(RawElasticResult input);
}
