package cloudgene.mapred.database.dao;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.util.SemVer;
import io.micronaut.core.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class VersionDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(VersionDao.class);

	public VersionDao(Database database) {
		super(database);
	}

	/**
	 * Checks if {@code database_versions} exists in the database.
	 *
	 * @return {@code true} if the database was queried without issue and the table
	 *         exists.
	 */
	public boolean isTableAvailable() {
		try {
			return database.getConnector().tableExists("database_versions");
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Creates the {@code database_versions} table.
	 *
	 * @return {@code true} if the table was created without issue.
	 */
	public boolean createTable() {
		try {
			String sql = "CREATE TABLE database_versions ("
					+ "id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, "
					+ "version VARCHAR(255) NOT NULL, "
					+ "updated_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

			Connection connection = database.getConnector().getDataSource().getConnection();
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.executeUpdate();

			connection.close();
			log.debug("Table database_versions created.");
			return true;
		} catch (SQLException e) {
			log.error("Failed to create database_versions table.", e);
			return false;
		}
	}

	/**
	 * Adds {@code version} to the {@code database_versions} table. A timestamp is
	 * generated on the database side.
	 *
	 * @param version Semver-compliant application version, used for database
	 *                updates.
	 * @return {@code true} if the version was inserted without issue.
	 */
	public boolean insert(@NonNull SemVer version) {
		String sql = "INSERT INTO database_versions (version) VALUES (?)";

		try {
			Object[] params = new Object[1];
			params[0] = version.toString();

			insert(sql, params);

			log.debug("insert version successful.");
			return true;
		} catch (SQLException e) {
			log.error("insert version failed.", e);
			return false;
		}
	}

	/**
	 * Returns the most recently inserted version in {@code database_versions}.
	 * Returns {@code null} on failure.
	 */
	public SemVer findLatest() {
		String sql = "SELECT version FROM database_versions "
				+ "WHERE updated_on = (SELECT MAX(updated_on) FROM database_versions) "
				+ "ORDER BY updated_on, id DESC";

		try {
			String result = queryForObject(sql, new StringMapper());
			SemVer parsed = SemVer.of(result);

			log.debug("Find latest database version successful.");
			return parsed;
		} catch (SQLException | IllegalArgumentException e) {
			log.error("Find latest database version failed.", e);
			return null;
		}
	}
}
