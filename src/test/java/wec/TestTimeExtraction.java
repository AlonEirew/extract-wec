package wec;

import data.InfoboxConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import wec.extractors.TimeSpan1MonthInfoboxExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.List;

public class TestTimeExtraction {

    @Test
    public void testIsSpanSingleMonth() throws IOException {
        InputStream inputStream = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream("time/time_expr_2d.txt");
        assert inputStream != null;
        List<String> strings = IOUtils.readLines(new InputStreamReader(inputStream));
        TimeSpan1MonthInfoboxExtractor extractor = new TimeSpan1MonthInfoboxExtractor(null, null, null);

        for (String line : strings) {
            boolean spanSingleMonth = extractor.isSpanSingleMonth(line);
            Assert.assertTrue(line, spanSingleMonth);
        }
    }

    @Test
    public void testIsNotSpanSingleMonth() throws IOException {
        InputStream inputStream = TestWECLinksExtractor.class.getClassLoader().getResourceAsStream("time/time_expr_unk.txt");
        assert inputStream != null;
        List<String> strings = IOUtils.readLines(new InputStreamReader(inputStream));
        TimeSpan1MonthInfoboxExtractor extractor = new TimeSpan1MonthInfoboxExtractor(null, null, null);

        for (String line : strings) {
            boolean spanSingleDay = extractor.isSpanSingleMonth(line);
            Assert.assertFalse(line, spanSingleDay);
        }
    }

    @Test
    public void testExtractDateFromInfobox() {
        InfoboxFilter filter = new InfoboxFilter(new InfoboxConfiguration());
        TimeSpan1MonthInfoboxExtractor extractor = new TimeSpan1MonthInfoboxExtractor(null, null, null);
        final List<AbstractMap.SimpleEntry<String, String>> sportText = getTimeFullPages();
        for(AbstractMap.SimpleEntry<String, String> text : sportText) {
            final String infoBox = filter.extractPageInfoBox(text.getValue());
            boolean spanSingleMonth = extractor.isSpanSingleMonth(infoBox);
            Assert.assertFalse(text.getKey(), spanSingleMonth);
        }
    }

    private List<AbstractMap.SimpleEntry<String, String>> getTimeFullPages() {
        return TestUtils.getTextAndTitle("time/page_time_extract.json");
    }
}
