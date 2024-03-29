package wec.config;

import wec.validators.DefaultInfoboxValidator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InfoboxConfiguration {
    // default (will be overridden by infobox_config.json)
    String infoboxLangText = "Infobox";
    private List<InfoboxConfig> infoboxConfigs;

    public String getInfoboxLangText() {
        return infoboxLangText;
    }

    public void setInfoboxLangText(String infoboxLangText) {
        this.infoboxLangText = infoboxLangText;
    }

    public List<InfoboxConfig> getInfoboxConfigs() {
        return infoboxConfigs;
    }

    public List<InfoboxConfig> getActiveInfoboxConfigs() {
        List<InfoboxConfig> onlyValid = new ArrayList<>(this.infoboxConfigs);
        onlyValid.removeIf(infobox -> !infobox.include);
        return onlyValid;
    }

    public InfoboxConfiguration.InfoboxConfig getInfoboxConfigByCoref(String corefType) {
        for(InfoboxConfiguration.InfoboxConfig infoboxConfig : this.infoboxConfigs) {
            if(infoboxConfig.getCorefType().equals(corefType)) {
                return infoboxConfig;
            }
        }

        return null;
    }

    public void setInfoboxConfigs(List<InfoboxConfig> infoboxConfigs) {
        this.infoboxConfigs = infoboxConfigs;
    }

    private DefaultInfoboxValidator initValidatorAndGet(InfoboxConfig locConfig) {
        DefaultInfoboxValidator extractor = locConfig.getExtractor();
        if (extractor == null) {
            String regex = "\\{\\{" + this.infoboxLangText.toLowerCase() +
                    "[\\w|]*?(" + String.join("|", locConfig.getInfoboxs()) + ")";
            Pattern pattern = Pattern.compile(regex);
            if (locConfig.getUseValidatorClass() != null && !locConfig.getUseValidatorClass().isEmpty()) {
                try {
                    Constructor<?>[] constructors = Class.forName(locConfig.getUseValidatorClass()).getConstructors();
                    extractor = (DefaultInfoboxValidator) constructors[0].newInstance(locConfig.getCorefType(), pattern);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                extractor = new DefaultInfoboxValidator(locConfig.getCorefType(), pattern);
            }
            locConfig.extractor = extractor;
        }

        return extractor;
    }

    public DefaultInfoboxValidator getExtractorByCorefType(String corefType) {
        for(InfoboxConfig locConfig : this.infoboxConfigs) {
            if(locConfig.getCorefType().equals(corefType)) {
                return this.initValidatorAndGet(locConfig);
            }
        }

        return null;
    }

    public List<DefaultInfoboxValidator> getAllIncludedValidators() {
        List<DefaultInfoboxValidator> includedValidators = new ArrayList<>();
        for(InfoboxConfig locConfig : this.infoboxConfigs) {
            if(locConfig.isInclude()) {
                includedValidators.add(this.initValidatorAndGet(locConfig));
            }
        }

        return includedValidators;
    }

    public static class InfoboxConfig {
        private String corefType;
        private boolean include;
        private String useValidatorClass;
        private List<String> infoboxs;

        private transient DefaultInfoboxValidator extractor;

        public String getCorefType() {
            return corefType;
        }

        public void setCorefType(String corefType) {
            this.corefType = corefType;
        }

        public boolean isInclude() {
            return include;
        }

        public void setInclude(boolean include) {
            this.include = include;
        }

        public String getUseValidatorClass() {
            return useValidatorClass;
        }

        public void setUseValidatorClass(String useValidatorClass) {
            this.useValidatorClass = useValidatorClass;
        }

        public List<String> getInfoboxs() {
            return infoboxs;
        }

        public void setInfoboxs(List<String> infoboxs) {
            this.infoboxs = infoboxs;
        }

        private DefaultInfoboxValidator getExtractor() {
            return this.extractor;
        }
    }
}
