package cloudgene.mapred.jobs.state;

/**
 * Legacy state enum. Attempts to encode every state a job can be in into a
 * single (integer) enum.
 * <p>
 * Fine-grained state is available by using {@link CompletionState},
 * {@link NotificationState}, and {@link SuccessState}.
 */
public enum JobState {

	/** The job hasn't started running yet (in queue). */
	WAITING(1),

	/**
	 * The job is currently running.
	 * <p>
	 * Maps to {@code AbstractJob.execute()}.
	 */
	RUNNING(2),

	/**
	 * The job is close to done: it completed the primary running phase and is
	 * exporting he produced artifacts.
	 * <p>
	 * Maps to {@code AbstractJob.after()}.
	 */
	EXPORTING(3),

	/**
	 * The job finished successfully and has not been retired yet. Data is available
	 * for download. No email notification has been sent yet reminding the user of
	 * upcoming retirement.
	 */
	SUCCESS(4),

	/**
	 * The job failed and has not been retired yet. No email notification has been
	 * sent yet reminding the user of upcoming retirement.
	 */
	FAILED(5),

	/** The job was canceled before it finished running. */
	CANCELED(6),

	/**
	 * The job reached its retirement age (or an admin archived it) and all artifacts
	 * have been deleted.
	 * <p>
	 * Retired jobs still show up in the UI, even though the data is no longer
	 * available.
	 */
	RETIRED(7),

	/**
	 * The job finished successfully and has not been retired yet. Data is available
	 * for download. The user has been notified via email that the job will be
	 * retired soon.
	 */
	SUCCESS_AND_NOTIFICATION_SENT(8),

	/**
	 * The job failed and has not been retired yet.
	 * The user has been notified via email that the job will be retired soon.
	 */
	FAILED_AND_NOTIFICATION_SENT(9),

	/**
	 * The job was manually deleted before its retirement (by the user or an admin)
	 * and all artifacts were deleted.
	 * <p>
	 * Unlike RETIRED jobs, DELETED jobs are hidden from the API and UI (it's a
	 * soft-delete).
	 */
	DELETED(10),

	/**
	 * The server went offline while this job was waiting or running. No artifacts
	 * were produced.
	 * <p>
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
