package wec.filters;

import wec.config.InfoboxConfiguration;
import wec.data.RawElasticResult;
import wec.data.WECCoref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wec.validators.DefaultInfoboxValidator;

public class InfoboxFilter implements ICorefFilter<RawElasticResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoboxFilter.class);

    private final InfoboxConfiguration infoboxConfiguration;

    public InfoboxFilter(InfoboxConfiguration infoboxConfiguration) {
        this.infoboxConfiguration = infoboxConfiguration;
    }

    @Override
    public boolean isConditionMet(RawElasticResult result) {
        if(infoboxConfiguration == null) {
            throw new IllegalStateException("infoboxConfiguration not initialized!");
        }

        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            if (result.getInfobox() != null && !result.getInfobox().isEmpty()) {
                for (DefaultInfoboxValidator validator : this.infoboxConfiguration.getAllIncludedValidators()) {
                    final String extractMatchedInfobox = validator.validateMatchedInfobox(result.getInfobox(), result.getTitle());
                    final String corefType = validator.getCorefType();

                    if (!extractMatchedInfobox.equals(DefaultInfoboxValidator.NA)) {
                        LOGGER.debug(result.getTitle() + " passed as " + extractMatchedInfobox + " infobox");
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
