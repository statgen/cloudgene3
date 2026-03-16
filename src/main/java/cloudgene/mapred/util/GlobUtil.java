package cloudgene.mapred.util;

import io.micronaut.core.annotation.NonNull;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

public final class GlobUtil {

	private GlobUtil() {}

	/**
	 * Checks if {@code filename} should be included, based on the given
	 * {@code includes} and {@code excludes} globs.
	 * <p>
	 * To be included, the {@code filename} should match at least one glob in
	 * {@code includes} (unless {@code includes} is empty, in which case it's
	 * considered a match), and should not match any globs in {@code excludes}.
	 *
	 * @param filename The file path under consideration.
	 * @param includes Inclusion criteria (as path globs). If empty, all files pass
	 *                 the inclusion test. If at least one element is provided, the
	 *                 {@code filename} should match one or more globs.
	 * @param excludes Exclusion criteria (as path globs). The {@code filename}
	 *                 should not match any globs.
	 * @return {@code true} if the {@code filename} should be included,
	 *         {@code false} otherwise.
	 */
	public static boolean isFileIncluded(
			@NonNull String filename,
			@NonNull List<String> includes,
			@NonNull List<String> excludes) {

		// If includes is empty, consider all files as included
		boolean isIncluded = includes.isEmpty()
				|| includes.stream().anyMatch(pattern -> matchPattern(filename, pattern));

		// If it doesn't match any include pattern, return false
		if (!isIncluded) {
			return false;
		}

		// If excludes is empty, no file is excluded
		boolean isExcluded = !excludes.isEmpty()
				&& excludes.stream().anyMatch(pattern -> matchPattern(filename, pattern));

		// The file should be included if it is in the includes list and not in the
		// excludes list
		return !isExcluded;
	}

	private static boolean matchPattern(@NonNull String filename, @NonNull String pattern) {
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		return matcher.matches(Paths.get(filename));
	}
}
