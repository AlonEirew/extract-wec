package wec;

import data.CorefSubType;
import data.CorefType;
import data.RawElasticResult;
import data.WECCoref;

import java.util.List;

public class PersonOrEventFilter implements ICorefFilter {
    private List<AInfoboxExtractor> extractors;


    public PersonOrEventFilter(List<AInfoboxExtractor> extractors) {
        this.extractors = extractors;
    }

    @Override
    public boolean isConditionMet(RawElasticResult result) {
        boolean retCond = false;

        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            for (AInfoboxExtractor extractor : this.extractors) {
                final CorefSubType corefSubType = extractor.extract(result.getText(), result.getTitle());
                final CorefType corefType = extractor.extractTypeFromSubType(corefSubType);

                if (corefSubType != CorefSubType.NA && corefType != CorefType.NA) {
                    WECCoref wecCoref = WECCoref.getAndSetIfNotExist(result.getTitle());
                    wecCoref.setCorefType(corefType);
                    wecCoref.setCorefSubType(corefSubType);
                    retCond = true;
                }
            }
        }

        return !retCond;
    }
}
