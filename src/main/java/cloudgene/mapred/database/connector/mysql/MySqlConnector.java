package cloudgene.mapred.database.connector.mysql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.connector.AbstractDatabaseConnector;

public class MySqlConnector extends AbstractDatabaseConnector {

	private static final Logger log = LoggerFactory.getLogger(MySqlConnector.class);

	private BasicDataSource dataSource;

	private final String host;
	private final String port;
	private final String database;
	private final String user;
	private final String password;

	public MySqlConnector(String host, String port, String database, String user, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.password = password;
	}

	@Override
	public void connect() throws SQLException {
		log.debug("Establishing connection to {}@{}:{}", user, host, port);

		if (DbUtils.loadDriver("com.mysql.cj.jdbc.Driver")) {
			dataSource = createDataSource();

			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
			dataSource.setUrl("jdbc:mysql://" + host + "/" + database
					+ "?autoReconnect=true&allowMultiQueries=true&rewriteBatchedStatements=true");
			dataSource.setUsername(user);
			dataSource.setPassword(password);

			log.debug("Max Active Connections: {}", dataSource.getMaxActive());
			log.debug("Max Idle Connections: {}", dataSource.getMaxIdle());
			log.debug("Min Idle Connections: {}", dataSource.getMinIdle());
			log.debug("Initial Size: {}", dataSource.getInitialSize());
			log.debug("Max Wait: {}", dataSource.getMaxWait());
			log.debug("Default Auto-Commit: {}", dataSource.getDefaultAutoCommit());
			log.debug("Validation Query: {}", dataSource.getValidationQuery());
			log.debug("Test on Borrow: {}", dataSource.getTestOnBorrow());
			log.debug("Test on Return: {}", dataSource.getTestOnReturn());
		} else {
			throw new SQLException("MySQL Driver class not found.");
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
		return database;
	}

	@Override
	public boolean tableExists(String table) throws SQLException {
		boolean exists = false;

		try(Connection connection = dataSource.getConnection()) {
			DatabaseMetaData meta = connection.getMetaData();

			try(ResultSet res = meta.getTables(null, null, table, new String[]{"TABLE"})) {
				exists = res.next();
			}
		}

		if (!exists) {
			log.warn("Table '{}' not found'", table);
		}

		return exists;
	}
}
