package cloudgene.mapred.server.services;

import cloudgene.mapred.BuildInfo;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.Template;
import cloudgene.mapred.core.User;
import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.plugins.nextflow.NextflowPlugin;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.responses.ServerResponse;
import cloudgene.mapred.util.config.Settings;
import cloudgene.mapred.util.command.Command;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import genepi.io.FileUtil;
import io.micronaut.security.oauth2.configuration.OauthClientConfigurationProperties;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.util.*;

import org.apache.commons.dbcp.BasicDataSource;

@Singleton
public class ServerService {

	public static final String IMAGE_DATA = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"96\" height=\"20\">"
			+ "  <linearGradient id=\"b\" x2=\"0\" y2=\"100%\"><stop offset=\"0\" stop-color=\"#bbb\" stop-opacity=\".1\"/><stop offset=\"1\" stop-opacity=\".1\"/></linearGradient>"
			+ "  <mask id=\"a\"><rect width=\"96\" height=\"20\" rx=\"3\" fill=\"#fff\"/></mask>"
			+ "  <g mask=\"url(#a)\"><path fill=\"#555\" d=\"M0 0h55v20H0z\"/><path fill=\"#97CA00\" d=\"M55 0h41v20H55z\"/><path fill=\"url(#b)\" d=\"M0 0h96v20H0z\"/></g>"
			+ "  <g fill=\"#fff\" text-anchor=\"middle\" font-family=\"DejaVu Sans,Verdana,Geneva,sans-serif\" font-size=\"11\">"
			+ "    <text x=\"27.5\" y=\"15\" fill=\"#010101\" fill-opacity=\".3\">version</text>"
			+ "    <text x=\"27.5\" y=\"14\">version</text>"
			+ "    <text x=\"74.5\" y=\"15\" fill=\"#010101\" fill-opacity=\".3\">" + BuildInfo.VERSION + "</text>"
			+ "    <text x=\"74.5\" y=\"14\">" + BuildInfo.VERSION + "</text>"
			+ "  </g>"
			+ "</svg>";

	@Inject
	protected Application application;

	@Inject
	protected List<OauthClientConfigurationProperties> clients;

	public ServerResponse getRoot(User user) {
		String name = application.getSettings().getName();
		String background = application.getSettings().getColors().get("background");
		String foreground = application.getSettings().getColors().get("foreground");
		boolean emailRequired = application.getSettings().isEmailRequired();

		String userEmailDescription = application.getTemplate(Template.USER_EMAIL_DESCRIPTION);
		String userWithoutEmailDescription = application.getTemplate(Template.USER_WITHOUT_EMAIL_DESCRIPTION);

		List<String> oauth = clients.stream()
				.map(OauthClientConfigurationProperties::getName)
				.toList();

		ServerResponse.User responseUser = null;
		List<ServerResponse.App> apps = new ArrayList<>();
		List<ServerResponse.App> deprecatedApps = null;
		List<ServerResponse.App> experimentalApps = null;

		if (user != null) {
			responseUser = new ServerResponse.User(
					user.getUsername(),
					user.getFullName(),
					user.getMail(),
					user.isAdmin());

			ApplicationRepository appRepo = application.getSettings().getApplicationRepository();
			List<cloudgene.mapred.apps.Application> rawApps = appRepo.getAllByUser(user, ApplicationRepository.APPS);

			deprecatedApps = new ArrayList<>();
			experimentalApps = new ArrayList<>();

			for (cloudgene.mapred.apps.Application app : rawApps) {
				ServerResponse.App processed = new ServerResponse.App(
						app.getId(),
						app.getWdlApp().getName(),
						app.getWdlApp().getVersion());

				switch (app.getWdlApp().getRelease()) {
					case "deprecated" -> deprecatedApps.add(processed);
					case "experimental" -> experimentalApps.add(processed);
					case null, default -> apps.add(processed);
				}
			}
		}

		boolean loggedIn = (user != null);

		boolean maintenance;
		String maintenanceMessage;

		if (application.getSettings().isMaintenance()) {
			maintenance = true;
			maintenanceMessage = application.getTemplate(Template.MAINTENANCE_MESSAGE);
		} else {
			maintenance = false;
			maintenanceMessage = null;
		}

		return new ServerResponse(
				name,
				background,
				foreground,
				emailRequired,
				userEmailDescription,
				userWithoutEmailDescription,
				oauth,
				responseUser,
				apps,
				deprecatedApps,
				experimentalApps,
				loggedIn,
				maintenance,
				maintenanceMessage);
	}

	public void updateSettings(
			String name,
			String adminName,
			String adminMail,
			String serverUrl,
			String baseUrl,
			String backgroundColor,
			String foregroundColor,
			String googleAnalytics,
			String mail,
			String mailSmtp,
			String mailPort,
			String mailUser,
			String mailPassword,
			String mailName,
			String workspaceType,
			String workspaceLocation) {

		Settings settings = application.getSettings();
		settings.setName(name);
		settings.setAdminName(adminName);
		settings.setAdminMail(adminMail);
		settings.setServerUrl(serverUrl);
		settings.setBaseUrl(baseUrl);
		settings.getColors().put("background", backgroundColor);
		settings.getColors().put("foreground", foregroundColor);
		settings.setGoogleAnalytics(googleAnalytics);
		settings.getExternalWorkspace().put("type", workspaceType);
		settings.getExternalWorkspace().put("location", workspaceLocation);

		if (mail != null && mail.equals("true")) {
			Map<String, String> mailConfig = new HashMap<>();
			mailConfig.put("smtp", mailSmtp);
			mailConfig.put("port", mailPort);
			mailConfig.put("user", mailUser);
			mailConfig.put("password", mailPassword);
			mailConfig.put("name", mailName);
			application.getSettings().setMail(mailConfig);
		} else {
			application.getSettings().setMail(null);
		}

		application.getSettings().save();
	}

	public String getClusterDetails() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode object = mapper.createObjectNode();

		// general settings
		object.put("maintenance", application.getSettings().isMaintenance());
		object.put("blocked", !application.getWorkflowEngine().isRunning());
		object.put("version", BuildInfo.VERSION);
		object.put("maintenance", application.getSettings().isMaintenance());
		object.put("blocked", !application.getWorkflowEngine().isRunning());
		object.put("threads", application.getSettings().getThreadsQueue());
		object.put("max_jobs_user", application.getSettings().getMaxRunningJobsPerUser());
		object.put("built_by", BuildInfo.BUILT_BY);
		object.put("built_time", BuildInfo.BUILD_TIME);

		// workspace and hdd
		File workspace = new File(application.getSettings().getLocalWorkspace());

		String workspacePath = workspace.getAbsolutePath();
		long freeDiskSpace = workspace.getUsableSpace() / (1024L * 1024L * 1024L);
		long totalDiskSpace = workspace.getTotalSpace() / (1024L * 1024L * 1024L);
		long usedDiskSpace = totalDiskSpace - freeDiskSpace;

		object.put("workspace_path", workspacePath);
		object.put("free_disc_space", freeDiskSpace);
		object.put("total_disc_space", totalDiskSpace);
		object.put("used_disc_space", usedDiskSpace);

		// plugins
		PluginManager manager = PluginManager.getInstance();

		ArrayNode plugins = object.putArray("plugins");

		for (IPlugin plugin : manager.getPlugins()) {
			ObjectNode pluginObject = mapper.createObjectNode();
			pluginObject.put("name", plugin.getName());

			if (plugin.isInstalled()) {
				pluginObject.put("enabled", true);
				pluginObject.put("details", plugin.getDetails());
			} else {
				pluginObject.put("enabled", false);
				pluginObject.put("error", plugin.getStatus());
			}
			plugins.add(pluginObject);
		}

		// check user defined resources
		for (Map<String, String> resource : application.getSettings().getResources()) {
			String name = resource.get("name");
			ObjectNode pluginObject = mapper.createObjectNode();
			pluginObject.put("name", name);
			if (!resource.containsKey("command")) {
				pluginObject.put("error", "Command defined in resource '" + name + "'");
			}
			String cmd = resource.get("command");
			String[] tiles = cmd.split(" ");
			String[] params = Arrays.copyOfRange(tiles, 1, tiles.length);
			Command command = new Command(tiles[0], params);
			command.setSilent(true);
			StringBuffer output = new StringBuffer();
			StringBuffer error = new StringBuffer();
			command.writeStdout(output);
			command.writeStderr(error);
			int exitCode = command.execute();
			if (exitCode == 0) {
				pluginObject.put("enabled", true);
				pluginObject.put("details", output.toString());
			} else {
				pluginObject.put("enabled", false);
				pluginObject.put("error", output + "\n" + error);
			}
			plugins.add(pluginObject);
		}

		// database
		BasicDataSource dbSrc = application.getDatabase().getDataSource();
		object.put("db_max_active", dbSrc.getMaxActive());
		object.put("db_active", dbSrc.getNumActive());
		object.put("db_max_idle", dbSrc.getMaxIdle());
		object.put("db_idle", dbSrc.getNumIdle());
		object.put("db_max_open_prep_statements", dbSrc.getMaxOpenPreparedStatements());

		return object.toString();
	}

	public void updateNextflowConfig(String content) {
		NextflowPlugin plugin = (NextflowPlugin) PluginManager.getInstance().getPlugin(NextflowPlugin.ID);
		String filename = plugin.getNextflowConfig();
		FileUtil.writeStringBufferToFile(filename, new StringBuffer(content));
	}

	public void updateNextflowEnv(String content) {
		NextflowPlugin plugin = (NextflowPlugin) PluginManager.getInstance().getPlugin(NextflowPlugin.ID);
		String filename = plugin.getNextflowEnv();
		FileUtil.writeStringBufferToFile(filename, new StringBuffer(content));
	}
}
