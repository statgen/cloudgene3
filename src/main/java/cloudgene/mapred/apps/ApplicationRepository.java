package cloudgene.mapred.apps;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.util.SemVer;
import cloudgene.mapred.util.config.Configuration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.util.GitHubException;
import cloudgene.mapred.util.GitHubUtil;
import cloudgene.mapred.util.GitHubUtil.Repository;
import cloudgene.mapred.util.S3Util;
import cloudgene.mapred.wdl.WdlApp;
import genepi.io.FileUtil;
import net.lingala.zip4j.ZipFile;
import software.amazon.awssdk.services.s3.model.S3Object;

public class ApplicationRepository {
	private static final Logger log = LoggerFactory.getLogger(ApplicationRepository.class);
	private static final String CONFIG_PATH = Configuration.getConfigDirectory();

	/** {@link #getAllByUser} flag to retrieve only executable apps */
	public static int APPS = 1;

	/** {@link #getAllByUser} flag to retrieve all apps, regardless of type. */
	public static int APPS_AND_DATASETS = 2;

	/**
	 * {@link #getAllByUser} flag to retrieve only dataset apps (non-executable).
	 */
	public static int DATASETS = 4;

	/**
	 * Sorted list containing all installed apps. Order enforced in {@link #reload}.
	 */
	private List<Application> apps = new ArrayList<>();

	/** Lookup mapping {@code "app-id@version" -> app}. */
	private final Map<String, Application> indexApps = new HashMap<>();

	/** Path to the root folder that apps will be installed in. */
	private String appsFolder = Configuration.getAppsDirectory();

	public void setAppsFolder(String appsFolder) {
		this.appsFolder = appsFolder;
	}

	public List<Application> getAll() {
		return apps;
	}

	public void setApps(@NonNull List<Application> apps) {
		this.apps = apps;
		reload();
	}

	public void reload() {
		indexApps.clear();
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

	/**
	 * Matches an application {@code id} with all registered apps, and if anything
	 * matches it returns the app.
	 * <p>
	 * {@code id} must match the form {@code <app-id>[@<version>]} (mandatory base
	 * ID string; optional version string separated by at-sign).
	 */
	public Application getById(@NonNull String id) {
		// NOTE(Marc): Versioned matches happen here: indexApps keys follow the full
		// <id>@<version> pattern.
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
			try {
				SemVer latestVersion = SemVer.of(latest.getWdlApp().getVersion());
				SemVer version = SemVer.of(versions.get(i).getWdlApp().getVersion());
				if (version.compareTo(latestVersion) > 0) {
					latest = versions.get(i);
				}
			} catch (IllegalArgumentException e) {
				log.warn("Non-semver compliant version found; ignoring.", e);
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
			} else if (filter == DATASETS && wdlApp.getWorkflow() == null) {
				listApps.add(application);
			}
		}

		Collections.sort(listApps);
		return listApps;
	}

	public void remove(@NonNull Application application) throws IOException {
		log.info("Removing application: {}", application.getId());
		apps.remove(application);
		reload();
	}

	public void updateConfig(@NonNull Application app, @Nullable Map<String, String> config) throws IOException {
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

	public List<Application> install(@NonNull String url) throws IOException, GitHubException, URISyntaxException {
		List<Application> applications = new ArrayList<>();
		Application application;

		if (url.startsWith("http://") || url.startsWith("https://")) {
			return installFromUrl(url);
		} else if (url.startsWith("s3://")) {
			application = installFromS3(url);
		} else if (url.startsWith("github://")) {
			String repo = url.replace("github://", "");
			Repository repository = GitHubUtil.parseShorthand(repo);
			application = installFromGitHub(repository);
		} else {
			Path asPath = Path.of(url);
			if (Files.exists(asPath)) {
				if (url.endsWith(".zip")) {
					application = installFromZipFile(asPath);
				} else if (url.endsWith(".yaml")) {
					application = installFromYaml(asPath, false);
				} else if (url.endsWith(".yml")) {
					application = installFromYaml(asPath, false);
				} else {
					application = installFromDirectory(asPath, false);
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
	private Path getArchiveFile(@NonNull String key) {
		int hash = key.hashCode();
		String filename = "archive_" + hash + ".zip";
		return Path.of(appsFolder, filename);
	}

	public List<Application> installFromUrl(@NonNull String url)
			throws IOException, GitHubException, URISyntaxException {

		if (!url.endsWith(".zip")) {
			return installFromUrlRepository(url);
		}

		// download file from url
		Path zipFile = getArchiveFile(url);
		URI uri = new URI(url);

		PathUtils.createParentDirectories(zipFile);
		PathUtils.copyFile(uri.toURL(), zipFile);

		Application application = installFromZipFile(zipFile);

		Files.delete(zipFile);

		List<Application> applications = new ArrayList<>();
		applications.add(application);

		return applications;
	}

	public List<Application> installFromUrlRepository(@NonNull String url)
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

	public List<Application> installFromRepository(@NonNull String file, String version)
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
									new TypeReference<>() {});
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

	public Application installFromS3(String uri) throws IOException {
		// download file from s3 bucket
		if (uri.endsWith(".zip")) {
			Path zipFile = getArchiveFile(uri);
			S3Util.copyToFile(uri, zipFile.toFile());

			Application application = installFromZipFile(zipFile);

			Files.delete(zipFile);
			return application;
		}

		Path appPath = Path.of(appsFolder, "s3-download");
		if (Files.exists(appPath)) {
			PathUtils.delete(appPath);
		}
		Files.createDirectories(appPath);

		S3Util.UriParts uriParts = S3Util.getParts(uri);
		String bucket = uriParts.bucket();
		String prefix = uriParts.key();

		List<S3Object> listing = S3Util.listObjects(bucket, prefix);

		// First pass: create folders.
		for (S3Object summary : listing) {
			String key = summary.key();

			// Only process dirs.
			if (!key.endsWith("/")) {
				continue;
			}

			System.out.println("Found folder: " + bucket + "/" + key);
			String relativeKey = key.replaceAll(prefix, "");
			Path target = appPath.resolve(relativeKey);
			Files.createDirectories(target);
		}

		// Second pass: copy files.
		for (S3Object summary : listing) {
			String key = summary.key();

			// Only process leaf objects.
			if (key.endsWith("/")) {
				continue;
			}

			System.out.println("Found file: " + bucket + "/" + key);

			String relativeKey = key.replaceAll(prefix, "");
			Path target = appPath.resolve(relativeKey);

			// create parent folder
			Path parent = target.getParent();
			if (!Files.exists(parent)) {
				Files.createDirectories(parent);
			}

			System.out.println("Copy file from " + bucket + "/" + key + " to " + target);
			S3Util.copyToFile(bucket, key, target.toFile());
		}

		try {
			return installFromDirectory(appPath, true);
		} finally {
			PathUtils.delete(appPath);
		}
	}

	public Application installFromGitHub(@NonNull Repository repository)
			throws IOException, GitHubException {

		URI uri = GitHubUtil.buildUrlFromRepository(repository);
		Path zipFile = getArchiveFile(uri.toString());

		PathUtils.createParentDirectories(zipFile);
		PathUtils.copyFile(uri.toURL(), zipFile);

		Application application = installFromZipFile(zipFile, repository.yaml());

		Files.delete(zipFile);
		return application;
	}

	public Application installFromZipFile(@NonNull Path zipFile) throws IOException {
		return installFromZipFile(zipFile, null);
	}

	public Application installFromZipFile(@NonNull Path zipFile, @Nullable String yamlFilename)
			throws IOException {

		// extract in apps folder
		int hash = zipFile.hashCode();
		Path appPath = Path.of(appsFolder, "archive_" + hash);

		if (Files.exists(appPath)) {
			PathUtils.delete(appPath);
		}
		Files.createDirectories(appPath);

		try (ZipFile file = new ZipFile(zipFile.toFile())) {
			file.extractAll(appPath.toString());
		}

		try {
			return installFromDirectory(appPath, true, yamlFilename);
		} finally {
			PathUtils.delete(appPath);
		}
	}

	public Application installFromDirectory(@NonNull Path path, boolean moveToApps) throws IOException {
		return installFromDirectory(path, moveToApps, null);
	}

	public Application installFromDirectory(@NonNull Path path, boolean moveToApps, @Nullable String customYaml)
			throws IOException {

		String name = "cloudgene.yaml";
		if (customYaml != null) {
			name = customYaml;
		}

		Path cloudgeneFile = path.resolve(name);
		if (Files.exists(cloudgeneFile)) {
			Application application = installFromYaml(cloudgeneFile, moveToApps);
			if (application != null) {
				return application;
			}
		}

		// No cloudgene.yaml found. try all other yaml files.
		try (DirectoryStream<Path> yamlFiles = Files.newDirectoryStream(path, "*.yaml")) {
			for (Path file : yamlFiles) {
				Application application = installFromYaml(file, moveToApps);
				if (application != null) {
					return application;
				}
			}
		}

		// Search in subfolders. e.g. github zip extracts all in a subfolder.
		try (DirectoryStream<Path> subDirectories = Files.newDirectoryStream(path, Files::isDirectory)) {
			for (Path subDir : subDirectories) {
				Application application = installFromDirectory(subDir, moveToApps, customYaml);
				if (application != null) {
					return application;
				}
			}
		}

		return null;
	}

	public Application installFromYaml(Path file, boolean moveToApps) throws IOException {

		Application application = new Application();
		application.setFilename(file.toString());
		application.setPermission("user");

		try {
			application.loadWdlApp();
		} catch (IOException e) {
			log.warn("Ignore file {}. Not a valid cloudgene file.", file, e);
			return null;
		}

		// application with same version is already installed.
		if (indexApps.get(application.getId()) != null) {
			throw new IOException("Application " + application.getId() + " is already installed");
		}

		if (moveToApps) {
			Path parent = file.getParent();

			Path target = Path.of(appsFolder, application.getWdlApp().getId(), application.getWdlApp().getVersion());

			Files.createDirectories(target);

			// copy to apps and update filename
			PathUtils.copyDirectory(parent, target);
			application.setFilename(target.resolve(file.getFileName()).toString());

			// delete older directory
			PathUtils.delete(parent);

			try {
				application.loadWdlApp();
			} catch (IOException e) {
				log.warn("Ignore file {}. Not a valid Cloudgene file.", file, e);
				return null;
			}
		}

		apps.add(application);
		indexApps.put(application.getId(), application);

		return application;
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

	public static JsonNode getVersion(@NonNull JsonNode releases, @Nullable String version) {
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
