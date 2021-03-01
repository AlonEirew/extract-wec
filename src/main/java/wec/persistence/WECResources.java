package wec.persistence;

import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;

@Component
public class WECResources {
    private static ElasticQueryApi elasticApi;

    private static EntityManager entityManager;
    private static MentionsRepository mentionsRepository;
    private static CorefRepository corefRepository;
    private static ContextRepository contextRepository;

    public static ElasticQueryApi getElasticApi() {
        return elasticApi;
    }

    public static void setElasticApi(ElasticQueryApi elasticApi) {
        WECResources.elasticApi = elasticApi;
    }

    public static void setMentionsRepository(MentionsRepository sMentionsRepository) {
        WECResources.mentionsRepository = sMentionsRepository;
    }

    public static MentionsRepository getMentionsRepository() {
        return mentionsRepository;
    }

    public static EntityManager getEntityManager() {
        return entityManager;
    }

    public static void setEntityManager(EntityManager sEntityManager) {
        WECResources.entityManager = sEntityManager;
    }

    public static CorefRepository getCorefRepository() {
        return corefRepository;
    }

    public static void setCorefRepository(CorefRepository corefRepository) {
        WECResources.corefRepository = corefRepository;
    }

    public static ContextRepository getContextRepository() {
        return contextRepository;
    }

    public static void setContextRepository(ContextRepository contextRepository) {
        WECResources.contextRepository = contextRepository;
    }

    public static void closeAllResources() {
        if(elasticApi != null) {
            elasticApi.close();
        }
    }
}
