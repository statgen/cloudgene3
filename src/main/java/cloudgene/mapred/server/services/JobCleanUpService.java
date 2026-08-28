package cloudgene.mapred.server.services;

import java.util.List;

import cloudgene.mapred.jobs.state.JobState;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.Template;
import cloudgene.mapred.database.dao.JobDao;
import cloudgene.mapred.database.dao.ParameterDao;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.workspace.WorkspaceFactory;
import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.util.MailUtil;
import cloudgene.mapred.util.config.Settings;
import genepi.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class JobCleanUpService {

	private static final Logger log = LoggerFactory.getLogger(JobCleanUpService.class);

	@Inject
	protected Application application;

	@Inject
	protected WorkspaceFactory workspaceFactory;

	public int executeRetire() {
		Database database = application.getDatabase();
		Settings settings = application.getSettings();

		JobDao dao = new JobDao(database);

		List<AbstractJob> oldJobs = dao.findAllNotifiedJobs();
		int deleted = 0;

		for (AbstractJob job : oldJobs) {
			if (job.getDeletedOn() < System.currentTimeMillis()) {
				// delete local directory
				String localOutput = FileUtil.path(settings.getLocalWorkspace(), job.getId());
				FileUtil.deleteDirectory(localOutput);

				job.setState(JobState.RETIRED);
				dao.update(job);

				log.info("Job {} retired.", job.getId());
				deleted++;

				// Clear sensitive data for all jobs that retire naturally due to age
				ParameterDao parameterDao = new ParameterDao(database);
				parameterDao.deleteSensitiveByJob(job);

				IWorkspace externalWorkspace = workspaceFactory.getByJob(job);

				try {
					externalWorkspace.delete(job.getId());
				} catch (Exception e) {
					log.error("Retire {} failed.", job.getId(), e);
				}
			}
		}

		log.info("{} jobs retired.", deleted);
		return deleted;
	}

	private enum NotifyResult {
		/** Set retirement date and sent notification for this SUCCESSFUL job. */
		SUCCESSFUL_JOB_NOTIFIED,

		/** Set retirement but did not notify this SUCCESSFUL job. */
		SUCCESSFUL_JOB_SKIPPED,

		/** Failed to send a notification for this SUCCESSFUL job. */
		SUCCESSFUL_JOB_ERROR,

		/** Set retirement date but did not notify this FAILED or CANCELED job. */
		FAILED_JOB_SKIPPED,

		/** This job has the wrong state to notify or update. */
		INCORRECT_JOB_STATE
	}

	private String getResultMessage(AbstractJob job, NotifyResult result) {
		return switch (result) {
			case SUCCESSFUL_JOB_NOTIFIED ->
				"Set deletion date and sent notification for successful job: " + job.getId();
			case SUCCESSFUL_JOB_SKIPPED -> "Set deletion date for successful job: " + job.getId()
					+ ". No notification sent.";
			case SUCCESSFUL_JOB_ERROR ->
				"Failed to send notification for successful job: " + job.getId();
			case FAILED_JOB_SKIPPED ->
				"Set deletion date for failed or canceled job: " + job.getId()
						+ ". No notification sent.";
			case INCORRECT_JOB_STATE ->
				"Job " + job.getId() + " has the wrong state to send a notification. "
						+ "It should be SUCCESS, FAILED, or CANCELED; found: " + job.getState() + ".";
		};
	}

	private NotifyResult notify(
			Settings settings,
			JobDao dao,
			AbstractJob job,
			int days) {

		long daysMillis = days * 24L * 60L * 60L * 1000L;
		long deletedOn = System.currentTimeMillis() + daysMillis;

		switch (job.getState()) {
			case SUCCESS -> {
				try {
					String userMail = job.getUser().getMail();

					boolean userHasMail = (userMail != null && !userMail.isBlank());
					boolean settingsHasMail = settings.getMail() != null;
					NotifyResult out = NotifyResult.SUCCESSFUL_JOB_SKIPPED;

					if (settingsHasMail && userHasMail) {
						String subject = "[" + settings.getName() + "] Job " + job.getId()
								+ " will be retired in " + days + " days";

						String body = application.getTemplate(
								Template.RETIRE_JOB_MAIL,
								job.getUser().getFullName(),
								days,
								job.getId());

						MailUtil.send(settings, userMail, subject, body);
						out = NotifyResult.SUCCESSFUL_JOB_NOTIFIED;
					}

					job.setState(JobState.SUCCESS_AND_NOTIFICATION_SENT);
					job.setDeletedOn(deletedOn);
					dao.update(job);

					return out;
				} catch (MessagingException e) {
					return NotifyResult.SUCCESSFUL_JOB_ERROR;
				}
			}

			case FAILED, CANCELED -> {
				job.setState(JobState.FAILED_AND_NOTIFICATION_SENT);
				job.setDeletedOn(deletedOn);
				dao.update(job);

				return NotifyResult.FAILED_JOB_SKIPPED;
			}

			default -> {
				return NotifyResult.INCORRECT_JOB_STATE;
			}
		}
	}

	/**
	 * Mark {@code job} for retirement in the indicated number of {@code days}, and
	 * send an email notification if applicable.
	 * <p>
	 * Valid job states for this operation are {@link JobState#SUCCESS},
	 * {@link JobState#FAILED}, and {@link JobState#CANCELED}. Only
	 * {@link JobState#SUCCESS} leads to an email notification (since other jobs
	 * don't have downloadable data).
	 * <p>
	 * The retirement timestamp is found by converting {@code days} to millis and
	 * offsetting from the current time.
	 * <p>
	 * This job never fails, it just returns different messages depending on the
	 * action taken.
	 *
	 * @param job  This job will be marked for deletion, and a notification email
	 *             may be sent to the user.
	 * @param days How many days from now until the job is deleted?
	 * @return A result message indicating if the job was marked for deletion, if a
	 *         notification was sent, etc.
	 */
	public String sendNotification(AbstractJob job, int days) {
		Settings settings = application.getSettings();
		JobDao dao = new JobDao(application.getDatabase());

		NotifyResult result = notify(settings, dao, job, days);
		return getResultMessage(job, result);
	}

	/**
	 * Mark all applicable jobs in the database for deletion, and notify the
	 * appropriate users.
	 * <p>
	 * Jobs are processed if their state is one of {@link JobState#SUCCESS},
	 * {@link JobState#FAILED}, or {@link JobState#CANCELED}; and it has been more
	 * than {@link Settings#getNotificationAfter()} days since the job finished.
	 * <p>
	 * All processed jobs are marked for retirement in
	 * {@link Settings#getRetireAfter()} days from the current time. Only jobs with
	 * state {@link JobState#SUCCESS} lead to an email notification.
	 * <p>
	 * Day values are converted to millis and computed from the current time (so
	 * there is no rounding to the beginning of the day or anything like that).
	 *
	 * @return Number of emails sent.
	 */
	public int sendNotifications() {
		Database database = application.getDatabase();
		Settings settings = application.getSettings();
		JobDao dao = new JobDao(database);

		int daysToRetirement = settings.getRetireAfter() - settings.getNotificationAfter();

		long notificationMillis = settings.getNotificationAfter() * 24L * 60L * 60L * 1000L;
		long notificationCutoff = System.currentTimeMillis() - notificationMillis;

		int notifiedCount = 0;
		int failureCount = 0;
		int otherCount = 0;

		List<AbstractJob> successfulJobs = dao.findAllOlderThan(notificationCutoff, JobState.SUCCESS);
		for (AbstractJob job : successfulJobs) {
			NotifyResult result = notify(settings, dao, job, daysToRetirement);

			if (result == NotifyResult.SUCCESSFUL_JOB_NOTIFIED) {
				notifiedCount++;
				log.info(getResultMessage(job, result));
			} else {
				assert result == NotifyResult.SUCCESSFUL_JOB_ERROR;
				failureCount++;
				log.error(getResultMessage(job, result));
			}
		}

		List<AbstractJob> failedJobs = dao.findAllOlderThan(notificationCutoff, JobState.FAILED);
		for (AbstractJob job : failedJobs) {
			NotifyResult result = notify(settings, dao, job, daysToRetirement);
			assert result == NotifyResult.FAILED_JOB_SKIPPED;
			log.info(getResultMessage(job, result));
		}
		otherCount += failedJobs.size();

		List<AbstractJob> canceledJobs = dao.findAllOlderThan(notificationCutoff, JobState.CANCELED);
		for (AbstractJob job : canceledJobs) {
			NotifyResult result = notify(settings, dao, job, daysToRetirement);
			assert result == NotifyResult.FAILED_JOB_SKIPPED;
			log.info(getResultMessage(job, result));
		}
		otherCount += canceledJobs.size();

		log.info("{} notifications sent. {} failures. {} jobs marked without email notification.",
				notifiedCount, failureCount, otherCount);

		return notifiedCount;
	}
}
