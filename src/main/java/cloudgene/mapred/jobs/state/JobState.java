package cloudgene.mapred.jobs.state;

/**
 * Legacy state enum. Attempts to encode every state a job can be in into a single (integer) enum.
 */
public enum JobState {

	/** Job hasn't started running yet (in queue). */
	STATE_WAITING(1),

	/** Job is currently running. */
	STATE_RUNNING(2),

	/** Seems to be a legacy value for the compression and zipping stage. Unused? */
	STATE_EXPORTING(3),

	/**
	 * Job finished successfully and has not been retired yet. Data is available for download.
	 * No email notification has been sent yet reminding the user of upcoming retirement.
	 */
	STATE_SUCCESS(4),

	/**
	 * Job failed and has not been retired yet.
	 * No email notification has been sent yet reminding the user of upcoming retirement.
	 */
	STATE_FAILED(5),

	/** The job was canceled before it finished running. */
	STATE_CANCELED(6),

	/** The job reached its retirement age and all artifacts have been deleted. */
	STATE_RETIRED(7),

	/**
	 * Job finished successfully and has not been retired yet. Data is available for download.
	 * The user has been notified via email that the job will be retired soon.
	 */
	STATE_SUCCESS_AND_NOTIFICATION_SEND(8),

	/**
	 * Job failed and has not been retired yet.
	 * The user has been notified via email that the job will be retired soon.
	 */
	STATE_FAILED_AND_NOTIFICATION_SEND(9),

	/** The job was manually deleted before its retirement (?). All artifacts have been deleted. */
	STATE_DELETED(10),

	/**
	 * The server went offline while this job was waiting or running. No artifacts were produced.
	 * Dead jobs can be re-run using {@code JobService.restart()}.
	 */
	STATE_DEAD(-1);

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
