package data;

import wec.DefaultInfoboxExtractor;

import java.lang.reflect.Constructor;
import java.util.List;

public class InfoboxConfiguration {
    private List<InfoboxConfig> infoboxConfigs;

    public List<InfoboxConfig> getInfoboxConfigs() {
        return infoboxConfigs;
    }

    public void setInfoboxConfigs(List<InfoboxConfig> infoboxConfigs) {
        this.infoboxConfigs = infoboxConfigs;
    }

    public static class InfoboxConfig {
        private String corefType;
        private boolean include;
        private String useExtractorClass;
        private List<String> infoboxs;

        private transient DefaultInfoboxExtractor extractor;

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

        public DefaultInfoboxExtractor getExtractor() {
            if(this.extractor == null) {
                if (this.useExtractorClass != null && !this.useExtractorClass.isEmpty()) {
                    try {
                        Constructor<?>[] constructors = Class.forName(this.useExtractorClass).getConstructors();
                        this.extractor = (DefaultInfoboxExtractor) constructors[0].newInstance(this.corefType, this.infoboxs);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    this.extractor = new DefaultInfoboxExtractor(this.corefType, this.infoboxs);
                }
            }
            return extractor;
        }

        public String getUseExtractorClass() {
            return useExtractorClass;
        }

        public void setUseExtractorClass(String useExtractorClass) {
            this.useExtractorClass = useExtractorClass;
        }

        public List<String> getInfoboxs() {
            return infoboxs;
        }

        public void setInfoboxs(List<String> infoboxs) {
            this.infoboxs = infoboxs;
        }
    }
}
