package wec.extractors;

import data.CorefSubType;
import data.CorefType;
import wec.AInfoboxExtractor;

public class CompanyInfoboxExtractor extends AInfoboxExtractor {

    private static final int MAX_EMPLOYEES = 1000;

    private static CorefSubType[] subTypes = {CorefSubType.COMPANY};

    public CompanyInfoboxExtractor() {
        super(subTypes, CorefType.COMPANY);
    }

    @Override
    public CorefSubType extract(String infobox, String title) {
        infobox = infobox.toLowerCase().replaceAll(" ", "");
        if (infobox.contains("{{infoboxcompany")) {
            for (String line : infobox.split("\n")) {
                if (line.startsWith("|num_employees=")) {
                    final String[] numEmpSplit = line.split("=");
                    if (numEmpSplit.length == 2) {
                        try {
                            int empAmount = Integer.parseInt(numEmpSplit[1]);
                            if (empAmount <= MAX_EMPLOYEES) {
                                return CorefSubType.COMPANY;
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        }

        return CorefSubType.NA;
    }
}
