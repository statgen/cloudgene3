package cloudgene.mapred.jobs.state;

import io.micronaut.core.annotation.Nullable;

/**
 * String enum encoding the email notifications sent for a particular job:
 * <ul>
 *     <li>
 *         {@code PENDING}: Initial state. No notifications have been sent.
 *     </li>
 *     <li>
 *         {@code SENT_RETIREMENT_REMINDER}: A "job will be retired soon" notification was sent.
 *     </li>
 * </ul>
 */
public enum NotificationState {

	/** Initial state. No notifications have been sent. */
	PENDING("pending"),

	// NOTE(Marc): Currently, job completion emails are handled by the
	//             imputationserver2 Nextflow script and depend on settings that are not
	//             visible here.
	// /** A "job finished running" notification was sent. */
	// NOTIFIED_COMPLETION("notified_completion"),

	/** A "job will be retired soon" notification was sent. */
	SENT_RETIREMENT_REMINDER("sent_retirement_reminder");

	private final String value;

	NotificationState(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	/**
	 * Parses the given {@code value} into a {@link NotificationState}.
	 * <p>
	 * If {@code value} is blank or null, returns null. Otherwise, {@code value}
	 * should match one of the existing states (ignoring case and surrounding
	 * whitespace).
	 */
	@Nullable
	public static NotificationState of(@Nullable String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		value = value.trim().toLowerCase();

		for (NotificationState state : values()) {
			if (value.equals(state.value)) {
				return state;
			}
		}

		throw new IllegalArgumentException("value not recognized: " + value);
	}
}
