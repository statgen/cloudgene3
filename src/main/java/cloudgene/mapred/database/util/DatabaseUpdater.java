package cloudgene.mapred.database.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import io.micronaut.core.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseUpdater {

	protected static final Logger log = LoggerFactory.getLogger(DatabaseUpdater.class);

	private final @NonNull Database database;
	private final @NonNull File versionFile;
	private final @NonNull URL updatesFile;
	private final @NonNull String currentVersion;

	private final @NonNull DatabaseConnector connector;
	private final @NonNull Map<String, IUpdateListener> listeners;

	private final @NonNull String oldVersion;
	private final boolean needUpdate;

	public DatabaseUpdater(
			@NonNull Database database,
			@NonNull File versionFile,
			@NonNull URL updatesFile,
			@NonNull String currentVersion) {

		this.database = database;
		this.versionFile = versionFile;
		this.updatesFile = updatesFile;
		this.currentVersion = currentVersion;

		// TODO(Marc): database.getConnector() is nullable, but here we assume connector
		// is not null!
		this.connector = database.getConnector();
		this.listeners = new HashMap<>();

		if (isVersionTableAvailable()) {
			String oldVersion = readVersionDB();
			log.info("Read current DB version: {}", oldVersion);

			// Should not happen, since an entry is created when metadata table exists.
			if (oldVersion == null) {
				oldVersion = readVersion();
				log.info("Read current version from DB was not successful, read it from file: {}", oldVersion);
			}

			this.oldVersion = oldVersion;
		} else {
			// check also file for backwards compatibility
			this.oldVersion = readVersion();
			log.info("Read current version from file: {}", oldVersion);
		}

		log.info("Current app version: {}", currentVersion);
		needUpdate = (compareVersion(currentVersion, oldVersion) > 0);
	}

	/**
	 * Assigns {@code listener} as the one and only update listener for
	 * {@code version}.
	 * Replaces any existing listeners for the same version.
	 */
	public void addListener(String version, IUpdateListener listener) {
		listeners.put(version, listener);
	}

	/**
	 * If the database needs updating, updates it. Otherwise, inserts the current
	 * version to the version table.
	 */
	public boolean updateDB() {
		if (needUpdate()) {
			log.info("Database needs update...");
			if (!update()) { // TODO: update() ALWAYS returns true...
				log.error("Updating database failed.");
				try {
					database.disconnect();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return false;
			}
			log.info("Update database done.");
		} else {
			log.info("Database is already up-to-date.");
			if (!isVersionTableAvailable()) {
				writeVersion(currentVersion);
			}
		}

		String dbVersion = readVersionDB();
		if (!dbVersion.equals(currentVersion)) {
			log.error("App version (v{}) and DB version (v{}) does not match. Update Application to latest version.",
					currentVersion, dbVersion);
			return false;
		}

		return true;
	}

	// TODO(Marc): Only called from updateDB(). Consider merging.
	private boolean update() {
		if (needUpdate) {
			log.info("Updating database from {} to {}...", oldVersion, currentVersion);

			try {
				readAndPrepareSqlClasspath(oldVersion, currentVersion);
			} catch (IOException | URISyntaxException | SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// check if DB version match with Main version
			if (isVersionTableAvailable()) {
				String currentDBVersion = readVersionDB();
				if ((compareVersion(currentVersion, currentDBVersion) > 0)) {
					writeVersion(currentVersion);
				}
			} else {
				writeVersion(currentVersion);
			}

			log.info("Updating database was successful.");
		}

		return true;
	}

	public boolean needUpdate() {
		return needUpdate;
	}

	/**
	 * Creates the database version table if not already present, and inserts
	 * {@code version} as the latest version. If a version file exists, it is
	 * deleted. Exceptions are ignored.
	 *
	 * @param version Newest application version (semver format expected).
	 */
	private void writeVersion(String version) {
		try {
			if (!isVersionTableAvailable()) {
				createVersionTable();
			}

			Connection connection = connector.getDataSource().getConnection();
			PreparedStatement ps = connection.prepareStatement("INSERT INTO database_versions (version) VALUES (?)");
			ps.setString(1, version);
			ps.executeUpdate();
			log.info("Version in DB updated to: {}", version);

			if (versionFile.exists()) {
				versionFile.delete();
				log.info("Deleted version.txt on file system.");
			}

			connection.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * If {@code versionFile} exists, returns its contents (expects semver version).
	 * Defaults to {@code 0.0.0}.
	 */
	@NonNull
	private String readVersion() {
		if (versionFile.exists()) {
			try {
				return readFileAsString(versionFile);
			} catch (Exception e) {
				return "0.0.0";
			}
		} else {
			return "0.0.0";
		}
	}

	private String readVersionDB() {
		String sql = "SELECT version FROM database_versions "
				+ "WHERE updated_on = (SELECT MAX(updated_on) FROM database_versions) "
				+ "ORDER BY updated_on, id DESC";

		String version = null;

		try {
			Connection connection = connector.getDataSource().getConnection();
			PreparedStatement ps = connection.prepareStatement(sql);
			ResultSet result = ps.executeQuery();

			if (result.next()) {
				version = result.getString(1);
			}

			connection.close();

		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return version;
	}

	// TODO(Marc): Why is this here???
	private static String readFileAsString(File file) throws IOException {
		try (InputStream is = new FileInputStream(file);
				InputStreamReader sr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(sr)) {

			String strLine;
			StringBuilder builder = new StringBuilder();

			while ((strLine = br.readLine()) != null) {
				builder.append(strLine);
			}

			return builder.toString();
		}
	}

	private String readAndPrepareSqlClasspath(String minVersion, String maxVersion)
			throws IOException, URISyntaxException, SQLException {

		try (InputStream is = updatesFile.openStream();
				InputStreamReader sr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(sr)) {

			String strLine;
			StringBuilder builder = new StringBuilder();
			boolean reading = false;
			String version = null;

			while ((strLine = br.readLine()) != null) {
				if (strLine.startsWith("--")) {
					if (builder.length() > 0) {
						executeSQLFile(builder.toString(), version);
						builder.setLength(0);
						IUpdateListener listener = listeners.get(version);
						if (listener != null) {
							listener.afterUpdate(database);
						}
					}

					version = strLine.replace("--", "").trim();
					reading = (compareVersion(version, minVersion) > 0 && compareVersion(version, maxVersion) <= 0);
					if (reading) {
						log.info("Loading SQL update for version {}", version);
						IUpdateListener listener = listeners.get(version);
						if (listener != null) {
							listener.beforeUpdate(database);
						}
					}
				}

				if (reading) {
					builder.append("\n");
					builder.append(strLine);
				}
			}

			// last block
			executeSQLFile(builder.toString(), version);s

			return builder.toString();
		}
	}

	public void executeSQLFile(String sqlContent, String version) throws SQLException {
		String cleanedSQL = sqlContent
				.replaceAll("(?s)/\\*.*?\\*/", "") // remove block comments
				.replaceAll("(?m)^\\s*--.*?$", "") // remove full line comments
				.replaceAll("(?m)(?<=\\s)--.*?$", "") // remove inline comments after SQL
				.trim();

		if (!cleanedSQL.isEmpty()) {
			Connection connection;
			connection = connector.getDataSource().getConnection();
			PreparedStatement ps = connection.prepareStatement(sqlContent);
			ps.executeUpdate();
			connection.close();
			log.info("DB SQL Update {} finished", version);
			writeVersion(version);
		}
	}

	public static int compareVersion(String version1, String version2) {
		String[] parts1 = version1.split("-", 2);
		String[] parts2 = version2.split("-", 2);

		String[] tiles1 = parts1[0].split("\\.");
		String[] tiles2 = parts2[0].split("\\.");

		for (int i = 0; i < tiles1.length; i++) {
			int number1 = Integer.parseInt(tiles1[i].trim());
			int number2 = Integer.parseInt(tiles2[i].trim());

			if (number1 != number2) {
				return number1 > number2 ? 1 : -1;
			}
		}

		if (parts1.length > 1) {
			if (parts2.length > 1) {
				return parts1[1].compareTo(parts2[1]);
			} else {
				return -1;
			}
		} else {
			if (parts2.length > 1) {
				return 1;
			}
		}

		return 0;
	}

	public boolean isVersionTableAvailable() {
		try {
			return database.getConnector().tableExists("database_versions");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public void createVersionTable() {
		String sql = "CREATE TABLE database_versions ("
				+ "id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, "
				+ "version VARCHAR(255) NOT NULL, "
				+ "updated_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

		try {
			Connection connection = connector.getDataSource().getConnection();
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.executeUpdate();
			connection.close();
			log.info("Table database_versions created.");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
