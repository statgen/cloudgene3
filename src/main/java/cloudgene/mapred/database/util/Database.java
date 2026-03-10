package cloudgene.mapred.database.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO(Marc): This class is a wrapper around DatabaseConnector and the only thing it
//             adds is listener support, but we don't use these listeners anywhere.
//             Remove.
public class Database {

	private static final Logger log = LoggerFactory.getLogger(Database.class);

	private @Nullable DatabaseConnector connector;
	private final List<DatabaseListener> listeners = new ArrayList<>();

	public Database() {
	}

	public void connect(@NonNull DatabaseConnector connector) throws SQLException {
		this.connector = connector;
		try {
			connector.connect();
			log.debug("Establish connection successful");
			fireChangeEvent(DatabaseListener.AFTER_CONNECTION);
		} catch (SQLException e) {
			log.error("Establish connection failed", e);
			throw e;
		}
	}

	public void disconnect() throws SQLException {
		if (connector != null) {
			if (connector.getDataSource() != null) {

				log.debug("Disconnecting");
				fireChangeEvent(DatabaseListener.BEFORE_DISCONNECTION);

				try {
					connector.disconnect();

					log.debug("Disconnection successful");
					fireChangeEvent(DatabaseListener.AFTER_DISCONNECTION);
				} catch (SQLException e) {
					log.error("Disconnection failed", e);
					throw e;
				}
			}
		}
	}

	public boolean isConnected() {
		if (connector != null) {
			return !connector.getDataSource().isClosed();
		} else {
			return false;
		}
	}

	// TODO(Marc): Connector is nullable, so this can throw a NullPointerException.
	public BasicDataSource getDataSource() {
		return connector.getDataSource();
	}

	public void addDatabaseListener(DatabaseListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeDatabaseListener(DatabaseListener listener) {
		listeners.remove(listener);
	}

	private void fireChangeEvent(int event) {
		for (DatabaseListener listener : listeners) {
			listener.onDatabaseEvent(event);
		}
	}

	@Nullable
	public DatabaseConnector getConnector() {
		return connector;
	}
}
