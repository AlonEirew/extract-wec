package wikilinks;

import org.junit.Test;
import persistence.ElasticFullDataReader;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TestElasticFullDataReader {

    @Test
    public void testReadAll() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ElasticFullDataReader elasticFullDataReader = new ElasticFullDataReader();
        elasticFullDataReader.readAll("localhost", 9200, "http", "enwiki_v2");
    }
}
