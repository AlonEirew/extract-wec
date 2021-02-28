package wec.workers;

import wec.data.RawElasticResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoboxWorker extends AWorker {

    private static String infoboxLang = "infobox" ;

    private final Map<String, AtomicInteger> infoboxCounts;
    private final Pattern pattern = Pattern.compile("\\{\\{" + infoboxLang + "([\\w|]*?)\\n");

    public InfoboxWorker(List<RawElasticResult> rawElasticResults, Map<String, AtomicInteger> infoboxCounts) {
        super(rawElasticResults);
        this.infoboxCounts = infoboxCounts;
    }

    @Override
    public void run() {
        for(RawElasticResult rawResult : this.rawElasticResults) {
            String infoboxRaw = rawResult.getInfobox();
            String infoboxLow = infoboxRaw.toLowerCase().replaceAll(" ", "");
            Matcher matcher = this.pattern.matcher(infoboxLow);
            if(matcher.find()) {
                String infoboxMatch = matcher.group(1);
                if (infoboxMatch.contains("|")) {
                    String[] split = infoboxMatch.split("\\|");
                    for(int i = 0; i < split.length ; i++) {
                        updateInfoboxMap(split[i]);
                    }
                } else {
                    updateInfoboxMap(infoboxMatch);
                }
            }
        }
    }

    private void updateInfoboxMap(String infoboxMatch) {
        if (!infoboxMatch.isEmpty()) {
            if (!this.infoboxCounts.containsKey(infoboxMatch)) {
                this.infoboxCounts.put(infoboxMatch, new AtomicInteger());
            }
            this.infoboxCounts.get(infoboxMatch).incrementAndGet();
        }
    }

    public static void setInfoboxLang(String infoboxLang) {
        InfoboxWorker.infoboxLang = infoboxLang;
    }
}
