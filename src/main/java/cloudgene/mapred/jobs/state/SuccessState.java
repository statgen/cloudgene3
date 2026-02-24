package cloudgene.mapred.jobs.state;

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

	public static SuccessState of(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("value must be non-null and non-blank.");
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
