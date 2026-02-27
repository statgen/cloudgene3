package cloudgene.mapred.apps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.util.config.Configuration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.util.DatabaseUpdater;
import cloudgene.mapred.util.GitHubException;
import cloudgene.mapred.util.GitHubUtil;
import cloudgene.mapred.util.GitHubUtil.Repository;
import cloudgene.mapred.util.S3Util;
import cloudgene.mapred.wdl.WdlApp;
import genepi.io.FileUtil;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class ApplicationRepository {

	private List<Application> apps;

	private Map<String, Application> indexApps;

	public static final String CONFIG_PATH = Configuration.getConfigDirectory();

	private String appsFolder = Configuration.getAppsDirectory();;

	private static final Logger log = LoggerFactory.getLogger(ApplicationRepository.class);

	public static int APPS = 1;

	public static int APPS_AND_DATASETS = 2;

	public static int DATASETS = 4;

	public ApplicationRepository() {
		apps = new ArrayList<>();
		reload();
	}

	public void setAppsFolder(String appsFolder) {
		this.appsFolder = appsFolder;
	}

	public List<Application> getAll() {
		return apps;
	}

	public void setApps(@NotNull List<Application> apps) {
		this.apps = apps;
		reload();
	}

	public void reload() {
		indexApps = new HashMap<>();
		log.info("Reload applications...");

		for (Application app : apps) {
			try {
				log.info("Load workflow file {}", app.getFilename());
				app.loadWdlApp();
				log.info("Application {} loaded.", app.getId());
			} catch (IOException e) {
				log.error("Application {} has syntax errors.", app.getFilename(), e);
			}
			indexApps.put(app.getId(), app);
		}

		Collections.sort(apps);
		log.info("Loaded {} applications.", apps.size());
	}

	public Application getByIdAndUser(String id, User user) {
		Application application = getById(id);

		if (hasAccess(user, application) && isActivated(application)) {
			return application;
		}

		return null;
	}

	public Application getById(String id) {
		Application application = indexApps.get(id);
		if (application != null) {
			return application;
		}

		// try without version
		List<Application> versions = new ArrayList<>();
		for (Application app : apps) {
			if (id.equals(app.getWdlApp().getId())) {
				versions.add(app);
			}
		}

		if (versions.isEmpty()) {
			return null;
		}

		// find latest
		Application latest = versions.get(0);
		for (int i = 1; i < versions.size(); i++) {
			String latestVersion = latest.getWdlApp().getVersion();
			String version = versions.get(i).getWdlApp().getVersion();
			if (DatabaseUpdater.compareVersion(version, latestVersion) == 1) {
				latest = versions.get(i);
			}
		}

		return latest;
	}

	public List<Application> getAllByUser(@Nullable User user, int filter) {
		List<Application> listApps = new ArrayList<>();

		for (Application application : getAll()) {

			if (!hasAccess(user, application) || !isActivated(application)) {
				continue;
			}

			WdlApp wdlApp = application.getWdlApp();

			if (filter == APPS_AND_DATASETS) {
				listApps.add(application);
			} else if (filter == APPS && wdlApp.getWorkflow() != null) {
				listApps.add(application);

			} else if (filter == DATASETS && wdlApp.getWorkflow() != null) {
				listApps.add(application);
			}

		}

		Collections.sort(listApps);
		return listApps;
	}

	public void remove(@NotNull Application application) throws IOException {
		log.info("Remove application " + application.getId());
		apps.remove(application);
		reload();
	}

	public void updateConfig(@NotNull Application app, @Nullable Map<String, String> config) throws IOException {

		WdlApp wdlApp = app.getWdlApp();

		if (config == null) {
			return;
		}

		for (IPlugin plugin : PluginManager.getInstance().getPlugins()) {
			Map<String, String> updatedConfig = plugin.getConfig(wdlApp);
			if (updatedConfig == null) {
				continue;
			}
			for (String key : config.keySet()) {
				updatedConfig.put(key, config.get(key));
			}
			plugin.updateConfig(wdlApp, updatedConfig);
		}

	}

	public List<Application> install(@NotNull String url) throws IOException, GitHubException, URISyntaxException {

		List<Application> applications = new ArrayList<>();
		Application application = null;
		if (url.startsWith("http://") || url.startsWith("https://")) {
			return installFromUrl(url);
		} else if (url.startsWith("s3://")) {
			application = installFromS3(url);
		} else if (url.startsWith("github://")) {

			String repo = url.replace("github://", "");
			Repository repository = GitHubUtil.parseShorthand(repo);
			application = installFromGitHub(repository);

		} else {

			if (new File(url).exists()) {

				if (url.endsWith(".zip")) {
					application = installFromZipFile(url);
				} else if (url.endsWith(".yaml")) {
					application = installFromYaml(url, false);
				} else if (url.endsWith(".yml")) {
					application = installFromYaml(url, false);
				} else {
					application = installFromDirectory(url, false);
				}

			} else {
				String repo = url.replace("github://", "");
				Repository repository = GitHubUtil.parseShorthand(repo);
				application = installFromGitHub(repository);
			}
		}
		if (application != null) {
			applications.add(application);
		}
		return applications;

	}

	/**
	 * Returns the path {@code {appsFolder}/archive_{hash}.zip}, where
	 * {@code hash} is calculated from the provided {@code key}.
	 *
	 * @param key Used to uniquely identify the download, to avoid collisions.
	 * @return Path to a (potential) archive file unique to the {@code key}'s hash.
	 */
	private File getArchiveFile(@NotNull String key) {
		int hash = key.hashCode();
		String path = FileUtil.path(appsFolder, "archive_" + hash + ".zip");
		return new File(path);
	}

	public List<Application> installFromUrl(@NotNull String url)
			throws IOException, GitHubException, URISyntaxException {

		if (!url.endsWith(".zip")) {
			return installFromUrlRepository(url);
		}

		// download file from url
		File zipFile = getArchiveFile(url);
		URI uri = new URI(url);
		FileUtils.copyURLToFile(uri.toURL(), zipFile);

		Application application = installFromZipFile(zipFile.getAbsolutePath());

		zipFile.delete();

		List<Application> applications = new ArrayList<>();
		applications.add(application);

		return applications;
	}

	public List<Application> installFromUrlRepository(@NotNull String url)
			throws IOException, GitHubException, URISyntaxException {

		Pattern pattern = Pattern.compile("@([^/\\?]*)");
		Matcher matcher = pattern.matcher(url);

		String version = "latest";
		if (matcher.find()) {
			version = matcher.group(1);
			url = url.replace(matcher.group(0), "");
		}

		File file = File.createTempFile("repo", ".json");
		String filename = file.getAbsolutePath();
		URI uri = new URI(url);

		FileUtils.copyURLToFile(uri.toURL(), file);
		return installFromRepository(filename, version);
	}

	public List<Application> installFromRepository(@NotNull String file, String version)
			throws IOException, GitHubException, URISyntaxException {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(new File(file));
		String type = root.path("type").asText();
		JsonNode releases = root.path("releases");

		if (!releases.isArray() || releases.isEmpty()) {
			throw new IOException("No releases found.");
		}

		JsonNode release = getVersion(releases, version);
		if (release == null) {
			throw new IOException("Version " + version + " not found.");
		}

		switch (type) {
			case "application": {
				String url = release.path("url").asText();
				System.out.println("Installing application from " + url + "...");
				return install(url);
			}
			case "pack": {
				JsonNode content = release.path("content");
				if (content.isArray()) {
					List<Application> applications = new ArrayList<>();
					for (JsonNode item : content) {
						String itemUrl = item.path("url").asText();
						System.out.println("Installing application from " + itemUrl + "...");
						List<Application> installedApplications = install(itemUrl);
						if (item.has("config") && item.hasNonNull("config")) {
							Map<String, String> config = mapper.convertValue(
									item.get("config"),
									new TypeReference<Map<String, String>>() {});
							for (Application application : installedApplications) {
								System.out.println("Configure application " + application.getId());
								updateConfig(application, config);
							}
						}
						applications.addAll(installedApplications);
					}
					return applications;
				}
				throw new IOException("Property 'content' is not an array.");
			}
			default:
				throw new IOException("Unknown repository type: " + type);
		}
	}

	public Application installFromS3(String url) throws IOException {
		// download file from s3 bucket
		if (url.endsWith(".zip")) {
			File zipFile = getArchiveFile(url);
			S3Util.copyToFile(url, zipFile);

			Application application = installFromZipFile(zipFile.getAbsolutePath());

			zipFile.delete();
			return application;
		}

		String appPath = FileUtil.path(appsFolder, "s3-download");
		FileUtil.deleteDirectory(appPath);
		FileUtil.createDirectory(appPath);

		S3Util.UrlParts urlParts = S3Util.getParts(url);
		String baseKey = urlParts.key();

		ObjectListing listing = S3Util.listObjects(url);

		// create folders
		for (S3ObjectSummary summary : listing.getObjectSummaries()) {
			String bucket = summary.getBucketName();
			String key = summary.getKey();

			if (!summary.getKey().endsWith("/")) {
				continue;
			}

			System.out.println("Found folder" + bucket + "/" + key);
			String relativeKey = summary.getKey().replaceAll(baseKey, "");
			String target = FileUtil.path(appPath, relativeKey);
			FileUtil.createDirectory(target);
		}

		// copy files
		for (S3ObjectSummary summary : listing.getObjectSummaries()) {
			String bucket = summary.getBucketName();
			String key = summary.getKey();

			if (summary.getKey().endsWith("/")) {
				continue;
			}

			System.out.println("Found file" + bucket + "/" + key);

			String relativeKey = summary.getKey().replaceAll(baseKey, "");
			String target = FileUtil.path(appPath, relativeKey);
			File file = new File(target);

			// create parent folder
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}

			System.out.println("Copy file from " + bucket + "/" + key + " to " + target);
			S3Util.copyToFile(bucket, key, file);
		}

		try {
			return installFromDirectory(appPath, true);
		} finally {
			FileUtil.deleteDirectory(appPath);
		}
	}

	public Application installFromGitHub(@NotNull Repository repository)
			throws IOException, URISyntaxException, GitHubException {

		String url = GitHubUtil.buildUrlFromRepository(repository);
		File zipFile = getArchiveFile(url);
		URI uri = new URI(url);

		FileUtils.copyURLToFile(uri.toURL(), zipFile);

		String zipFilename = zipFile.getAbsolutePath();
		Application application = installFromZipFile(zipFilename, repository.getYaml());

		zipFile.delete();
		return application;
	}

	public Application installFromZipFile(@NotNull String zipFilename) throws IOException {
		return installFromZipFile(zipFilename, null);
	}

	public Application installFromZipFile(@NotNull String zipFilename, @Nullable String yamlFilename)
			throws IOException {

		// extract in apps folder
		int hash = zipFilename.hashCode();
		String appPath = FileUtil.path(appsFolder, "archive_" + hash);

		FileUtil.deleteDirectory(appPath);
		FileUtil.createDirectory(appPath);

		try {
			ZipFile file = new ZipFile(zipFilename);
			file.extractAll(appPath);
			file.close();
		} catch (ZipException e) {
			throw new IOException(e);
		}

		try {
			return installFromDirectory(appPath, true, yamlFilename);
		} finally {
			FileUtil.deleteDirectory(appPath);
		}
	}

	public Application installFromDirectory(@NotNull String path, boolean moveToApps) throws IOException {
		return installFromDirectory(path, moveToApps, null);
	}

	public Application installFromDirectory(@NotNull String path, boolean moveToApps, @Nullable String customYaml)
			throws IOException {

		String name = "cloudgene.yaml";
		if (customYaml != null) {
			name = customYaml;
		}

		String cloudgeneFilename = FileUtil.path(path, name);
		if (new File(cloudgeneFilename).exists()) {
			Application application = installFromYaml(cloudgeneFilename, moveToApps);
			if (application != null) {
				return application;
			}
		}

		// No cloudgene.yaml found. try all other yaml files.
		String[] files = FileUtil.getFiles(path, "*.yaml");
		for (String filename : files) {
			Application application = installFromYaml(filename, moveToApps);
			if (application != null) {
				return application;
			}
		}

		// search in subfolders. e.g. github zip extracts all in a subfolder.
		for (String directory : getDirectories(path)) {
			Application application = installFromDirectory(directory, moveToApps, customYaml);
			if (application != null) {
				return application;
			}
		}

		return null;
	}

	public Application installFromYaml(String filename, boolean moveToApps) throws IOException {

		Application application = new Application();
		application.setFilename(filename);
		application.setPermission("user");

		try {
			application.loadWdlApp();
		} catch (IOException e) {
			log.warn("Ignore file " + filename + ". Not a valid cloudgene file.", e);
			return null;
		}

		// application with same version is already installed.
		if (indexApps.get(application.getId()) != null) {
			throw new IOException("Application " + application.getId() + " is already installed");
		}

		if (moveToApps) {
			File file = new File(filename);
			File folder = file.getParentFile();

			String targetPath = FileUtil.path(appsFolder, application.getWdlApp().getId(),
					application.getWdlApp().getVersion());

			FileUtil.createDirectory(targetPath);
			File target = new File(targetPath);

			// copy to apps and update filename
			FileUtils.copyDirectory(folder, target);
			application.setFilename(FileUtil.path(targetPath, file.getName()));

			// delete older directory
			FileUtil.deleteDirectory(folder);

			try {
				application.loadWdlApp();
			} catch (IOException e) {
				log.warn("Ignore file " + filename + ". Not a valid cloudgene file.", e);
				return null;
			}
		}

		apps.add(application);
		indexApps.put(application.getId(), application);

		return application;
	}

	private String[] getDirectories(@NotNull String path) {
		File dir = new File(path);
		File[] files = dir.listFiles();

		int count = 0;
		for (File file : files) {
			if (file.isDirectory()) {
				count++;
			}
		}

		String[] names = new String[count];

		count = 0;
		for (File file : files) {
			if (file.isDirectory()) {
				names[count] = file.getAbsolutePath();
				count++;
			}
		}

		return names;
	}

	public String getConfigDirectory(String id) {
		Application app = getById(id);
		return getConfigDirectory(app.getWdlApp());
	}

	public String getConfigDirectory(WdlApp app) {
		return FileUtil.path(CONFIG_PATH, app.getId(), app.getVersion());
	}

	public boolean hasAccess(@Nullable User user, @Nullable Application application) {
		if (application == null || user == null) {
			return false;
		}
		return user.isAdmin() || user.hasRole(application.getPermissions());
	}

	public boolean isActivated(Application application) {
		if (application == null) {
			return false;
		}
		return application.isEnabled() && application.isLoaded() && !application.hasSyntaxError();
	}

	public static JsonNode getVersion(@NotNull JsonNode releases, @Nullable String version) {
		if ("latest".equalsIgnoreCase(version)) {
			return releases.get(0);
		}

		for (JsonNode release : releases) {
			if (release.has("version") && release.get("version").asText().equals(version)) {
				return release;
			}
		}

		return null;
	}
}
