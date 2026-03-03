package cloudgene.mapred.util;

import java.io.File;
import java.util.Map;

import cloudgene.mapred.util.config.Settings;
import genepi.io.FileUtil;
import jakarta.annotation.Nullable;

/**
 * Search for a binary file in a sequence of locations, based on the provided
 * {@code name} and invoked methods.
 * <p>
 * The first method to successfully find a binary wins.
 */
public class BinaryFinder {

	private String location = null;

	private final String name;

	public BinaryFinder(String name) {
		this.name = name;
	}

	/**
	 * Check if a binary file exists at {@code ${variable}/${this.name}}
	 *
	 * @param variable Environment variable expected to point to the binary's parent
	 *                 folder.
	 * @return this (for chaining).
	 */
	public BinaryFinder env(String variable) {
		if (location != null) {
			return this;
		}

		String path = System.getenv(variable);
		if (path != null && !path.isEmpty()) {
			String binary = FileUtil.path(path, name);
			if (new File(binary).exists()) {
				location = binary;
			}
		}

		return this;
	}

	/**
	 * Checks available plugin information for a binary file path, based on the
	 * current {@code settings}.
	 * <p>
	 * Essentially, searches in {@code ${plugin[key]}/${this.name}}
	 *
	 * @param settings Queried for the current plugin information.
	 * @param plugin   Name used to identify the relevant plugin.
	 * @param key      Name used to identify the relevant path information.
	 *
	 * @return this (for chaining).
	 */
	public BinaryFinder settings(@Nullable Settings settings, String plugin, String key) {
		if (location != null) {
			return this;
		}

		if (settings != null) {
			Map<String, String> config = settings.getPlugin(plugin);
			if (config != null) {
				String path = config.get(key);
				if (path != null && !path.isEmpty()) {
					String binary = FileUtil.path(path, name);
					if (new File(binary).exists()) {
						location = binary;
					}
				}
			}
		}

		return this;
	}

	/**
	 * Checks if the binary is a child of the given {@code path}
	 *
	 * @param path Directory potentially containing the binary.
	 * @return this (for chaining).
	 */
	public BinaryFinder path(String path) {
		if (location != null) {
			return this;
		}

		String binary = FileUtil.path(path, name);
		if (new File(binary).exists()) {
			location = binary;
		}

		return this;
	}

	/**
	 * Checks if the binary is a child of any of the directories listed in
	 * {@code $PATH}
	 *
	 * @return this (for chaining).
	 */
	public BinaryFinder envPath() {
		if (location != null) {
			return this;
		}

		String envPath = System.getenv("PATH");
		if (envPath != null && !envPath.isEmpty()) {
			String[] paths = envPath.split(":");
			for (String path : paths) {
				String binary = FileUtil.path(path, name);
				if (new File(binary).exists()) {
					location = binary;
					return this;
				}
			}
		}

		return this;
	}

	/**
	 * Returns the first matching location, if any (otherwise, returns
	 * {@code null}).
	 */
	@Nullable
	public String find() {
		return location;
	}
}
