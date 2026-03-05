package cloudgene.mapred.database.util;

import genepi.io.FileUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseUpdater {

	protected static final Logger log = LoggerFactory.getLogger(DatabaseUpdater.class);

	private final DatabaseConnector connector;
	private final Database database;
	private final String oldVersion;
	private final String currentVersion;
	private final String filename;
	private final InputStream updateFileAsStream;
	private final boolean needUpdate;
	private final Map<String, IUpdateListener> listeners;

	public DatabaseUpdater(Database database, String filename, InputStream updateFileAsStream, String currentVersion) {
		this.filename = filename;
		this.database = database;
		this.connector = database.getConnector();
		this.updateFileAsStream = updateFileAsStream;
		this.currentVersion = currentVersion;
		this.listeners = new HashMap<>();

		if (isVersionTableAvailable()) {
			String oldVersion = readVersionDB();
			log.info("Read current DB version: {}", oldVersion);

			// Should not happen, since an entry is created when metadata table exists.
			if (oldVersion == null) {
				oldVersion = readVersion(filename);
				log.info("Read current version from DB was not successful, read it from file: {}", oldVersion);
			}

			this.oldVersion = oldVersion;
		} else {
			// check also file for backwards compatibility
			this.oldVersion = readVersion(filename);
			log.info("Read current version from file: {}", oldVersion);
		}

		log.info("Current app version: {}", currentVersion);
		needUpdate = (compareVersion(currentVersion, oldVersion) > 0);
	}

	public void addUpdate(String version, IUpdateListener listener) {
		listeners.put(version, listener);
	}

	public boolean updateDB() {

		if (needUpdate()) {
			log.info("Database needs update...");
			if (!update()) {
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

	public boolean update() {
		if (needUpdate) {
			log.info("Updating database from {} to {}...", oldVersion, currentVersion);

			try {
				readAndPrepareSqlClasspath(updateFileAsStream, oldVersion, currentVersion);
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

	public void writeVersion(String newVersion) {
		try {
			if (!isVersionTableAvailable()) {
				createVersionTable();
			}

			Connection connection = connector.getDataSource().getConnection();
			PreparedStatement ps = connection.prepareStatement("INSERT INTO database_versions (version) VALUES (?)");
			ps.setString(1, newVersion);
			ps.executeUpdate();
			log.info("Version in DB updated to: {}", newVersion);

			if (new File(filename).exists()) {
				FileUtil.deleteFile(filename);
				log.info("Deleted version.txt on file system.");
			}

			connection.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public String readVersion(String versionFile) {
		File file = new File(versionFile);

		if (file.exists()) {
			try {
				return readFileAsString(versionFile);
			} catch (Exception e) {
				return "0.0.0";
			}
		} else {
			return "0.0.0";
		}
	}

	public String readVersionDB() {
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

	public static String readFileAsString(String filename) throws java.io.IOException, URISyntaxException {
		InputStream is = new FileInputStream(filename);

		DataInputStream in = new DataInputStream(is);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		StringBuilder builder = new StringBuilder();

		while ((strLine = br.readLine()) != null) {
			builder.append(strLine);
		}

		in.close();
		return builder.toString();
	}

	public String readAndPrepareSqlClasspath(InputStream filestream, String minVersion, String maxVersion)
			throws java.io.IOException, URISyntaxException, SQLException {

		DataInputStream in = new DataInputStream(filestream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
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
		executeSQLFile(builder.toString(), version);

		in.close();
		return builder.toString();
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
