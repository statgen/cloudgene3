package cloudgene.mapred.server.services;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.database.ParameterDao;
import cloudgene.mapred.jobs.*;
import cloudgene.mapred.jobs.state.JobState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.DownloadDao;
import cloudgene.mapred.database.JobDao;
import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.jobs.workspace.WorkspaceFactory;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.exceptions.JsonHttpStatusException;
import cloudgene.mapred.util.FormUtil.Parameter;
import cloudgene.mapred.util.Page;
import cloudgene.mapred.util.config.Settings;
import cloudgene.mapred.wdl.WdlApp;
import genepi.io.FileUtil;
import io.micronaut.http.HttpStatus;
import jakarta.inject.Singleton;

@Singleton
public class JobService {

	private static final Logger log = LoggerFactory.getLogger(JobService.class);

	private static final SimpleDateFormat ID_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");

	protected Application application;
	protected WorkspaceFactory workspaceFactory;

	public JobService(Application application, WorkspaceFactory workspaceFactory) {
		this.application = application;
		this.workspaceFactory = workspaceFactory;
	}

	/**
	 * Attempts to retrieve the requested job from:
	 * <ol>
	 *     <li>The current workflow engine (in-memory data).</li>
	 *     <li>The database.</li>
	 * </ol>
	 *
	 * If the job cannot be found, a {@link JsonHttpStatusException} is thrown.
	 */
	public AbstractJob getById(String id) {
		// TODO: better to go via database? only load from engine when running?

		AbstractJob job = application.getWorkflowEngine().getJobById(id);

		if (job == null) {
			// finished job is in database
			JobDao dao = new JobDao(application.getDatabase());
			job = dao.findById(id, true);
		} else {
			if (job instanceof CloudgeneJob) {
				((CloudgeneJob) job).updateProgress();
			}
		}

		if (job == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, "Job " + id + " not found.");
		}

		return job;
	}

	/**
	 * Similar to {@link #getById(String)}, but also throws a
	 * {@link JsonHttpStatusException} if {@code user} is not authorized to view
	 * this job.
	 */
	public AbstractJob getByIdAndUser(String id, User user) {
		if (user == null) {
			throw new JsonHttpStatusException(HttpStatus.UNAUTHORIZED, "Access denied.");
		}

		AbstractJob job = getById(id);

		// admin has access to all jobs. Other users only to their own jobs.
		if (!user.isAdmin() && job.getUser().getId() != user.getId()) {
			throw new JsonHttpStatusException(HttpStatus.FORBIDDEN, "Access denied.");
		}

		return job;
	}

	/**
	 * Submits the requested job for running, if applicable.
	 * <p>
	 * {@code appId} must be a valid application identifier for an installed app.
	 * Requesting an unknown ID raises an exception.
	 * <p>
	 * Non-admin users are limited to {@code maxRunningJobsPerUser} simultaneous
	 * queued and/or running jobs. Attempting to submit beyond this limit raises an
	 * exception.
	 *
	 * @param appId     String of form {@code <name>[@version]} identifying which
	 *                  installed Cloudgene application to run.
	 * @param form      Input parameters for the job.
	 * @param user      Who is submitting this job?
	 * @param userAgent Web user agent used to perform this request (stored with job
	 *                  data).
	 * @return The submitted job.
	 */
	public AbstractJob submitJob(String appId, List<Parameter> form, User user, String userAgent) {
		if (user == null) {
			throw new JsonHttpStatusException(HttpStatus.UNAUTHORIZED, "Access denied.");
		}

		WorkflowEngine engine = this.application.getWorkflowEngine();
		Settings settings = this.application.getSettings();

		int maxPerUser = settings.getMaxRunningJobsPerUser();
		if (!user.isAdmin() && engine.getJobsByUser(user).size() >= maxPerUser) {
			throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST,
					"Only " + maxPerUser + " jobs per user can be executed simultaneously.");
		}

		ApplicationRepository repository = settings.getApplicationRepository();
		cloudgene.mapred.apps.Application application = repository.getByIdAndUser(appId, user);
		if (application == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, "Application '" + appId + "' not found.");
		}
		WdlApp app = application.getWdlApp();
		if (app.getWorkflow() == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND,
					"Application '" + appId + "' has no workflow section.");
		}

		String id = createId();

		Map<String, String> inputParams;

		IWorkspace workspace = workspaceFactory.getDefault();

		try {
			// setup workspace
			workspace.setJob(id);
			workspace.setup();

			// parse input params
			inputParams = JobParameterParser.parse(form, app, workspace);

		} catch (Exception e) {
			throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}

		String name = id;
		String jobName = inputParams.get("job-name");
		if (jobName != null && !jobName.isBlank()) {
			name = jobName;
		}

		// TODO: remove and solve via workspace!
		String localWorkspace = FileUtil.path(settings.getLocalWorkspace(), id);
		FileUtil.createDirectory(localWorkspace);

		CloudgeneJob job = new CloudgeneJob(user, id, app, inputParams);
		job.setId(id);
		job.setName(name);
		job.setLocalWorkspace(localWorkspace);
		job.setWorkspace(workspace);
		job.setSettings(settings);
		job.setApplication(app.getName() + " " + app.getVersion());
		job.setApplicationId(appId);
		job.setUserAgent(userAgent);

		engine.submit(job);

		return job;
	}

	public Page<AbstractJob> getAllByUserAndPage(User user, Integer page, int pageSize) {
		int offset = 0;
		if (page != null) {

			offset = page;
			if (offset < 1) {
				offset = 1;
			}
			offset = (offset - 1) * pageSize;
		}

		// find all jobs by user
		JobDao dao = new JobDao(application.getDatabase());

		// count all jobs
		int count = dao.countAllByUser(user);

		List<AbstractJob> jobs;
		if (page != null) {
			jobs = dao.findAllByUser(user, offset, pageSize);
		} else {
			jobs = dao.findAllByUser(user);
			page = 1;
			pageSize = count;

		}

		// if job is running, use in memory instance
		List<AbstractJob> finalJobs = new ArrayList<>();
		for (AbstractJob job : jobs) {
			AbstractJob runningJob = application.getWorkflowEngine().getJobById(job.getId());
			if (runningJob != null) {
				finalJobs.add(runningJob);
			} else {
				finalJobs.add(job);
			}
		}

		Page<AbstractJob> result = new Page<>();
		result.setCount(count);
		result.setPage(page);
		result.setPageSize(pageSize);
		result.setData(finalJobs);

		return result;
	}

	public AbstractJob delete(AbstractJob job) {
		Settings settings = application.getSettings();

		// delete local directory
		String localOutput = FileUtil.path(settings.getLocalWorkspace(), job.getId());
		FileUtil.deleteDirectory(localOutput);

		// delete job from database
		job.setState(JobState.STATE_DELETED);

		JobDao dao = new JobDao(application.getDatabase());
		dao.update(job);

		// When a user manually deletes a job, clear sensitive data immediately
		ParameterDao parameterDao = new ParameterDao(application.getDatabase());
		parameterDao.deleteSensitiveByJob(job);

		// delete all results that are stored on external workspaces

		IWorkspace workspace = workspaceFactory.getByJob(job);
		try {
			workspace.delete(job.getId());
		} catch (Exception e) {
			log.error("Deleting {} form workspace failed.", job.getId(), e);
		}

		return job;
	}

	/**
	 * If the {@code job} is in the long time queue (WAITING or RUNNING, basically),
	 * it is canceled. This is reflected by a change in its {@code state}.
	 * Otherwise, nothing is done. Returns the passed {@code job}.
	 */
	public AbstractJob cancel(AbstractJob job) {
		application.getWorkflowEngine().cancel(job);
		return job;
	}

	public AbstractJob restart(AbstractJob job) {
		Settings settings = application.getSettings();

		if (job.getState() != JobState.STATE_DEAD) {
			throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST, "Job " + job.getId() + " is not pending.");
		}

		String localWorkspace = FileUtil.path(settings.getLocalWorkspace(), job.getId());

		job.setLocalWorkspace(localWorkspace);
		job.setSettings(settings);

		String appId = job.getApplicationId();

		ApplicationRepository repository = settings.getApplicationRepository();
		cloudgene.mapred.apps.Application application = repository.getByIdAndUser(appId, job.getUser());
		if (application == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, "Application '" + appId + "' not found.");

		}

		IWorkspace workspace = workspaceFactory.getDefault();

		try {
			// setup workspace
			workspace.setJob(job.getId());
			workspace.setup();
		} catch (Exception e) {
			throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
		job.setWorkspace(workspace);

		((CloudgeneJob) job).loadApp(application.getWdlApp());

		this.application.getWorkflowEngine().restart(job);

		return job;
	}

	public int reset(AbstractJob job, int maxDownloads) {
		DownloadDao downloadDao = new DownloadDao(application.getDatabase());
		int count = 0;

		for (CloudgeneParameterOutput param : job.getOutputParams()) {
			if (param.isDownload()) {
				List<Download> downloads = param.getFiles();

				for (Download download : downloads) {
					download.setCount(maxDownloads);
					downloadDao.update(download);
					count++;
				}

			}
		}

		return count;
	}

	public AbstractJob changePriority(AbstractJob job, long priority) {
		application.getWorkflowEngine().updatePriority(job, priority);
		return job;
	}

	public String archive(AbstractJob job) {
		Settings settings = application.getSettings();

		JobDao dao = new JobDao(application.getDatabase());

		if (job.getState() != JobState.STATE_SUCCESS
				&& job.getState() != JobState.STATE_FAILED
				&& job.getState() != JobState.STATE_CANCELED) {
			return "Job " + job.getId() + " has wrong state for this operation.";
		}

		try {
			// delete local directory and hdfs directory
			String localOutput = FileUtil.path(settings.getLocalWorkspace(), job.getId());
			FileUtil.deleteDirectory(localOutput);

			job.setState(JobState.STATE_RETIRED);
			dao.update(job);

			// When an admin manually deletes a job, clear sensitive data immediately
			ParameterDao parameterDao = new ParameterDao(application.getDatabase());
			parameterDao.deleteSensitiveByJob(job);

			IWorkspace workspace = workspaceFactory.getByJob(job);

			try {
				workspace.delete(job.getId());
			} catch (Exception e) {
				log.error("Deleting {} from workspace failed.", job.getId(), e);
			}

			return "Retired job " + job.getId();
		} catch (Exception e) {
			return "Retire " + job.getId() + " failed.";
		}
	}

	public String increaseRetireDate(AbstractJob job, int days) {
		JobDao dao = new JobDao(application.getDatabase());

		if (job.getState() == JobState.STATE_SUCCESS_AND_NOTIFICATION_SEND
				|| job.getState() == JobState.STATE_FAILED_AND_NOTIFICATION_SEND) {

			try {

				job.setDeletedOn(job.getDeletedOn() + (days * 24L * 60L * 60L * 1000L));

				dao.update(job);

				return "Update delete on date for job " + job.getId() + ".";

			} catch (Exception e) {

				return "Update delete date for job " + job.getId() + " failed.";
			}

		} else {
			return "Job " + job.getId() + " has wrong state for this operation.";
		}
	}

	/**
	 * Returns a hopefully unique ID based on a timestamp, with form
	 * {@code job-yyyyMMdd-HHmmss-SSS}.
	 */
	public String createId() {
		return "job-" + ID_DATE_FORMAT.format(new Date());
	}

	/**
	 * Returns all available jobs from the given {@code state} (not to be confused
	 * with {@link JobState}), where the options are:
	 * <ul>
	 *     <li>
	 *         {@code running-ltq}: whatever the long time queue is (actual
	 *         waiting and running jobs?)
	 *     </li>
	 *     <li>
	 *         {@code running-stq}: legacy value. Doesn't return anything.
	 *     </li>
	 *     <li>
	 *         {@code current}: whatever current is (finished jobs with data
	 *         available?)
	 *     </li>
	 *     <li>
	 *         {@code retired}: whatever retired is (old jobs that have been
	 *         purged?)
	 *     </li>
	 * </ul>
	 */
	public List<AbstractJob> getJobs(String state) {
		List<AbstractJob> jobs = new ArrayList<>();

		WorkflowEngine engine = application.getWorkflowEngine();
		JobDao dao = new JobDao(application.getDatabase());

		if (state != null) {
			switch (state) {
				case "running-ltq":
					jobs = engine.getAllJobsInLongTimeQueue();
					break;

				case "running-stq":
					// TODO: remove!
					jobs = new ArrayList<>();
					break;

				case "current":
					jobs = dao.findAllNotRetiredJobs();
					List<AbstractJob> toRemove = new ArrayList<>();
					for (AbstractJob job : jobs) {
						if (engine.isInQueue(job)) {
							toRemove.add(job);
						}
					}
					jobs.removeAll(toRemove);
					break;

				case "retired":
					jobs = dao.findAllByState(JobState.STATE_RETIRED);
					break;
			}
		}

		return jobs;
	}

	public String getJobLog(AbstractJob job, String name) throws IOException {
		if (job.isRunning()) {
			// files are locally when job is running
			return job.getLog(name);
		} else {
			IWorkspace workspace = workspaceFactory.getByJob(job);
			return workspace.downloadLog(name);
		}
	}
}
