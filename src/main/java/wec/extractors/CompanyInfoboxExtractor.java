package wec.extractors;

import wec.DefaultInfoboxExtractor;

import java.util.List;
import java.util.regex.Pattern;

public class CompanyInfoboxExtractor extends DefaultInfoboxExtractor {

    private static final int MAX_EMPLOYEES = 1000;

    public CompanyInfoboxExtractor(String corefType, List<String> infoboxs, Pattern pattern) {
        super(corefType, infoboxs, pattern);
    }

    @Override
    public String extractMatchedInfobox(String infobox, String title) {
        String extract = super.extractMatchedInfobox(infobox, title);
        if(!extract.equals(DefaultInfoboxExtractor.NA)) {
            String infoboxLow = infobox.toLowerCase().replaceAll(" ", "");
            for (String line : infoboxLow.split("\n")) {
                if (line.startsWith("|num_employees=")) {
                    final String[] numEmpSplit = line.split("=");
                    if (numEmpSplit.length == 2) {
                        try {
                            int empAmount = Integer.parseInt(numEmpSplit[1]);
                            if (empAmount <= MAX_EMPLOYEES) {
                                return extract;
                            }
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
            }
        }

        return DefaultInfoboxExtractor.NA;
    }
}
