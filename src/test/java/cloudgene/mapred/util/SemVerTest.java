package cloudgene.mapred.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SemVerTest {

	private static Stream<Arguments> provideForOf() {
		return Stream.of(
				// Null or empty ->
				arguments(null, null, "version must be non-null"),
				arguments("", null, "version must be non-null"),
				arguments(" \n\t  ", null, "version must be non-null"),

				arguments("a", null, "Expected to find a number"), // Major missing
				arguments("0.B", null, "Expected to find a number"), // Minor missing
				arguments("0..0", null, "Expected to find a number"), // Minor missing
				arguments("1.2.cd", null, "Expected to find a number"), // Patch missing

				arguments("4!5!6", null, "Expected to find character '.'"), // Wrong separators

				arguments(
						"87.65.43",
						new SemVer(87, 65, 43, List.of(), List.of()),
						null),
				arguments(
						"1.23.456",
						new SemVer(1, 23, 456, List.of(), List.of()),
						null),
				arguments(
						"9.0.2-foo",
						new SemVer(
								9, 0, 2,
								identifiers("foo"),
								List.of()),
						null),
				arguments(
						"9.0.2-42",
						new SemVer(
								9, 0, 2,
								identifiers(42),
								List.of()),
						null),
				arguments(
						"9.0.2+bar",
						new SemVer(
								9, 0, 2,
								List.of(),
								identifiers("bar")),
						null),
				arguments(
						"9.0.2+1989",
						new SemVer(
								9, 0, 2,
								List.of(),
								identifiers(1989)),
						null),
				arguments(
						"9.0.2-foo+bar",
						new SemVer(
								9, 0, 2,
								identifiers("foo"),
								identifiers("bar")),
						null),

				arguments(
						"9.0.2-foo.2.qwerty.99",
						new SemVer(
								9, 0, 2,
								identifiers("foo", 2, "qwerty", 99),
								List.of()),
						null));
	}

	@ParameterizedTest
	@MethodSource("provideForOf")
	public void testOf(String version, SemVer parsed, String errMsg) {
		try {
			SemVer observed = SemVer.of(version);

			assertNull(errMsg);
			assertEquals(parsed, observed);
		} catch (IllegalArgumentException e) {
			assertNotNull(errMsg);
			assertTrue(e.getMessage().startsWith(errMsg));
		}
	}

	private static Stream<Arguments> provideForCompareTo() {
		return Stream.of(
				// Identical maps to 0
				arguments(SemVer.of("0.0.0"), SemVer.of("0.0.0"), 0),
				arguments(SemVer.of("43.52.601"), SemVer.of("43.52.601"), 0),
				arguments(SemVer.of("8.8.8-foo"), SemVer.of("8.8.8-foo"), 0),
				arguments(SemVer.of("999.99.9+bar"), SemVer.of("999.99.9+bar"), 0),
				arguments(SemVer.of("987.65.4-foo.2+bar.3"), SemVer.of("987.65.4-foo.2+bar.3"), 0),

				// Major is #1
				arguments(SemVer.of("2.2.2"), SemVer.of("3.2.2"), -1),
				arguments(SemVer.of("66.2.2"), SemVer.of("0.2.2"), +1),
				arguments(SemVer.of("4.7.6"), SemVer.of("5.1.2"), -1),
				arguments(SemVer.of("8.0.0"), SemVer.of("3.99.99"), +1),

				// Minor is #2
				arguments(SemVer.of("0.2.1"), SemVer.of("0.5.1"), -1),
				arguments(SemVer.of("64.14.0"), SemVer.of("64.13.999"), +1),

				// Patch is #3
				arguments(SemVer.of("7.7.6"), SemVer.of("7.7.7"), -1),
				arguments(SemVer.of("123.45.6"), SemVer.of("123.45.4"), +1),

				// Pre-release is #4. If present, has lower precedence than if missing.
				arguments(SemVer.of("1.2.3"), SemVer.of("1.2.3-foo"), +1),
				arguments(SemVer.of("0.0.0-42"), SemVer.of("0.0.0"), -1),
				arguments(SemVer.of("2.1.1-foo"), SemVer.of("1.99.99"), +1),
				arguments(SemVer.of("3.3.3"), SemVer.of("3.4.3-123"), -1),
				arguments(SemVer.of("5.5.6-bar.7"), SemVer.of("5.5.5"), +1),

				// Alpha identifiers are compared alphabetically, num identifiers are compared
				// numerically.
				arguments(SemVer.of("0.0.0-aaa"), SemVer.of("0.0.0-bbb"), -1),
				arguments(SemVer.of("0.0.0-10"), SemVer.of("0.0.0-9"), +1),

				// Identifiers are separated by dots. They're compared one-by-one.
				arguments(SemVer.of("7.7.7-foo.8"), SemVer.of("7.7.7-bar.12"), +1),
				arguments(SemVer.of("7.7.7-hi.8"), SemVer.of("7.7.7-hi.12"), -1),

				// The tie-breaker is the number of identifiers.
				arguments(SemVer.of("7.7.7-hi.8"), SemVer.of("7.7.7-hi.8.there"), -1),

				// Metadata is ignored
				arguments(SemVer.of("4.7.3"), SemVer.of("4.7.3+foo"), 0),
				arguments(SemVer.of("3.8.5+305"), SemVer.of("3.8.5"), 0),
				arguments(SemVer.of("1.0.0"), SemVer.of("0.1.0+foo"), +1),
				arguments(SemVer.of("0.1.0+bar.2"), SemVer.of("0.2.0+foo.3"), -1),
				arguments(SemVer.of("0.0.1+bar.2"), SemVer.of("0.0.0+foo.3"), +1));
	}

	@ParameterizedTest
	@MethodSource("provideForCompareTo")
	public void testCompareTo(SemVer first, SemVer second, int sign) {
		int observed = first.compareTo(second);
		assertEquals(sign, Integer.signum(observed));
	}

	private static List<SemVer.Identifier> identifiers(Object... ids) {
		List<SemVer.Identifier> out = new ArrayList<>();
		for (Object id : ids) {
			if (id instanceof String alpha) {
				out.add(SemVer.Identifier.makeAlpha(alpha));
			} else if (id instanceof Integer num) {
				out.add(SemVer.Identifier.makeNum(num));
			} else {
				throw new IllegalArgumentException("Unrecognized id type: " + id.getClass().getName());
			}
		}
		return out;
	}
}
