package wec;

import data.RawElasticResult;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import wec.validators.TimeSpan1MonthInfoboxValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class TestTimeExtraction {

    @Test
    public void testIsSpanSingleMonth() throws IOException {
        InputStream inputStream = TestWikipediaLinkExtractor.class.getClassLoader().getResourceAsStream("time/time_expr_2d.txt");
        assert inputStream != null;
        List<String> strings = IOUtils.readLines(new InputStreamReader(inputStream));
        TimeSpan1MonthInfoboxValidator extractor = new TimeSpan1MonthInfoboxValidator(null, null);

        for (String line : strings) {
            boolean spanSingleMonth = extractor.isSpanSingleMonth(line);
            Assert.assertTrue(line, spanSingleMonth);
        }
    }

    @Test
    public void testIsNotSpanSingleMonth() throws IOException {
        InputStream inputStream = TestWikipediaLinkExtractor.class.getClassLoader().getResourceAsStream("time/time_expr_unk.txt");
        assert inputStream != null;
        List<String> strings = IOUtils.readLines(new InputStreamReader(inputStream));
        TimeSpan1MonthInfoboxValidator extractor = new TimeSpan1MonthInfoboxValidator(null, null);

        for (String line : strings) {
            boolean spanSingleDay = extractor.isSpanSingleMonth(line);
            Assert.assertFalse(line, spanSingleDay);
        }
    }

    @Test
    public void testExtractDateFromInfobox() {
        TimeSpan1MonthInfoboxValidator extractor = new TimeSpan1MonthInfoboxValidator(null, null);
        final List<RawElasticResult> sportText = getTimeFullPages();
        for(RawElasticResult text : sportText) {
            boolean spanSingleMonth = extractor.isSpanSingleMonth(text.getInfobox());
            Assert.assertFalse(text.getTitle(), spanSingleMonth);
        }
    }

    private List<RawElasticResult> getTimeFullPages() {
        return TestUtils.getTextAndTitle("time/page_time_extract.json");
    }
}
