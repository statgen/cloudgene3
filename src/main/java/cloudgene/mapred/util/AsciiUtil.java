package cloudgene.mapred.util;

/**
 * Utility static class for querying ASCII character properties.
 * <p>
 * We only need it because the Java standard library does Unicode-everything by
 * default and does not provide a fallback.
 */
public final class AsciiUtil {
	private AsciiUtil() {}

	/**
	 * Returns {@code true} if {@code c} is an ASCII numeral character
	 * ({@code '0'...'9'}).
	 */
	public static boolean isNumeral(char c) {
		return ('0' <= c) && (c <= '9');
	}

	/**
	 * Returns {@code true} if {@code c} is an ASCII lowercase character
	 * ({@code 'a'...'z'}).
	 */
	public static boolean isLower(char c) {
		return ('a' <= c) && (c <= 'z');
	}

	/**
	 * Returns {@code true} if {@code c} is an ASCII uppercase character
	 * ({@code 'A'...'Z'}).
	 */
	public static boolean isUpper(char c) {
		return ('A' <= c) && (c <= 'Z');
	}

	/**
	 * Returns {@code true} if {@code c} is an alphabetical character (lowercase or
	 * uppercase letter).
	 */
	public static boolean isAlpha(char c) {
		return isLower(c) || isUpper(c);
	}

	/**
	 * Returns {@code true} if {@code c} is an alphanumeric character (a letter or a
	 * number).
	 */
	public static boolean isAlNum(char c) {
		return isAlpha(c) || isNumeral(c);
	}
}
