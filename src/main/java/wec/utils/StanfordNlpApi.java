package wec.utils;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

public class StanfordNlpApi {
    private final static StanfordCoreNLP pipelineWithPos;

    static {
        Properties props1 = new Properties();
        props1.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, regexner, depparse");
//        props1.setProperty("ner.docdate.usePresent", "true");
        props1.setProperty("sutime.includeRange", "true");
        props1.setProperty("sutime.markTimeRanges", "true");
        pipelineWithPos = new StanfordCoreNLP(props1);
    }

    public static CoreDocument withPosAnnotate(String context) {
        if(!context.isEmpty()) {
            CoreDocument doc = new CoreDocument(context);
            pipelineWithPos.annotate(doc);
            return doc;
        }
        return null;
    }

    public static StanfordCoreNLP getPipelineWithPos() {
        return pipelineWithPos;
    }
}
