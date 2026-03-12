package cloudgene.mapred.database.connector;

import java.util.Map;

import cloudgene.mapred.database.connector.h2.H2Connector;
import cloudgene.mapred.database.connector.mysql.MySqlConnector;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

/**
 * Provides {@link #createConnector(Map)} to configure and return a
 * {@link DatabaseConnector}.
 */
public final class DatabaseConnectorFactory {

	private DatabaseConnectorFactory() {
	}

	/**
	 * Reads {@code settings} to configure and return either an {@link H2Connector}
	 * or a {@link MySqlConnector}. The {@code} driver field is used to determine
	 * the output type (should be {@code h2} or {@code mysql}).
	 */
	@Nullable
	public static DatabaseConnector createConnector(@NonNull Map<String, String> settings) {
		String driver = settings.get("driver");

		if (driver == null) {
			return null;
		}

		if (driver.equals("h2")) {
			String database = settings.get("database");
			String user = settings.get("user");
			String password = settings.get("password");

			return new H2Connector(database, user, password, false);

		} else if (driver.equals("mysql")) {
			String host = settings.get("host");
			String port = settings.get("port");
			String database = settings.get("database");
			String user = settings.get("user");
			String password = settings.get("password");

			MySqlConnector connector = new MySqlConnector(host, port, database, user, password);

			if (settings.containsKey("maxActive")) {
				int maxActive = Integer.parseInt(settings.get("maxActive"));
				connector.setMaxActive(maxActive);
			}

			if (settings.containsKey("maxWait")) {
				int maxWait = Integer.parseInt(settings.get("maxWait"));
				connector.setMaxWait(maxWait);
			}

			return connector;
		} else {
			return null;
		}
	}
}
