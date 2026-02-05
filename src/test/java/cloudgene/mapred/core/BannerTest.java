package cloudgene.mapred.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class BannerTest {

	@Test
	public void testTypeToString() {
		assertEquals(2, Banner.Type.values().length);

		// String form is full word in lowercase.
		assertEquals("warning", Banner.Type.WARNING.toString());
		assertEquals("danger", Banner.Type.DANGER.toString());
	}

	@Test
	public void testTypeOf() {
		// Matching WARNING: case and surrounding whitespace don't matter.
		for (String warn : Arrays.asList("warning", " warning", " warning\t\n", "WARNING", " \t wArNinG \n")) {
			Banner.Type type = Banner.Type.of(warn);
			assertEquals(Banner.Type.WARNING, type);
		}

		// Same for DANGER.
		for (String danger : Arrays.asList("danger", " danger", "DANGER\n", "\tDaNgEr\t")) {
			Banner.Type type = Banner.Type.of(danger);
			assertEquals(Banner.Type.DANGER, type);
		}

		// Null and blank strings are invalid.
		for (String blank : Arrays.asList(null, "", " ", "\t\n")) {
			IllegalArgumentException e = assertThrows(
					IllegalArgumentException.class,
					() -> Banner.Type.of(blank));

			assertEquals("Value must be a non-blank string.", e.getMessage());
		}

		// Unrecognized text (including partial matches) is invalid.
		for (String unknown : Arrays.asList("info", "FOO-BAR", "warn", "anger")) {
			IllegalArgumentException e = assertThrows(
					IllegalArgumentException.class,
					() -> Banner.Type.of(unknown));

			assertTrue(e.getMessage().startsWith("Unrecognized type: "));
		}
	}

	@Test
	public void testInitialValues() {
		Banner banner = new Banner(Banner.Type.WARNING, "This is a warning");

		// ID and position get initialized to -1 to signal "value has not been set yet".
		assertEquals(-1, banner.getId());
		assertEquals(-1, banner.getPosition());
	}
}
