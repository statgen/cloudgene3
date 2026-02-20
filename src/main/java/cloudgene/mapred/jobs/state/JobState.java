package cloudgene.mapred.jobs.state;

/**
 * Legacy state enum. Attempts to encode every state a job can be in into a single (integer) enum.
 * <p>
 * Fine-grained state is available by using {@link CompletionState}, {@link NotificationState}, and {@link SuccessState}.
 */
public enum JobState {

	/** Job hasn't started running yet (in queue). */
	WAITING(1),

	/** Job is currently running. */
	RUNNING(2),

	/** Seems to be a legacy value for the compression and zipping stage. Unused? */
	EXPORTING(3),

	/**
	 * Job finished successfully and has not been retired yet. Data is available for download.
	 * No email notification has been sent yet reminding the user of upcoming retirement.
	 */
	SUCCESS(4),

	/**
	 * Job failed and has not been retired yet.
	 * No email notification has been sent yet reminding the user of upcoming retirement.
	 */
	FAILED(5),

	/** The job was canceled before it finished running. */
	CANCELED(6),

	/** The job reached its retirement age and all artifacts have been deleted. */
	RETIRED(7),

	/**
	 * Job finished successfully and has not been retired yet. Data is available for download.
	 * The user has been notified via email that the job will be retired soon.
	 */
	SUCCESS_AND_NOTIFICATION_SENT(8),

	/**
	 * Job failed and has not been retired yet.
	 * The user has been notified via email that the job will be retired soon.
	 */
	FAILED_AND_NOTIFICATION_SENT(9),

	/** The job was manually deleted before its retirement (?). All artifacts have been deleted. */
	DELETED(10),

	/**
	 * The server went offline while this job was waiting or running. No artifacts were produced.
	 * Dead jobs can be re-run using {@code JobService.restart()}.
	 */
	DEAD(-1);

	private final int value;

	JobState(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static JobState of(int value) {
		for (JobState state : values()) {
			if (value == state.getValue()) {
				return state;
			}
		}

		throw new IllegalArgumentException("Value is not a known JobState: " + value);
	}
}
