package utils;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

public class StanfordNlpApi {
    private final static StanfordCoreNLP pipelineWithPos;
    private final static StanfordCoreNLP pipelineNoPos;

    static {
        Properties props1 = new Properties();
        props1.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
//        props1.setProperty("ner.docdate.usePresent", "true");
        props1.setProperty("sutime.includeRange", "true");
        props1.setProperty("sutime.markTimeRanges", "true");
        pipelineWithPos = new StanfordCoreNLP(props1);

        Properties props2 = new Properties();
        props2.setProperty("annotators", "tokenize, ssplit");
        pipelineNoPos = new StanfordCoreNLP(props2);
    }

    public static CoreDocument noPosAnnotate(String context) {
        CoreDocument doc = new CoreDocument(context);
        pipelineNoPos.annotate(doc);
        return doc;
    }

    public static CoreDocument withPosAnnotate(String context) {
        CoreDocument doc = new CoreDocument(context);
        pipelineWithPos.annotate(doc);
        return doc;
    }
}
