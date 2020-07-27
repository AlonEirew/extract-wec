package wec;

import data.CorefSubType;
import data.CorefType;
import data.RawElasticResult;
import data.WECCoref;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class PersonOrEventFilter implements ICorefFilter {

    private final static Logger LOGGER = LogManager.getLogger(PersonOrEventFilter.class);

    private final List<AInfoboxExtractor> extractors;

    public PersonOrEventFilter(List<AInfoboxExtractor> extractors) {
        this.extractors = extractors;
    }

    @Override
    public boolean isConditionMet(RawElasticResult result) {
        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            for (AInfoboxExtractor extractor : this.extractors) {
                final CorefSubType corefSubType = extractor.extract(result.getText(), result.getTitle());
                final CorefType corefType = extractor.extractTypeFromSubType(corefSubType);

                if (corefSubType != CorefSubType.NA && corefType != CorefType.NA) {
                    LOGGER.info(result.getTitle() + " passed " + extractor.getClass().getSimpleName() + " extractor");
                    WECCoref wecCoref = WECCoref.getAndSetIfNotExist(result.getTitle());
                    wecCoref.setCorefType(corefType);
                    wecCoref.setCorefSubType(corefSubType);
                    return true;
                }
            }
        }

        return false;
    }
}
