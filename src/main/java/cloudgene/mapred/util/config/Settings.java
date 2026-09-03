package cloudgene.mapred.util.config;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.jobs.Environment;
import genepi.io.FileUtil;

public class Settings {

	private static final Logger log = LoggerFactory.getLogger(Settings.class);

	public static final String DEFAULT_SECURITY_KEY = "default-key-change-me-immediately";

	/** Authority (domain + optional port) for this website's URL. */
	private String serverUrl = "localhost:8082";
	/** Path (slash-separated sequence of path components) for this website's URL. */
	private String baseUrl = "";
	private String tempPath = "tmp";
	private String localWorkspace = "workspace";
	private String name = "Cloudgene";
	private String secretKey = "";
	private Map<String, String> mail;
	private Map<String, String> database;
	private Map<String, Map<String, String>> plugins;
	private List<Map<String, String>> errorHandlers = new ArrayList<>();
	private int autoRetireInterval = 5;
	private int retireAfter = 6;
	private int notificationAfter = 4;
	private int threadsQueue = 5;
	private int maxRunningJobsPerUser = 2;
	private boolean autoRetire = false;
	private boolean writeStatistics = true;
	private boolean maintenance = false;
	private boolean emailRequired = true;
	private String adminMail = null;
	private String adminName = null;
	private boolean showLogs = false;
	private Map<String, String> externalWorkspace = null;
	private int uploadLimit = 5000;
	private String googleAnalytics = "";
	private int maxDownloads = 10;
	private String port = "8082";
	private boolean workspaceCleanup = true;
	private List<String> counters = new ArrayList<>();
	private ApplicationRepository repository;

	// fake!
	private List<Application> apps = new ArrayList<>();

	public Settings() {
		repository = new ApplicationRepository();
		repository.setAppsFolder(Configuration.getAppsDirectory());

		// read default settings from env variables when set
		this.name = Configuration.get("CG_SERVICE_NAME", this.name);

		database = new HashMap<>();
		initDefaultDatabase(database, "data/cloudgene");
	}

	public static Settings load() throws IOException {
		String filename = Configuration.getSettingsFilename();

		if (!new File(filename).exists()) {
			log.info("Loading default settings. File '{}' not found.", filename);
			return new Settings();
		}

		log.info("Loading settings from {}...", filename);

		YamlConfig yamlConfig = new YamlConfig();
		yamlConfig.setPropertyElementType(Settings.class, "apps", Application.class);
		yamlConfig.setClassTag("cloudgene.mapred.util.Application", Application.class);

		YamlReader reader = new YamlReader(new FileReader(filename), yamlConfig);
		Settings settings = reader.read(Settings.class);
		reader.close();
		log.info("Settings loaded.");

		log.info("Auto retire: {}", settings.isAutoRetire());
		log.info("Retire jobs after {} days.", settings.retireAfter);
		log.info("Notify user after {} days.", settings.notificationAfter);
		log.info("Write statistics: {}", settings.writeStatistics);

		return settings;
	}

	public List<Application> getApps() {
		return repository.getAll();
	}

	public void setApps(List<Application> apps) {
		this.apps = apps;
		repository.setApps(apps);
	}

	public static void initDefaultDatabase(Map<String, String> database, String defaultSchema) {
		database.put("driver", "h2");
		database.put("database", defaultSchema);
		database.put("user", "cloudgene");
		database.put("password", "cloudgene");
	}

	public void save() {
		String filename = Configuration.getSettingsFilename();
		try {

			File file = new File(filename);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
			}

			log.info("Storing settings to file {} ({} apps installed)", filename, getApps().size());
			apps = repository.getAll();

			YamlConfig yamlConfig = new YamlConfig();
			yamlConfig.setPropertyElementType(Settings.class, "apps", Application.class);
			yamlConfig.setClassTag("cloudgene.mapred.util.Application", Application.class);

			YamlWriter writer = new YamlWriter(new FileWriter(filename), yamlConfig);
			writer.write(this);
			writer.close();

		} catch (Exception e) {
			log.error("Storing settings failed.", e);
		}
	}

	public String getTempPath() {
		return tempPath;
	}

	public void setTempPath(String tempPath) {
		this.tempPath = tempPath;
	}

	public String getLocalWorkspace() {
		return localWorkspace;
	}

	public void setLocalWorkspace(String localWorkspace) {
		this.localWorkspace = localWorkspace;
	}

	public Map<String, String> getMail() {
		return mail;
	}

	public void setMail(Map<String, String> mail) {
		this.mail = mail;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getTempFilename(String filename) {
		String path = getTempPath();
		String name = FileUtil.getFilename(filename);
		return FileUtil.path(path, name);
	}

	public File getTempFolder(String name) throws IOException {
		return Files.createTempDirectory(Path.of(getTempPath()), name).toFile();
	}

	public void setNotificationAfter(int notificationAfter) {
		this.notificationAfter = notificationAfter;
	}

	public int getNotificationAfter() {
		return notificationAfter;
	}

	public void setRetireAfter(int retireAfter) {
		this.retireAfter = retireAfter;
	}

	public int getRetireAfter() {
		return retireAfter;
	}

	public void setAutoRetire(boolean autoRetire) {
		this.autoRetire = autoRetire;
	}

	public boolean isAutoRetire() {
		return autoRetire;
	}

	public void setWriteStatistics(boolean writeStatistics) {
		this.writeStatistics = writeStatistics;
	}

	public boolean isWriteStatistics() {
		return writeStatistics;
	}

	public void setMaintenance(boolean maintenance) {
		this.maintenance = maintenance;
	}

	public boolean isMaintenance() {
		return maintenance;
	}

	public void setAdminMail(String adminMail) {
		this.adminMail = adminMail;
	}

	public String getAdminMail() {
		return adminMail;
	}

	public void setAdminName(String adminName) {
		this.adminName = adminName;
	}

	public String getAdminName() {
		return adminName;
	}

	public void setThreadsQueue(int threadsQueue) {
		this.threadsQueue = threadsQueue;
	}

	public int getThreadsQueue() {
		return threadsQueue;
	}

	public int getMaxRunningJobsPerUser() {
		return maxRunningJobsPerUser;
	}

	public void setMaxRunningJobsPerUser(int maxRunningJobsPerUser) {
		this.maxRunningJobsPerUser = maxRunningJobsPerUser;
	}

	public void setDatabase(Map<String, String> database) {
		this.database = database;
	}

	public Map<String, String> getDatabase() {
		return database;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public int getUploadLimit() {
		return uploadLimit;
	}

	public void setUploadLimit(int uploadLimit) {
		this.uploadLimit = uploadLimit;
	}

	public int getAutoRetireInterval() {
		return autoRetireInterval;
	}

	public void setAutoRetireInterval(int autoRetireInterval) {
		this.autoRetireInterval = autoRetireInterval;
	}

	public void setPlugins(Map<String, Map<String, String>> plugins) {
		this.plugins = plugins;
	}

	public Map<String, Map<String, String>> getPlugins() {
		return plugins;
	}

	public Map<String, String> getPlugin(String plugin) {
		if (plugins != null) {
			return plugins.get(plugin);
		} else {
			return null;
		}
	}

	public void setErrorHandlers(List<Map<String, String>> errorHandlers) {
		this.errorHandlers = errorHandlers;
	}

	public List<Map<String, String>> getErrorHandlers() {
		return errorHandlers;
	}

	public void setRepository(ApplicationRepository repository) {
		this.repository = repository;
	}

	public void setGoogleAnalytics(String googleAnalytics) {
		this.googleAnalytics = googleAnalytics;
	}

	public String getGoogleAnalytics() {
		return googleAnalytics;
	}

	public void setMaxDownloads(int maxDownloads) {
		this.maxDownloads = maxDownloads;
	}

	public int getMaxDownloads() {
		return maxDownloads;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPort() {
		return port;
	}

	public void setWorkspaceCleanup(boolean workspaceCleanup) {
		this.workspaceCleanup = workspaceCleanup;
	}

	public boolean getWorkspaceCleanup() {
		return workspaceCleanup;
	}

	public void setShowLogs(boolean showLogs) {
		this.showLogs = showLogs;
	}

	public boolean isShowLogs() {
		return showLogs;
	}

	public ApplicationRepository getApplicationRepository() {
		return repository;
	}

	public Map<String, String> getExternalWorkspace() {
		return externalWorkspace;
	}

	public void setExternalWorkspace(Map<String, String> externalWorkspace) {
		this.externalWorkspace = externalWorkspace;
	}

	public String getExternalWorkspaceLocation() {
		if (externalWorkspace == null) {
			externalWorkspace = new HashMap<>();
			externalWorkspace.put("type", "local");
			externalWorkspace.put("location", getLocalWorkspace());
		}

		if (externalWorkspace.get("location") == null) {
			return "";
		}

		return externalWorkspace.get("location");
	}

	public String getExternalWorkspaceType() {
		if (externalWorkspace == null) {
			externalWorkspace = new HashMap<>();
			externalWorkspace.put("type", "local");
			externalWorkspace.put("location", getLocalWorkspace());
		}

		if (externalWorkspace.get("type") == null) {
			return "";
		}

		return externalWorkspace.get("type");
	}

	public List<String> getCounters() {
		return counters;
	}

	public void setCounters(List<String> counters) {
		this.counters = counters;
	}

	/**
	 * Sets the authority (domain + optional port) of this website's URL.
	 * <p>
	 * {@code serverUrl} must be non-blank, and parseable as a URL authority.
	 * Otherwise, {@link IllegalArgumentException} is thrown.
	 */
	public void setServerUrl(String serverUrl) {
		if (serverUrl == null || serverUrl.isBlank()) {
			throw new IllegalArgumentException("server URL must be a non-blank string.");
		}
		serverUrl = serverUrl.strip();

		try {
			URI uri = new URI("//" + serverUrl).parseServerAuthority();

			if (uri.getHost() == null
					|| uri.getUserInfo() != null
					|| !uri.getRawPath().isEmpty()
					|| uri.getRawQuery() != null
					|| uri.getRawFragment() != null
					|| uri.getHost().endsWith(".")
					|| serverUrl.endsWith(":")
					|| uri.getPort() > 65_535) {
				throw new IllegalArgumentException("Invalid server URL: " + serverUrl);
			}

			this.serverUrl = serverUrl;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid server URL: " + serverUrl, e);
		}
	}

	/** Returns the authority (domain + optional port) of this website's URL */
	public String getServerUrl() {
		return serverUrl;
	}

	/** Returns the path segment of this website's URL. */
	public @NonNull String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Sets the path segment of this website's URL.
	 * <p>
	 * {@code baseUrl} must either be null/blank (normalized to empty string) or a
	 * path using forward-slash as separator ({@code /}).
	 * <p>
	 * After normalization, valid non-empty strings will be stored with a leading
	 * slash and no trailing slashes (so you can make paths by appending a string
	 * that starts with a slash, e.g. {@code "/foo/bar"}).
	 */
	public void setBaseUrl(@Nullable String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			this.baseUrl = "";
			return;
		}
		baseUrl = baseUrl.strip();

		while (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}

		// Corner case: the string was a sequence of slashes.
		if (baseUrl.isEmpty()) {
			this.baseUrl = "";
			return;
		}

		try {
			URI uri = new URI(baseUrl);

			if (uri.getScheme() != null
					|| uri.getRawAuthority() != null
					|| uri.getRawQuery() != null
					|| uri.getRawFragment() != null) {
				throw new IllegalArgumentException("Invalid base URL: " + baseUrl);
			}

			if (!baseUrl.startsWith("/")) {
				baseUrl = "/" + baseUrl;
			}

			this.baseUrl = baseUrl;
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid base URL: " + baseUrl, e);
		}
	}

	/**
	 * Returns the full URL pointing to this website:
	 * {@code https://<serverUrl>/<baseUrl>}
	 */
	public String getFullUrl() {
		return "https://" + getServerUrl() + getBaseUrl();
	}

	public Environment buildEnvironment() {
		return new Environment(this);
	}

	public boolean isEmailRequired() {
		return emailRequired;
	}

	public void setEmailRequired(boolean emailRequired) {
		this.emailRequired = emailRequired;
	}
}
