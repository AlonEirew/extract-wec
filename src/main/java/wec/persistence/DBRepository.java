package wec.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import wec.data.WECContext;
import wec.data.WECCoref;
import wec.data.WECMention;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@Transactional
public class DBRepository {
    @Autowired private MentionsRepository mentionsRepository;
    @Autowired private ContextRepository contextRepository;
    @Autowired private CorefRepository corefRepository;
    @Autowired private EntityManager entityManager;


    public DBRepository() {
    }

    public void saveMentionsList(List<WECMention> mentionList) {
        if(!mentionList.isEmpty()) {
            Set<WECCoref> corefsToPersist = new HashSet<>();
            Set<WECContext> contextsToPersist = new HashSet<>();
            for (WECMention mention : mentionList) {
                if (!entityManager.contains(mention.getCorefChain())) {
                    corefsToPersist.add(mention.getCorefChain());
                }

                contextsToPersist.add(mention.getContext());
            }

            contextRepository.saveAll(contextsToPersist);
            corefRepository.saveAll(corefsToPersist);
            mentionsRepository.saveAll(mentionList);
        }
    }
}
