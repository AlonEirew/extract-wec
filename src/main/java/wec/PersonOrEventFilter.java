package wec;

import data.InfoboxConfiguration;
import data.RawElasticResult;
import data.WECCoref;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PersonOrEventFilter implements ICorefFilter {

    private final static Logger LOGGER = LogManager.getLogger(PersonOrEventFilter.class);

    private final InfoboxConfiguration infoboxConfiguration;

    public PersonOrEventFilter(InfoboxConfiguration infoboxConfiguration) {
        this.infoboxConfiguration = infoboxConfiguration;
    }

    @Override
    public boolean isConditionMet(RawElasticResult result) {
        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            for (InfoboxConfiguration.InfoboxConfig config : this.infoboxConfiguration.getInfoboxConfigs()) {
                DefaultInfoboxExtractor extractor = config.getExtractor();
                if(config.isInclude() && extractor != null) {
                    final String extractMatchedInfobox = extractor.extractMatchedInfobox(result.getText(), result.getTitle());
                    final String corefType = extractor.getCorefType();

                    if (!extractMatchedInfobox.equals(DefaultInfoboxExtractor.NA)) {
                        LOGGER.info(result.getTitle() + " passed " + extractor.getClass().getSimpleName() + " extractor");
                        WECCoref wecCoref = WECCoref.getAndSetIfNotExist(result.getTitle());
                        wecCoref.setCorefType(corefType);
                        wecCoref.setCorefSubType(extractMatchedInfobox);
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
