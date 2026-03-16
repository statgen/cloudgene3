package cloudgene.mapred.database.updates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import cloudgene.mapred.database.dao.VersionDao;
import cloudgene.mapred.database.util.Database;
import io.micronaut.core.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseUpdater {

	protected static final Logger log = LoggerFactory.getLogger(DatabaseUpdater.class);

	private final @NonNull Database database;
	private final @NonNull URL updatesFile;
	private final @NonNull String currentVersion;

	private final @NonNull VersionDao dao;
	private final @NonNull Map<String, IUpdateListener> listeners;

	private final @NonNull String oldVersion;
	private final boolean needsUpdate;

	public DatabaseUpdater(
			@NonNull Database database,
			@NonNull URL updatesFile,
			@NonNull String currentVersion) {

		if (database == null) {
			throw new IllegalArgumentException("database must be non-null");
		}

		if (database.getConnector() == null) {
			throw new IllegalArgumentException("Database connector must be non-null.");
		}

		if (updatesFile == null) {
			throw new IllegalArgumentException("updatesFile must be non-null");
		}

		if (currentVersion == null || currentVersion.isBlank()) {
			throw new IllegalArgumentException("currentVersion must be non-null and non-blank");
		}

		this.database = database;
		this.updatesFile = updatesFile;
		this.currentVersion = currentVersion;

		dao = new VersionDao(database);
		listeners = new HashMap<>();

		String oldVersion = "0.0.0";
		if (dao.isTableAvailable()) {
			String dbVersion = dao.findLatest();
			if (dbVersion != null) {
				oldVersion = dbVersion;
				log.info("Read current DB version: {}", oldVersion);
			}
		}
		this.oldVersion = oldVersion;

		log.info("Current app version: {}", currentVersion);
		needsUpdate = (compareVersion(currentVersion, oldVersion) > 0);
	}

	public boolean needsUpdate() {
		return needsUpdate;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public String getOldVersion() {
		return oldVersion;
	}

	/**
	 * Assigns {@code listener} as the one and only update listener for
	 * {@code version}. Replaces any existing listeners for the same version.
	 */
	public void addListener(@NonNull String version, @NonNull IUpdateListener listener) {
		listeners.put(version, listener);
	}

	// TODO(Marc): We should use the return value to indicate if updates were made,
	//             and throw and Exception if something broke.

	/**
	 * If the database needs updating, updates it. Inserts the current version to
	 * the version table.
	 *
	 * @return {@code true} if no errors were found (regardless of updates
	 *         performed).
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
			if (!dao.isTableAvailable()) {
				if (!writeVersion(currentVersion)) {
					log.error("Failed to initialize version table");
					return false;
				}
			}
		}

		String dbVersion = dao.findLatest();
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
		} catch (IOException | SQLException e) {
			return false;
		}

		// Check if we need to write the current version to the DB (e.g., if it doesn't
		// contain any updates so it wasn't added by executeUpdates()).
		if (dao.isTableAvailable()) {
			String currentDBVersion = dao.findLatest();
			if ((compareVersion(currentVersion, currentDBVersion) > 0)) {
				if (!writeVersion(currentVersion)) {
					return false;
				}
			}
		} else {
			if (!writeVersion(currentVersion)) {
				return false;
			}
		}

		log.info("Database version successfully updated.");
		return true;
	}

	/**
	 * Creates the database version table if not already present, and inserts
	 * {@code version} as the latest version. Exceptions are ignored.
	 *
	 * @param version Newest application version. Must be semver-compliant. Expected
	 *                to be greater than previously registered versions.
	 * @return {@code true} if the version was inserted without issue (including
	 *         table creation, if necessary).
	 */
	public boolean writeVersion(@NonNull String version) {
		if (!dao.isTableAvailable()) {
			if (!dao.createTable()) {
				return false;
			}
		}

		return dao.insert(version);
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
	private void executeUpdates() throws IOException, SQLException {
		UpdateFileExecutor executor = new UpdateFileExecutor();
		executor.execute();
	}

	/**
	 * Utility class. Implements the behavior in {@link #executeUpdates()}
	 */
	private class UpdateFileExecutor {
		private final StringBuilder builder = new StringBuilder();

		private String strLine = null;
		private boolean reading = false;
		private String version = null;

		public UpdateFileExecutor() {
		}

		/**
		 * Implements the behavior in {@link #executeUpdates()}
		 */
		public void execute() throws IOException, SQLException {
			try (InputStream is = updatesFile.openStream();
					InputStreamReader sr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(sr)) {

				while ((strLine = br.readLine()) != null) {
					if (strLine.startsWith("--")) {
						endBlock();
						beginBlock();
					}

					// Only if we're in a version block and version is in range.
					if (reading) {
						builder.append("\n");
						builder.append(strLine);
					}
				}

				endBlock();
			}
		}

		/**
		 * Handles the start of a new version block. Parses the version, sets
		 * {@code reading} if the block should be processed, and possibly invokes
		 * {@link IUpdateListener#beforeUpdate(Database)}
		 */
		public void beginBlock() {
			version = strLine.replace("--", "").trim();
			reading = ((compareVersion(version, oldVersion) > 0)
					&& (compareVersion(version, currentVersion) <= 0));

			if (reading) {
				log.info("Loading SQL update for version {}", version);
				IUpdateListener listener = listeners.get(version);
				if (listener != null) {
					listener.beforeUpdate(database);
				}
			}
		}

		/**
		 * Handles the end of a version block. Executes SQL commands if present, flushes
		 * the {@code builder} contents, and possibly invokes
		 * {@link IUpdateListener#afterUpdate(Database)}
		 */
		public void endBlock() throws SQLException {
			if (!builder.isEmpty()) { // Old version block had commands to run
				executeSQL(builder.toString(), version);
				builder.setLength(0);
				IUpdateListener listener = listeners.get(version);
				if (listener != null) {
					listener.afterUpdate(database);
				}
			}
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
	private void executeSQL(@NonNull String sql, @NonNull String version) throws SQLException {
		String cleanedSQL = sql
				.replaceAll("(?s)/\\*.*?\\*/", "") // remove block comments
				.replaceAll("(?m)^\\s*--.*?$", "") // remove full line comments
				.replaceAll("(?m)(?<=\\s)--.*?$", "") // remove inline comments after SQL
				.trim();

		if (!cleanedSQL.isEmpty()) {
			Connection connection = database.getConnector().getDataSource().getConnection();
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
}
