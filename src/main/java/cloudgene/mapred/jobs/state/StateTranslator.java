package cloudgene.mapred.jobs.state;

public final class StateTranslator {

	private StateTranslator() {
	}

	public record StateResult(CompletionState completion, SuccessState success, NotificationState notification) {
	}

	/**
	 * Converts the old-style {@code jobState} into a triple of
	 * {@code (CompletionState, SuccessState, NotificationState)}. Due to
	 * ambiguities in the conversion, sometimes {@code null} is returned to indicate
	 * that the previous value should be preserved.
	 */
	public static StateResult old2New(JobState jobState) {
		switch (jobState) {
			case WAITING:
				return new StateResult(
						CompletionState.SUBMITTED,
						SuccessState.PENDING,
						NotificationState.PENDING);

			case RUNNING:
			case EXPORTING:
				return new StateResult(
						CompletionState.RUNNING,
						SuccessState.PENDING,
						NotificationState.PENDING);

			case SUCCESS:
				return new StateResult(
						CompletionState.COMPLETE,
						SuccessState.SUCCEEDED,
						NotificationState.PENDING);

			case FAILED:
				return new StateResult(
						CompletionState.COMPLETE,
						SuccessState.FAILED,
						NotificationState.PENDING);

			case CANCELED:
				return new StateResult(
						CompletionState.COMPLETE,
						SuccessState.CANCELED,
						NotificationState.PENDING);

			case RETIRED:
				return new StateResult(
						CompletionState.RETIRED,
						null,
						null);

			case DELETED:
				return new StateResult(
						CompletionState.DELETED,
						null,
						null);

			case SUCCESS_AND_NOTIFICATION_SENT:
				return new StateResult(
						CompletionState.COMPLETE,
						SuccessState.SUCCEEDED,
						NotificationState.SENT_RETIREMENT_REMINDER);

			case FAILED_AND_NOTIFICATION_SENT:
				return new StateResult(
						CompletionState.COMPLETE,
						null, // This is also set when the job was CANCELED.
						NotificationState.SENT_RETIREMENT_REMINDER);

			case DEAD:
				return new StateResult(
						CompletionState.SUBMITTED, // Didn't finish, not running, can be re-run.
						SuccessState.DEAD,
						NotificationState.PENDING);

			default:
				throw new IllegalArgumentException("Unrecognized job state: " + jobState);
		}
	}
}
