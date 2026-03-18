package cloudgene.mapred.util;

import io.micronaut.core.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Record class for representing semver versions (see the
 * <a href="https://semver.org/">semver spec</a>). Aims to be fully
 * spec-compliant.
 * <p>
 * Semver versions take the form
 * {@code <major>.<minor>.<patch>[-<prerelease>][+<metadata>]}
 * <p>
 * Use {@link #of(String)} to parse a {@code String} into a {@code SemVer}
 * object. {@code SemVer} objects are comparable, and the comparison follows the
 * semver spec.
 *
 * @param major      Major version. Changes in major indicate breaking API
 *                   changes.
 * @param minor      Minor version. Changes in minor indicate
 *                   backwards-compatible API changes.
 * @param patch      Patch version. Bug fixes and other changes that don't
 *                   meaningfully change the API.
 * @param preRelease Pre-release tag. Sequence of dot-separated identifiers.
 * @param metadata   Metadata tag (ignored for comparisons).
 */
public record SemVer(
		int major,
		int minor,
		int patch,
		@NonNull List<Identifier> preRelease,
		@NonNull List<Identifier> metadata)
		implements Comparable<SemVer> {

	/**
	 * Single part of the pre-release and/or metadata chains. Must match
	 * {@code [a-zA-Z0-9-]+}. Can be numeric or alpha (text).
	 * <p>
	 * Use {@link #isAlpha()} to determine what the contents are.
	 *
	 * @param alpha The identifier's alpha value (set to {@code null} if type is
	 *              numeric).
	 * @param num   The identifier's numeric value (set to {@code 0} if type is
	 *              alpha).
	 */
	public record Identifier(String alpha, int num) implements Comparable<Identifier> {

		/**
		 * Returns {@code true} if this identifier is alpha (text-like), {@code false}
		 * if this identifier is numeric (integer-like).
		 */
		public boolean isAlpha() {
			return alpha != null;
		}

		/**
		 * Returns a new alpha identifier with the given {@code alpha} value.
		 */
		public static Identifier makeAlpha(@NonNull String alpha) {
			if (alpha == null || alpha.isEmpty()) {
				throw new IllegalArgumentException("alpha must be non-null and non-empty");
			}

			return new Identifier(alpha, 0);
		}

		/**
		 * Returns a new numeric identifier with the given {@code num} value.
		 */
		public static Identifier makeNum(int num) {
			if (num < 0) {
				throw new IllegalArgumentException("num must be non-negative");
			}

			return new Identifier(null, num);
		}

		@Override
		public int compareTo(@NonNull Identifier that) {
			if (this.isAlpha()) {
				if (that.isAlpha()) {
					return this.alpha.compareTo(that.alpha); // Both alpha
				} else {
					return +1; // alpha is greater.
				}
			} else {
				if (that.isAlpha()) {
					return -1; // alpha is greater.
				} else {
					return Integer.compare(this.num, that.num); // Both numeric
				}
			}
		}

		@Override
		@NonNull
		public String toString() {
			if (isAlpha()) {
				return alpha;
			} else {
				return Integer.toString(num);
			}
		}
	}

	/**
	 * Helper class. Implements the parsing functionality for {@link #of(String)}.
	 */
	private static class Parser {

		@NonNull
		private final String version;

		private int i;

		public Parser(@NonNull String version) {
			this.version = version;
			i = 0;
		}

		/**
		 * Returns {@code true} if there are no more characters in the {@code version}
		 * string, or if we have encountered a null character.
		 */
		private boolean done() {
			return (i >= version.length()) || (version.charAt(i) == 0);
		}

		/**
		 * Returns the character at position {@code i} (defaults to a null character if
		 * we are done).
		 */
		private char peek() {
			if (i >= version.length()) {
				return 0;
			} else {
				return version.charAt(i);
			}
		}

		/**
		 * Steps {@code i} by 1, and returns the character at the new position (defaults
		 * to no step and returning a null character if we are done).
		 */
		private char advance() {
			if (done()) {
				return 0;
			}

			i += 1;

			return peek();
		}

		/**
		 * If the current character is {@code c}, steps by 1. Otherwise, throws an
		 * {@link IllegalArgumentException}.
		 */
		private void chompCharacter(char c) {
			if (peek() != c) {
				throw new IllegalArgumentException(
						String.format("Expected to find character '%c' at i=%d while parsing '%s'", c, i, version));
			}

			advance();
		}

		/**
		 * Returns {@code true} if {@code c} is an ASCII alphanumeric character (letter
		 * or number), or an ASCII dash.
		 * <p>
		 * These are the valid characters for an identifier in the pre-release or
		 * metadata sections.
		 */
		private boolean isValidIdentifier(char c) {
			return AsciiUtil.isAlNum(c) || (c == '-');
		}

		/**
		 * Does the main work of parsing the {@code version} contents into a
		 * {@link SemVer} object.
		 */
		public SemVer parse() {
			int major = parseNumber();
			chompCharacter('.');

			int minor = parseNumber();
			chompCharacter('.');

			int patch = parseNumber();

			List<Identifier> preRelease = List.of();
			if (peek() == '-') {
				chompCharacter('-');
				preRelease = parseIdentifiers();
			}

			List<Identifier> metadata = List.of();
			if (peek() == '+') {
				chompCharacter('+');
				metadata = parseIdentifiers();
			}

			return new SemVer(major, minor, patch, preRelease, metadata);
		}

		/**
		 * Attempts to parse a positive integer at the current position, consuming all
		 * used characters. Throws an {@link IllegalArgumentException} if no number can
		 * be parsed.
		 */
		private int parseNumber() {
			char c = peek();
			if (!AsciiUtil.isNumeral(c)) {
				throw new IllegalArgumentException(
						String.format("Expected to find a number at i=%d while parsing '%s'", i, version));
			}

			int value = (c - '0');

			while ((c = advance()) != 0) {
				if (!AsciiUtil.isNumeral(c)) {
					break;
				}
				value = 10 * value + (c - '0');
			}

			return value;
		}

		/**
		 * Attempts to parse a dot-separated list of identifiers (i.e., the pre-release
		 * or metadata tags). Throws an {@link IllegalArgumentException} if no
		 * identifiers can be parsed.
		 */
		private List<Identifier> parseIdentifiers() {
			List<Identifier> identifiers = new ArrayList<>();

			if (done()) {
				throw new IllegalArgumentException("No identifiers left to parse at i=" + i);
			}

			char c = peek();
			while (!done()) {
				boolean numeric = true;
				int value = 0;
				int start = i;

				while (!done() && isValidIdentifier(c)) {
					if (AsciiUtil.isNumeral(c)) {
						if (numeric) {
							value = 10 * value + (c - '0');
						}
					} else {
						numeric = false;
					}

					c = advance();
				}

				Identifier id;
				if (numeric) {
					id = Identifier.makeNum(value);
				} else {
					id = Identifier.makeAlpha(version.substring(start, i));
				}

				identifiers.add(id);

				if (c == '.') {
					c = advance();
				} else {
					break;
				}
			}

			return identifiers;
		}
	}

	@Override
	public int compareTo(@NonNull SemVer that) {
		int major = Integer.compare(this.major, that.major);
		if (major != 0) {
			return major;
		}

		int minor = Integer.compare(this.minor, that.minor);
		if (minor != 0) {
			return minor;
		}

		int patch = Integer.compare(this.patch, that.patch);
		if (patch != 0) {
			return patch;
		}

		// Metadata does not count for comparison.
		return compareIdentifierLists(this.preRelease, that.preRelease);
	}

	private static int compareIdentifierLists(
			@NonNull List<Identifier> first,
			@NonNull List<Identifier> second) {

		if (first.isEmpty()) {
			if (second.isEmpty()) {
				return 0; // Both empty.
			} else {
				// this does not have identifiers -> this is greater (semver rules).
				return +1;
			}
		} else {
			if (second.isEmpty()) {
				// that does not have identifiers -> that is greater (semver rules).
				return -1;
			} else {
				// Comprare segment by segment until we find a winner.
				int min = Math.min(first.size(), second.size());
				for (int i = 0; i < min; i++) {
					int sign = first.get(i).compareTo(second.get(i));
					if (sign != 0) {
						return sign;
					}
				}

				// If everything else is a tie, size wins.
				return Integer.compare(first.size(), second.size());
			}
		}
	}

	@Override
	@NonNull
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(major);
		builder.append('.');
		builder.append(minor);
		builder.append('.');
		builder.append(patch);

		if (!preRelease.isEmpty()) {
			builder.append('-');
			builder.append(preRelease.get(0).toString());

			for (int i = 1; i < preRelease.size(); i++) {
				builder.append('.');
				builder.append(preRelease.get(i).toString());
			}
		}

		if (!metadata.isEmpty()) {
			builder.append('+');
			builder.append(metadata.get(0).toString());

			for (int i = 1; i < metadata.size(); i++) {
				builder.append('.');
				builder.append(metadata.get(i).toString());
			}
		}

		return builder.toString();
	}

	/**
	 * Attempts to parse {@code version} into a valid {@link SemVer} object. On
	 * failure, throws {@link IllegalArgumentException}.
	 * <p>
	 * Full format: {@code <major>.<minor>.<patch>[-<pre-release>][+<metadata>]},
	 * where {@code <major>.<minor>.<patch>} are three mandatory non-negative
	 * integers, and both {@code <pre-release>} and {@code <metadata>} are optional
	 * dot-separated sequences of either positive integers or alphanumeric
	 * identifiers (dashes allowed).
	 */
	public static SemVer of(@NonNull String version) {
		if (version == null || version.isBlank()) {
			throw new IllegalArgumentException("version must be non-null and non-blank");
		}

		version = version.trim();
		return new Parser(version).parse();
	}

	/**
	 * Returns a simple {@link SemVer} object with the given
	 * {@code <major>.<minor>.<patch>} version. Throws an
	 * {@link IllegalArgumentException} if any of the version numbers is negative.
	 * <p>
	 * For more complex versions, see {@link #of(String)}.
	 */
	public static SemVer of(int major, int minor, int patch) {
		if (major < 0) {
			throw new IllegalArgumentException("Major version must be >= 0; found: " + major);
		}
		if (minor < 0) {
			throw new IllegalArgumentException("Minor version must be >= 0; found: " + minor);
		}
		if (patch < 0) {
			throw new IllegalArgumentException("Patch version must be >= 0; found: " + patch);
		}

		return new SemVer(major, minor, patch, List.of(), List.of());
	}
}
