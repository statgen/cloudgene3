package cloudgene.mapred.database.connector.h2;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.micronaut.core.annotation.NonNull;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.connector.DatabaseConnector;

public class H2Connector implements DatabaseConnector {

	protected static final Logger log = LoggerFactory.getLogger(H2Connector.class);

	private BasicDataSource dataSource;

	private final @NonNull String path;
	private final String user;
	private final String password;
	private final boolean multiUser;

	public H2Connector(@NonNull String path, String user, String password, boolean multiUser) {
		this.user = user;
		this.password = password;
		this.multiUser = multiUser;

		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("path must be non-null and non-empty");
		}

		if (path.startsWith("mem:")) {
			// In-memory database
			this.path = path;
		} else if (path.startsWith("/")) {
			// Absolute path
			this.path = path;
		} else {
			// Relative path
			this.path = "./" + path;
		}
	}

	@Override
	public void connect() throws SQLException {
		log.debug("Establishing connection to {}@{}", user, path);

		if (DbUtils.loadDriver("org.h2.Driver")) {
			try {
				dataSource = new BasicDataSource();
				dataSource.setDriverClassName("org.h2.Driver");

				if (multiUser) {
					dataSource.setUrl("jdbc:h2:" + path + ";AUTO_SERVER=TRUE;MODE=MySQL");
				} else {
					dataSource.setUrl("jdbc:h2:" + path + ";MODE=MySQL");
				}

				dataSource.setUsername(user);
				dataSource.setPassword(password);
				dataSource.setMaxIdle(10_000);
				dataSource.setDefaultAutoCommit(true);
			} catch (Exception e) {
				log.error("Failed to connect to H2 database", e);
			}
		} else {
			log.error("H2 Driver Class not found");
		}
	}

	@Override
	public void disconnect() throws SQLException {
		dataSource.close();
	}

	@Override
	public BasicDataSource getDataSource() {
		return dataSource;
	}

	@Override
	public String getSchema() {
		return null;
	}

	@Override
	public boolean tableExists(String table) throws SQLException {
		boolean exists = false;

		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData meta = connection.getMetaData();

			try (ResultSet res = meta.getTables(null, null, table.toUpperCase(), new String[]{"TABLE"})) {
				exists = res.next();
			}
		}

		if (!exists) {
			log.warn("Table '{}' not found'", table);
		}

		return exists;
	}
}
