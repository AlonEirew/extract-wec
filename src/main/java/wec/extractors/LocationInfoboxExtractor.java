package wec.extractors;

import wec.DefaultInfoboxExtractor;

import java.util.regex.Pattern;

public class LocationInfoboxExtractor extends DefaultInfoboxExtractor {

    public LocationInfoboxExtractor(String corefType, Pattern pattern) {
        super(corefType, pattern);
    }

    @Override
    public String extractMatchedInfobox(String infobox, String title) {
        String extract = super.extractMatchedInfobox(infobox, title);

        if(!extract.equals(DefaultInfoboxExtractor.NA)) {
            String location = this.extractLocationLine(infobox);
            if(location != null && !location.isEmpty()) {
                return extract;
            }
        }

        return DefaultInfoboxExtractor.NA;
    }

    public String extractLocationLine(String infoBox) {
        String location = "";
        if(infoBox != null && infoBox.contains("location")) {
            String locationSubStr = infoBox.substring(infoBox.indexOf("location"));
            if(locationSubStr.contains("<br>")) {
                location = locationSubStr.substring(0, locationSubStr.indexOf("<br>"));
            } else if(locationSubStr.contains("\n")) {
                location = locationSubStr.substring(0, locationSubStr.indexOf("\n"));
            } else {
                location = locationSubStr;
            }
        }

        return location.replaceAll("\\s+", " ");
    }
}
