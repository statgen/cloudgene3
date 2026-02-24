package cloudgene.mapred.jobs.state;

/**
 * String enum encoding the completion state of a particular job:
 * <ul>
 *     <li>
 *         {@code SUBMITTED}: Initial state. The job is waiting and has not started running.
 *     </li>
 *     <li>
 *         {@code RUNNING}: The job is currently running.
 *     </li>
 *     <li>
 *         {@code COMPLETE}: The job has already stopped running, independent of success status.
 * 	       It could also have been canceled (see {@link SuccessState}).
 *     </li>
 *     <li>
 *         {@code RETIRED}: The job is still visible, but its data is no longer available
 * 	       (the job was automatically retired due to age, or an admin archived it).
 *     </li>
 *     <li>
 *         {@code DELETED}: The job was deleted (by the user o an admin) and can no longer be
 * 	       seen in the UI or API. Some information may remain in the database (soft-delete).
 *     </li>
 * </ul>
 */
public enum CompletionState {

	/** Initial state. The job is waiting and has not started running. */
	SUBMITTED("submitted"),

	/** The job is currently running. */
	RUNNING("running"),

	/**
	 * The job has already stopped running, independent of success status.
	 * It could also have been canceled (see {@link SuccessState}).
	 */
	COMPLETE("complete"),

	/**
	 * The job is still visible, but its data is no longer available (the job was
	 * automatically retired due to age, or an admin archived it).
	 */
	RETIRED("retired"),

	/**
	 * The job was deleted (by the user o an admin) and can no longer be seen in the
	 * UI or API. Some information may remain in the database (soft-delete).
	 */
	DELETED("deleted");

	private final String value;

	CompletionState(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static CompletionState of(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("value must be non-null and non-blank.");
		}

		value = value.trim().toLowerCase();

		for (CompletionState state : values()) {
			if (value.equals(state.value)) {
				return state;
			}
		}

		throw new IllegalArgumentException("value not recognized: " + value);
	}
}
