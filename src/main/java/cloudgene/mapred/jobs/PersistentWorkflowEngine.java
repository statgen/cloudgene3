package cloudgene.mapred.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.*;
import cloudgene.mapred.jobs.engine.handler.IJobErrorHandler;
import cloudgene.mapred.jobs.state.JobState;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.Database;

public class PersistentWorkflowEngine extends WorkflowEngine {

	private static final Logger log = LoggerFactory.getLogger(PersistentWorkflowEngine.class);

	private final Database database;
	private final JobDao jobDao;
	private final CounterDao counterDao;
	private final Map<String, Long> counters;
	private final List<IJobErrorHandler> handlers = new ArrayList<>();

	public PersistentWorkflowEngine(Database database, int ltqThreads) {
		super(ltqThreads);
		this.database = database;

		log.info("Init Counters....");

		counterDao = new CounterDao(database);
		counters = counterDao.getAll();

		jobDao = new JobDao(database);

		List<AbstractJob> deadJobs = jobDao.findAllByState(JobState.WAITING);
		deadJobs.addAll(jobDao.findAllByState(JobState.RUNNING));
		deadJobs.addAll(jobDao.findAllByState(JobState.EXPORTING));

		for (AbstractJob job : deadJobs) {
			log.info("lost control over job {} -> Dead", job.getId());
			job.setState(JobState.DEAD);
			jobDao.update(job);
		}
	}

	@Override
	protected void statusUpdated(@NonNull AbstractJob job) {
		super.statusUpdated(job);
		jobDao.update(job);
	}

	@Override
	protected void jobCompleted(@NonNull AbstractJob job) {
		super.jobCompleted(job);

		DownloadDao downloadDao = new DownloadDao(database);

		for (CloudgeneParameterOutput parameter : job.getOutputParams()) {
			if (parameter.isDownload()) {
				if (parameter.getFiles() != null) {
					for (Download download : parameter.getFiles()) {
						download.setParameter(parameter);
						downloadDao.insert(download);
					}
				}
			}
		}

		if (job.getLogOutput().getFiles() != null) {
			for (Download download : job.getLogOutput().getFiles()) {
				download.setParameter(job.getLogOutput());
				downloadDao.insert(download);
			}
		}

		if (job.getSteps() != null) {
			StepDao stepDao = new StepDao(database);
			for (Step step : job.getSteps()) {
				stepDao.insert(step);
				MessageDao messageDao = new MessageDao(database);
				if (step.getLogMessages() != null) {
					for (Message logMessage : step.getLogMessages()) {
						messageDao.insert(logMessage);
					}
				}
			}
		}

		// count all runs when counter was not set by application
		Map<String, Long> submittedCounters = job.getContext().getSubmittedCounters();
		if (!submittedCounters.containsKey("runs")) {
			if (job.getState() == JobState.SUCCESS) {
				submittedCounters.put("runs", 1L);
			}
		}

		// write all submitted counters into database
		for (String name : submittedCounters.keySet()) {
			Long value = submittedCounters.get(name);

			if (value != null) {
				Long counterValue = counters.get(name);

				if (counterValue == null) {
					counterValue = value;
				} else {
					counterValue = counterValue + value;
				}

				counters.put(name, counterValue);
				counterDao.insert(name, value, job);
			}
		}

		// write all submitted values into database
		JobValueDao jobValueDao = new JobValueDao(database);
		Map<String, String> submittedValues = job.getContext().getSubmittedValues();

		for (String name : submittedValues.keySet()) {
			String value = submittedValues.get(name);

			if (value != null) {
				jobValueDao.insert(name, value, job);
			}
		}

		// update job updates (state, endtime, ....)
		jobDao.update(job);

		if (job.getState() == JobState.FAILED) {
			for (IJobErrorHandler handler : handlers) {
				handler.handle(this, job);
			}
		}
	}

	@Override
	protected void jobSubmitted(@NonNull AbstractJob job) {
		super.jobSubmitted(job);
		jobDao.insert(job);

		ParameterDao parameterDao = new ParameterDao(database);

		for (CloudgeneParameterInput parameter : job.getInputParams()) {
			parameter.setJobId(job.getId());
			parameterDao.insert(parameter);
		}

		for (CloudgeneParameterOutput parameter : job.getOutputParams()) {
			parameter.setJobId(job.getId());
			parameterDao.insert(parameter);
		}

		parameterDao.insert(job.getLogOutput());
	}

	@Override
	@NonNull
	public Map<String, Long> getCounters(JobState state, @Nullable List<String> names) {
		if (state == JobState.SUCCESS) {
			List<String> keys = (names == null) ? counters.keySet().stream().toList() : names;
			Map<String, Long> filtered = new HashMap<>();

			for (String name : keys) {
				filtered.put(name, this.counters.get(name));
			}

			return filtered;
		} else {
			return super.getCounters(state, null);
		}
	}

	public Map<String, CounterDao.Stats> getCountersByUser(User user) {
		return counterDao.getByUser(user);
	}

	public void addJobErrorHandler(IJobErrorHandler handler) {
		this.handlers.add(handler);
	}
}
