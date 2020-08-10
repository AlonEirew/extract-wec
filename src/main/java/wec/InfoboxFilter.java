package wec;

import data.InfoboxConfiguration;
import data.RawElasticResult;
import data.WECCoref;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InfoboxFilter implements ICorefFilter {

    private final static Logger LOGGER = LogManager.getLogger(InfoboxFilter.class);

    private final InfoboxConfiguration infoboxConfiguration;

    public InfoboxFilter(InfoboxConfiguration infoboxConfiguration) {
        this.infoboxConfiguration = infoboxConfiguration;
    }

    @Override
    public boolean isConditionMet(RawElasticResult result) {
        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
            if (result.getInfobox() != null && !result.getInfobox().isEmpty()) {
                for (DefaultInfoboxExtractor extractor : this.infoboxConfiguration.getAllIncludedExtractor()) {
                    final String extractMatchedInfobox = extractor.extractMatchedInfobox(result.getInfobox(), result.getTitle());
                    final String corefType = extractor.getCorefType();

                    if (!extractMatchedInfobox.equals(DefaultInfoboxExtractor.NA)) {
                        LOGGER.info(result.getTitle() + " passed as " + extractMatchedInfobox + " infobox");
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
