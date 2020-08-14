package wec.extractors;

import data.RawElasticResult;

public interface IExtractor<Result> {
    Result extract(RawElasticResult elasticResult);
}
