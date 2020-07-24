package wec;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import wec.extractors.AttackInfoboxExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class TestTimeExtraction {

    @Test
    public void testIsSpanSingleMonth() throws IOException {
        InputStream inputStream = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream("time/time_expr_2d.txt");
        List<String> strings = IOUtils.readLines(new InputStreamReader(inputStream));
        AInfoboxExtractor extractor = new AttackInfoboxExtractor();

        for (String line : strings) {
            boolean spanSingleDay = extractor.isSpanSingleMonth(line);
            Assert.assertTrue(line, spanSingleDay);
        }
    }

    @Test
    public void testIsNotSpanSingleMonth() throws IOException {
        InputStream inputStream = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream("time/time_expr_unk.txt");
        List<String> strings = IOUtils.readLines(new InputStreamReader(inputStream));
        AInfoboxExtractor extractor = new AttackInfoboxExtractor();

        for (String line : strings) {
            boolean spanSingleDay = extractor.isSpanSingleMonth(line);
            Assert.assertFalse(line, spanSingleDay);
        }
    }
}
