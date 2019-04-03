package wikilinks;

public interface ICorefFilter<T> {
    boolean isConditionMet(T input);
}
