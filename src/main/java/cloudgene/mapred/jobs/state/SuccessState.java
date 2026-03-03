package cloudgene.mapred.jobs.state;

import io.micronaut.core.annotation.Nullable;

/**
 * String enum encoding the success state of a particular job:
 * <ul>
 *     <li>
 *         {@code PENDING}: Initial state. The job has not finished running yet.
 *     </li>
 *     <li>
 *         {@code SUCCEEDED}: The job finished running successfully.
 *     </li>
 *     <li>
 *         {@code FAILED}: The job encountered a critical error and failed.
 *     </li>
 *     <li>
 *         {@code CANCELED}: The job was manually canceled before it finished running.
 *     </li>
 *     <li>
 *         {@code DEAD} The job stopped running before completion because the server went down.
 *     </li>
 * </ul>
 */
public enum SuccessState {

	/** Initial state. The job has not finished running yet. */
	PENDING("pending"),

	/** The job finished running successfully. */
	SUCCEEDED("succeeded"),

	/** The job encountered a critical error and failed. */
	FAILED("failed"),

	/** The job was manually canceled before it finished running. */
	CANCELED("canceled"),

	/** The job stopped running before completion because the server went down. */
	DEAD("dead");

	private final String value;

	SuccessState(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Parses the given {@code value} into a {@link SuccessState}.
	 * <p>
	 * If {@code value} is blank or null, returns null. Otherwise, {@code value}
	 * should match one of the existing states (ignoring case and surrounding
	 * whitespace).
	 */
	@Nullable
	public static SuccessState of(@Nullable String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		value = value.trim().toLowerCase();

		for (SuccessState state : values()) {
			if (value.equals(state.value)) {
				return state;
			}
		}

		throw new IllegalArgumentException("value not recognized: " + value);
	}
}
