package cloudgene.mapred.server.tasks;

import java.util.List;
import java.util.Map;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.CounterHistoryDao;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.WorkflowEngine;
import cloudgene.mapred.jobs.state.JobState;
import cloudgene.mapred.server.Application;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ServerTasks {

	@Inject
	protected Application application;

	@Scheduled(fixedDelay = "5m")
	public void writeStatistics() {
		if (!application.getSettings().isWriteStatistics()) {
			return;
		}

		WorkflowEngine engine = application.getWorkflowEngine();
		Database database = application.getDatabase();

		List<AbstractJob> jobs = engine.getAllJobsInLongTimeQueue();
		long countWaiting = 0;
		long countRunning = 0;

		for (AbstractJob job : jobs) {
			if (job.getState() == JobState.RUNNING) {
				countRunning++;
			}
			if (job.getState() == JobState.WAITING) {
				countWaiting++;
			}
		}

		Map<String, Long> countersRunning = engine
				.getCounters(JobState.RUNNING, null);
		Map<String, Long> countersWaiting = engine
				.getCounters(JobState.WAITING, null);
		Map<String, Long> countersComplete = engine
				.getCounters(JobState.SUCCESS, null);

		UserDao daoUser = new UserDao(database);
		List<User> users = daoUser.findAll();

		long timestamp = System.currentTimeMillis();

		CounterHistoryDao daoHistory = new CounterHistoryDao(database);
		daoHistory.insert(timestamp, "users", users.size());
		daoHistory.insert(timestamp, "runningJobs", countRunning);
		daoHistory.insert(timestamp, "waitingJobs", countWaiting);
		daoHistory.insert(timestamp, "runningChromosomes",
				(countersRunning.get("chromosomes") == null ? 0
						: countersRunning.get("chromosomes")));
		daoHistory.insert(timestamp, "waitingChromosomes",
				(countersWaiting.get("chromosomes") == null ? 0
						: countersWaiting.get("chromosomes")));
		daoHistory.insert(timestamp, "completeChromosomes",
				(countersComplete.get("chromosomes") == null ? 0
						: countersComplete.get("chromosomes")));
		daoHistory.insert(timestamp, "completeJobs", (countersComplete
				.get("runs") == null ? 0 : countersComplete.get("runs")));

	}
}
