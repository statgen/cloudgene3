package cloudgene.mapred.util;

import genepi.io.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextUtilTest {

    @Test
    public void testTail() {
        File file = new File("test-data/all-possible-inputs.yaml");
        String fullContents = FileUtil.readFileAsString(file.getPath());

        String expected;
        String observed;

        // Asking for 5 lines gives us the last 5 lines (last line is empty).
        expected =
                "      description: OutputFile\n" +
                        "      type: file\n" +
                        "      download: true\n" +
                        "      temp: false\n";
        observed = TextUtil.tail(file, 5);
        assertEquals(expected, observed);

        // Asking for 2 lines gives us the last 2 lines (last line is empty).
        expected = "      temp: false\n";
        observed = TextUtil.tail(file, 2);
        assertEquals(expected, observed);

        // TODO(Marc): Passing lines=1 returns everything except the last newline.
        //             That's definitely weird and unintended.
//        expected = "\n";
//        expected = fullContents;
//        observed = TextUtil.tail(file, 1);
//        assertEquals(expected, observed);

        // Asking for <= 0 lines returns the whole file.
        expected = fullContents;
        observed = TextUtil.tail(file, 0);
        assertEquals(expected, observed);

        // Asking for <= 0 lines returns the whole file.
        expected = fullContents;
        observed = TextUtil.tail(file, -23);
        assertEquals(expected, observed);

        // Asking for more lines than the file has returns the whole file.
        expected = fullContents;
        observed = TextUtil.tail(file, 999);
        assertEquals(expected, observed);
    }
}
