package wec.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import wec.data.WECContext;
import wec.data.WECCoref;
import wec.data.WECMention;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class DBRepository {
    @Autowired private ContextRepository contextRepository;
    @Autowired private CorefRepository corefRepository;
    @Autowired private MentionsRepository mentionRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(DBRepository.class);

    public DBRepository() {
    }

    public void saveContexts(Collection<WECContext> contextList) {
        Iterable<WECContext> wecContexts = this.contextRepository.saveAll(contextList);
        LOGGER.info(contextList.size() + " contexts committed to database");
        for (WECContext context : wecContexts) {
            for(WECMention mention : context.getMentionList()) {
                mention.setContextId(context.getContextId());
            }
        }
    }

    public void saveCorefAndMentions(Collection<WECCoref> corefs) {
        List<WECCoref> toPersist = new ArrayList<>();
        for(WECCoref coref : corefs) {
            if(!coref.isMarkedForRemoval()) {
                toPersist.add(coref);
            }
        }
        this.corefRepository.saveAll(toPersist);
        LOGGER.info(toPersist.size() + " corefs committed to database");
    }

    public Iterable<WECMention> findAllMentions() {
        return this.mentionRepository.findAll();
    }

    public Optional<WECContext> findContextById(long contextId) {
        return this.contextRepository.findById(contextId);
    }
}
