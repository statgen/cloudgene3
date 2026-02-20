package cloudgene.mapred.util.config;

import genepi.io.FileUtil;

public class Configuration {

	public static String getSettingsFilename() {
		return FileUtil.path(getConfigDirectory(), "settings.yaml");
	}

	public static String getVersionFilename() {
		return get("CG_VERSION_FILENAME", "version.txt");
	}

	public static String getConfigDirectory() {
		return get("CG_CONFIG_DIRECTORY", "config");
	}

	public static String getAppsDirectory() {
		return get("CG_APPS_DIRECTORY", "apps");
	}

	public static String get(String variable, String defaultValue) {
		String value = System.getenv(variable);
		if (value != null && !value.isEmpty()) {
			return value;
		}

		return defaultValue;
	}
}
