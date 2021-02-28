package wec.extractors;

import wec.data.RawElasticResult;

public interface IExtractor<Result> {
    Result extract(RawElasticResult elasticResult);
}
