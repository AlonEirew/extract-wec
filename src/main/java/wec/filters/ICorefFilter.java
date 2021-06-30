package wec.filters;

import wec.data.RawElasticResult;

public interface ICorefFilter<T> {
    boolean isConditionMet(T input);
}
