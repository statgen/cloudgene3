package cloudgene.mapred.server.controller;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.CounterDao;
import cloudgene.mapred.database.dao.CounterHistoryDao;
import cloudgene.mapred.jobs.JobValue;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.auth.AuthenticationService;
import cloudgene.mapred.server.responses.*;
import cloudgene.mapred.server.services.ServerService;
import cloudgene.mapred.util.TextUtil;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api/v2/admin/server")
@Secured(User.ROLE_ADMIN)
public class ServerAdminController {

	private static final Logger log = LoggerFactory.getLogger(ServerAdminController.class);

	private static final String LOG_FILENAME = "logs/cloudgene.log";

	@Inject
	protected Application application;

	@Inject
	protected AuthenticationService authenticationService;

	@Inject
	protected ServerService serverService;

	@Get("/queue/block")
	@Produces(MediaType.TEXT_PLAIN)
	public String blockQueue() {
		application.getWorkflowEngine().block();
		return "Queue blocked.";
	}

	@Get("/queue/open")
	@Produces(MediaType.TEXT_PLAIN)
	public String openQueue() {
		application.getWorkflowEngine().resume();
		return "Queue opened.";
	}

	@Get("/maintenance/enter")
	@Produces(MediaType.TEXT_PLAIN)
	public String enterMaintenance() {
		application.getSettings().setMaintenance(true);
		application.getSettings().save();
		return "Enter Maintenance mode.";
	}

	@Get("/maintenance/exit")
	@Produces(MediaType.TEXT_PLAIN)
	public String exitMaintenance() {
		application.getSettings().setMaintenance(false);
		application.getSettings().save();
		return "Exit Maintenance mode.";
	}

	@Get("/cluster")
	public ClusterDetailsResponse getDetails() {
		return serverService.getClusterDetails();
	}

	@Get("/logs/cloudgene.log")
	public String getLogs() {
		File file = new File(LOG_FILENAME);
		try {
			return TextUtil.tail(file, 1_000);
		} catch (IOException e) {
			log.error("Failed to load log file: " + LOG_FILENAME, e);
			return "No log file available.";
		}
	}

	@Get("/settings")
	public ServerSettingsResponse getSettings() {
		return ServerSettingsResponse.build(application.getSettings());
	}

	@Post("/settings/update")
	public ServerSettingsResponse updateSettings(
			String name,
			String adminName,
			String adminMail,
			String serverUrl,
			String baseUrl,
			@Nullable String googleAnalytics,
			boolean mail,
			String mailSmtp,
			String mailUser,
			String mailPassword,
			String mailPort,
			String mailName,
			String workspaceType,
			String workspaceLocation) {

		serverService.updateSettings(name, adminName, adminMail, serverUrl, baseUrl, googleAnalytics, mail, mailSmtp,
				mailPort, mailUser, mailPassword, mailName, workspaceType, workspaceLocation);

		return ServerSettingsResponse.build(application.getSettings());
	}

	@Get("/nextflow/config")
	public NextflowConfigResponse getNextflowConfig() {
		return NextflowConfigResponse.build(application.getSettings());
	}

	@Post("/nextflow/config/update")
	public NextflowConfigResponse updateNextflowConfig(String config, String env) {
		serverService.updateNextflowConfig(config);
		serverService.updateNextflowEnv(env);

		return NextflowConfigResponse.build(application.getSettings());
	}

	@Get("/statistics")
	public List<StatisticsResponse.Entry> getStatistics(@Nullable @QueryValue("days") Integer days) {
		if (days == null) {
			days = 1;
		}

		CounterHistoryDao dao = new CounterHistoryDao(application.getDatabase());
		Instant end = Instant.now();
		Instant start = end.minus(days, ChronoUnit.DAYS);

		List<CounterHistoryDao.Entry> stats = dao.getAllBetween(start, end);
		return StatisticsResponse.build(stats);
	}

	@Get("/counters")
	public CounterStatisticsResponse getCounters() {
		Map<String, Map<String, CounterDao.Stats>> counters = serverService.getCounterStatistics();
		return CounterStatisticsResponse.build(counters);
	}

	@Get("/values")
	public HttpResponse<Map<String, List<JobValueResponse>>> getValues() {
		Map<String, List<JobValue>> values = serverService.getValues();
		Map<String, List<JobValueResponse>> response = JobValueResponse.build(values);
		return HttpResponse.ok(response);
	}
}
