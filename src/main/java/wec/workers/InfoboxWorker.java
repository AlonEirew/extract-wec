package wec.workers;

import wec.data.RawElasticResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoboxWorker extends AWorker {

    private static String infoboxLang = "infobox" ;

    private static final Map<String, AtomicInteger> infoboxCounts = new ConcurrentHashMap<>();
    private final Pattern pattern = Pattern.compile("\\{\\{" + infoboxLang + "([\\w|]*?)\\n");

    @Override
    public void run() {
        for(RawElasticResult rawResult : this.getRawElasticResults()) {
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

        invokeListener();
    }

    private void updateInfoboxMap(String infoboxMatch) {
        if (!infoboxMatch.isEmpty()) {
            if (!infoboxCounts.containsKey(infoboxMatch)) {
                infoboxCounts.put(infoboxMatch, new AtomicInteger());
            }
            infoboxCounts.get(infoboxMatch).incrementAndGet();
        }
    }

    public static void setInfoboxLang(String infoboxLang) {
        InfoboxWorker.infoboxLang = infoboxLang;
    }

    public static Map<String, AtomicInteger> getInfoboxCounts() {
        return infoboxCounts;
    }
}
