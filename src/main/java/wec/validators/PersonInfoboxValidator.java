package wec.validators;

import wec.DefaultInfoboxValidator;

import java.util.regex.Pattern;

public class PersonInfoboxValidator extends DefaultInfoboxValidator {

    public PersonInfoboxValidator(String corefType, Pattern pattern) {
        super(corefType, pattern);
    }

    @Override
    public String validateMatchedInfobox(String infobox, String title) {
        infobox = infobox.toLowerCase().replaceAll(" ", "");
        if(infobox.contains("birth_name") || infobox.contains("birth_date") ||
                infobox.contains("birth_place")) {
            return this.getCorefType();
        }

        return DefaultInfoboxValidator.NA;
    }
}
