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
            final String infoBox = this.extractPageInfoBox(result.getText());
            if (infoBox != null && !infoBox.isEmpty()) {
                for (DefaultInfoboxExtractor extractor : this.infoboxConfiguration.getAllIncludedExtractor()) {
                    final String extractMatchedInfobox = extractor.extractMatchedInfobox(result.getText(), result.getTitle());
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

    public String extractPageInfoBox(String pageText) {
        return extractPageInfoBox(pageText, false);
    }

    public String extractPageInfoBox(String pageText, boolean toLowerForm) {
        StringBuilder infoBoxFinal = new StringBuilder();

        final int beginIndex = pageText.indexOf("{{" + infoboxConfiguration.getInfoboxLangText());
        if (beginIndex != -1) {
            final String infoboxSubstring = pageText.substring(beginIndex);
            int infoBarCount = 0;
            for (int i = 0; i < infoboxSubstring.length(); i++) {
                final char c = infoboxSubstring.charAt(i);
                if (c == '}') {
                    infoBarCount--;
                    if (infoBarCount == 0) {
                        infoBoxFinal.append(c);
                        break;
                    }
                } else if (c == '{') {
                    infoBarCount++;
                }

                infoBoxFinal.append(c);
            }
        }

        if(toLowerForm) {
            return infoBoxFinal.toString().toLowerCase().replaceAll(" ", "");
        }

        return infoBoxFinal.toString();
    }
}
