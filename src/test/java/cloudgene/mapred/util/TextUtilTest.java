package cloudgene.mapred.util;

import genepi.io.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TextUtilTest {

	@Test
	public void testTail() throws IOException {
		File file = new File("test-data/all-possible-inputs.yaml");
		String fullContents = FileUtil.readFileAsString(file.getPath()).replace("\r", "").trim();

		String expected;
		String observed;

		// Asking for 5 lines gives us the last 5 lines (ignoring the trailing newline).
		expected =
				"    - id: output\n" +
						"      description: OutputFile\n" +
						"      type: file\n" +
						"      download: true\n" +
						"      temp: false";
		observed = TextUtil.tail(file, 5);
		assertEquals(expected, observed);

		// Asking for 2 lines gives us the last 2 lines (ignoring the trailing newline).
		expected = "      download: true\n" +
				"      temp: false";
		observed = TextUtil.tail(file, 2);
		assertEquals(expected, observed);

		// Asking for 1 line gives us the last line (ignoring the trailing newline).
        expected = "      temp: false";
        observed = TextUtil.tail(file, 1);
        assertEquals(expected, observed);

		// Asking for <= 0 lines throws an IllegalArgumentException.
		assertThrows(IllegalArgumentException.class, () -> TextUtil.tail(file, 0));

		// Asking for <= 0 lines throws an IllegalArgumentException.
		assertThrows(IllegalArgumentException.class, () -> TextUtil.tail(file, -23));

		// Asking for more lines than the file has returns the whole file.
		expected = fullContents;
		observed = TextUtil.tail(file, 999);
		assertEquals(expected, observed);
	}
}
