package cloudgene.mapred.database.updates;

import java.io.BufferedReader;
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

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.connector.DatabaseConnector;
import io.micronaut.core.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseUpdater {

	protected static final Logger log = LoggerFactory.getLogger(DatabaseUpdater.class);

	private final @NonNull Database database;
	private final @NonNull URL updatesFile;
	private final @NonNull String currentVersion;

	private final @NonNull DatabaseConnector connector;
	private final @NonNull Map<String, IUpdateListener> listeners;

	private final @NonNull String oldVersion;
	private final boolean needsUpdate;

	public DatabaseUpdater(
			@NonNull Database database,
			@NonNull URL updatesFile,
			@NonNull String currentVersion) {

		this.database = database;
		this.updatesFile = updatesFile;
		this.currentVersion = currentVersion;

		this.connector = database.getConnector();
		if (connector == null) {
			throw new IllegalArgumentException("Database connector must be non-null.");
		}

		this.listeners = new HashMap<>();

		String oldVersion = "0.0.0";
		if (isVersionTableAvailable()) {
			String dbVersion = readVersionDB();
			if (dbVersion != null) {
				oldVersion = dbVersion;
				log.info("Read current DB version: {}", oldVersion);
			}
		}
		this.oldVersion = oldVersion;

		log.info("Current app version: {}", currentVersion);
		needsUpdate = (compareVersion(currentVersion, oldVersion) > 0);
	}

	/**
	 * Assigns {@code listener} as the one and only update listener for
	 * {@code version}. Replaces any existing listeners for the same version.
	 */
	public void addListener(String version, IUpdateListener listener) {
		listeners.put(version, listener);
	}

	/**
	 * If the database needs updating, updates it. Inserts the current version to
	 * the version table.
	 */
	public boolean updateDB() {
		if (needsUpdate()) {
			log.info("Database needs update...");
			if (!update()) {
				log.error("Updating database failed.");
				try {
					database.disconnect();
				} catch (SQLException e) {
					log.error("Error disconnecting database", e);
				}
				return false;
			}
			log.info("Update database done.");
		} else {
			log.info("Database is already up-to-date.");
			if (!isVersionTableAvailable()) {
				try {
					writeVersion(currentVersion);
				} catch (SQLException e) {
					log.error("Failed to initialize version table", e);
					return false;
				}
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
		log.info("Updating database version from {} to {}...", oldVersion, currentVersion);

		try {
			executeUpdates();
		} catch (IOException | URISyntaxException | SQLException e) {
			return false;
		}

		// Check if we need to write the current version to the DB (e.g., if it doesn't
		// contain any updates so it wasn't added by executeUpdates()).
		try {
			if (isVersionTableAvailable()) {
				String currentDBVersion = readVersionDB();
				if ((compareVersion(currentVersion, currentDBVersion) > 0)) {
					writeVersion(currentVersion);
				}
			} else {
				writeVersion(currentVersion);
			}
		} catch (SQLException e) {
			return false;
		}

		log.info("Database version successfully updated.");
		return true;
	}

	public boolean needsUpdate() {
		return needsUpdate;
	}

	/**
	 * Creates the database version table if not already present, and inserts
	 * {@code version} as the latest version. If a version file exists, it is
	 * deleted. Exceptions are ignored.
	 *
	 * @param version Newest application version (semver format expected).
	 */
	private void writeVersion(String version) throws SQLException {
		if (!isVersionTableAvailable()) {
			createVersionTable();
		}

		Connection connection = connector.getDataSource().getConnection();
		PreparedStatement ps = connection.prepareStatement("INSERT INTO database_versions (version) VALUES (?)");
		ps.setString(1, version);
		ps.executeUpdate();
		log.info("Version in DB updated to: {}", version);

		connection.close();
	}

	/**
	 * Attempts to read the latest version from the {@code database_versions} table
	 * in the database. On failure, returns {@code null} (no exception is thrown).
	 */
	private String readVersionDB() {
		String sql = "SELECT version FROM database_versions "
				+ "WHERE updated_on = (SELECT MAX(updated_on) FROM database_versions) "
				+ "ORDER BY updated_on, id DESC";

		String version = null;

		try (Connection connection = connector.getDataSource().getConnection()) {
			PreparedStatement ps = connection.prepareStatement(sql);
			ResultSet result = ps.executeQuery();

			if (result.next()) {
				version = result.getString(1);
			}
		} catch (SQLException e) {
			// pass
		}

		return version;
	}

	/**
	 * Parses {@code updatesFile}, and executes all relevant SQL commands.
	 * <p>
	 * Expects the file contents to be a series of SQL statements separated by
	 * comments, with each comment being the semver version that applies to the
	 * following SQL statements. Only versions between {@code oldVersion}
	 * (exclusive) and {@code currentVersion} (inclusive) are processed.
	 * <p>
	 * If a version is in range and is present as a comment in the file, also checks
	 * if a listener is registered in {@code listeners} for that version, and
	 * triggers the callbacks {@link IUpdateListener#beforeUpdate(Database)} (before
	 * executing any associated SQL statements) and
	 * {@link IUpdateListener#afterUpdate(Database)} (after all associated SQL
	 * statements). In particular, there don't need to be any SQL statements: as
	 * long as the comment is present, the callbacks are triggered.
	 */
	private void executeUpdates() throws IOException, URISyntaxException, SQLException {

		try (InputStream is = updatesFile.openStream();
				InputStreamReader sr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(sr)) {

			String strLine;
			StringBuilder builder = new StringBuilder();
			boolean reading = false;
			String version = null;

			while ((strLine = br.readLine()) != null) {
				if (strLine.startsWith("--")) { // New version block found
					if (builder.length() > 0) { // Old version block had commands to run
						executeSQL(builder.toString(), version);
						builder.setLength(0);
						IUpdateListener listener = listeners.get(version);
						if (listener != null) {
							listener.afterUpdate(database);
						}
					}

					// Initialize the new version block
					version = strLine.replace("--", "").trim();
					reading = (compareVersion(version, oldVersion) > 0 && compareVersion(version, currentVersion) <= 0);
					if (reading) {
						log.info("Loading SQL update for version {}", version);
						IUpdateListener listener = listeners.get(version);
						if (listener != null) {
							listener.beforeUpdate(database);
						}
					}
				}

				// If we already found a version comment, and the version is within range,
				// accumulate SQL statements into builder.
				if (reading) {
					builder.append("\n");
					builder.append(strLine);
				}
			}

			// TODO(Marc): This is wrong! It doesn't trigger the callback.
			// last block
			executeSQL(builder.toString(), version);
		}
	}

	/**
	 * If {@code sql} contains SQL commands to run, runs them and writes
	 * {@code version} to the database.
	 *
	 * @param sql     Update commands to run. Comments and surrounding whitespace
	 *                are removed.
	 * @param version Version that this update belongs to. Written to DB iif
	 *                {@code sql} is non-empty and runs without issue.
	 * @throws SQLException If anything goes wrong (DB connectivity, {@code sql}
	 *                      content issues...)
	 */
	public void executeSQL(String sql, String version) throws SQLException {
		String cleanedSQL = sql
				.replaceAll("(?s)/\\*.*?\\*/", "") // remove block comments
				.replaceAll("(?m)^\\s*--.*?$", "") // remove full line comments
				.replaceAll("(?m)(?<=\\s)--.*?$", "") // remove inline comments after SQL
				.trim();

		if (!cleanedSQL.isEmpty()) {
			Connection connection = connector.getDataSource().getConnection();
			PreparedStatement ps = connection.prepareStatement(cleanedSQL);
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

	/**
	 * Returns {@code true} if the database can be reached and a table called
	 * {@code database_versions} is confirmed to exist. Returns {@code false}
	 * otherwise (doesn't throw).
	 */
	public boolean isVersionTableAvailable() {
		try {
			return database.getConnector().tableExists("database_versions");
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Attempts to create the table {@code database_versions} in the database.
	 */
	public void createVersionTable() throws SQLException {
		String sql = "CREATE TABLE database_versions ("
				+ "id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, "
				+ "version VARCHAR(255) NOT NULL, "
				+ "updated_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

		Connection connection = connector.getDataSource().getConnection();
		PreparedStatement statement = connection.prepareStatement(sql);
		statement.executeUpdate();

		connection.close();
		log.info("Table database_versions created.");
	}
}
