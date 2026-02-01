package cloudgene.mapred.util;

public final class TimeUtil {

	private TimeUtil() {
	}

	/**
	 * Converts the given milliseconds into a {@code String} showing hours, minutes,
	 * and seconds. Output format: {@code [H h ][M min ]S sec}, where hours and/or
	 * minutes are only shown if that segment is nonzero.
	 *
	 * @param millis Time span to represent, as total milliseconds.
	 * @return {@code [H h ][M min ]S sec}
	 */
	public static String format(long millis) {
		long totalSeconds = millis / 1000;
		long totalMinutes = totalSeconds / 60;

		long hours = totalMinutes / 60;
		long minutes = totalMinutes % 60;
		long seconds = totalSeconds % 60;

		String out = seconds + " sec";
		if (minutes > 0)
			out = minutes + " min " + out;
		if (hours > 0)
			out = hours + " h " + out;

		return out;
	}
}
