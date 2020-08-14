package wec.validators;

import wec.DefaultInfoboxValidator;

import java.util.regex.Pattern;

public class CompanyInfoboxValidator extends DefaultInfoboxValidator {

    private static final int MAX_EMPLOYEES = 1700;

    public CompanyInfoboxValidator(String corefType, Pattern pattern) {
        super(corefType, pattern);
    }

    @Override
    public String extractMatchedInfobox(String infobox, String title) {
        String extract = super.extractMatchedInfobox(infobox, title);
        if(!extract.equals(DefaultInfoboxValidator.NA)) {
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

        return DefaultInfoboxValidator.NA;
    }
}
