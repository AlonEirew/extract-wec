package wikilinks;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;

import java.io.IOException;

public class TestCreateWikiLinks {

    @Test
    public void testReadAll() throws IOException {
        final RestHighLevelClient elasticClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
        CreateWikiLinks createWikiLinks = new CreateWikiLinks();
        createWikiLinks.readAllAndPerisist();
    }
}
