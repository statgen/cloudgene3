package cloudgene.mapred.util;

import io.micronaut.core.annotation.NonNull;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public final class TextUtil {

	private TextUtil() {}

	/**
	 * Reads the last {@code lines} lines from the provided {@code file} (just like the {@code tail} command).
	 * <p>
	 * Converts newlines to UNIX-style (only line feed: {@code \n}). Ignores trailing newlines. UTF-8 is assumed.
	 * <p>
	 * If {@code lines} is greater than the actual line count in {@code file}, the whole file is returned.
	 *
	 * @param file  File whose tail we want to read. Must be non-null and correspond to an actual file in the filesystem.
	 * @param lines Number of lines from {@code file} to print. Must be strictly positive.
	 * @return The {@code file} tail.
	 * @throws IllegalArgumentException if {@code file} is null or not present in the filesystem; or if {@code lines} is less than 1.
	 * @throws IOException              if any issues are encountered reading from the filesystem.
	 */
	public static String tail(@NonNull File file, int lines) throws IOException {
		if (!file.isFile()) {
			throw new IllegalArgumentException("'file' is not a file in the filesystem: " + file);
		}

		if (lines <= 0) {
			throw new IllegalArgumentException("'lines' must be > 0; found: " + lines);
		}

		try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
				.setBufferSize(4096)
				.setCharset(StandardCharsets.UTF_8)
				.setFile(file)
				.get()) {

			List<String> lineList = reader.readLines(lines);
			Collections.reverse(lineList);

			return String.join("\n", lineList);
		}
	}
}
