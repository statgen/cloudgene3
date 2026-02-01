package cloudgene.mapred.util;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public final class TextUtil {

	private TextUtil() {}

	private static final Logger log = LoggerFactory.getLogger(TextUtil.class);

	/**
	 * Reads the last {@code lines} lines from the provided {@code file} (just like
	 * the {@code tail} command).
	 *
	 * @param file  File whose tail we want to read.
	 * @param lines Number of lines from {@code file} to print.
	 * @return The {@code file} tail.
	 */
	public static String tail(@NotNull File file, int lines) {
		// TODO(Marc): This implementation seems very slow (seems to be copying one byte
		// at a time and then reversing a string). We should add tests and rewrite.
		try (java.io.RandomAccessFile fileHandler = new java.io.RandomAccessFile(file, "r")) {
			long fileLength = fileHandler.length() - 1;
			StringBuilder sb = new StringBuilder();
			int line = 0;

			for (long filePointer = fileLength; filePointer != -1; filePointer--) {
				fileHandler.seek(filePointer);
				int readByte = fileHandler.readByte();

				if (readByte == 0xA) {
					line = line + 1;
					if (line == lines) {
						if (filePointer == fileLength) {
							continue;
						}
						break;
					}
				} else if (readByte == 0xD) {
					line = line + 1;
					if (line == lines) {
						if (filePointer == fileLength - 1) {
							continue;
						}
						break;
					}
				}
				sb.append((char) readByte);
			}

			String lastLine = sb.reverse().toString();
			return lastLine;
		} catch (IOException e) {
			log.error("Parsing log file failed.", e);
			return null;
		}
	}
}
