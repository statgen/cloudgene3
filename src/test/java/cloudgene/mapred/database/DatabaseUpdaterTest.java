package cloudgene.mapred.database;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.database.util.*;
import io.micronaut.core.annotation.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseUpdaterTest {

	@Test
	public void testListeners() throws SQLException, MalformedURLException {
		Database db = loadTestDb("DatabaseUpdaterTest_testListeners");
		URL updatesFile = new File("test-data/test-updates.sql").toURI().toURL();

		DatabaseUpdater updater = new DatabaseUpdater(
				db,
				updatesFile,
				"1.0.0");

		List<String> records = new ArrayList<>();

		updater.addListener("0.0.1", new VersionRecorder("0.0.1", records));
		updater.addListener("0.0.2", new VersionRecorder("0.0.2", records));
		updater.addListener("0.0.3", new VersionRecorder("0.0.3", records));

		boolean result = updater.updateDB();
		assertTrue(result);

		assertEquals(
				List.of(
						// 0.0.1 is present in test-updates.sql and actually has contents.
						"Before update: 0.0.1",
						"After update: 0.0.1",

						// 0.0.2 is present in test-updates.sql but is empty.
						"Before update: 0.0.2",
						"After update: 0.0.2"

				// 0.0.3 is not present in test-updates.sql, so it's skipped.
				// 0.1.0 exists in test-updates.sql, but has no attached listener.
				),
				records);

		db.disconnect();
	}

	private static Database loadTestDb(@NonNull String dbName) throws SQLException {
		if (dbName == null || dbName.isBlank()) {
			throw new IllegalArgumentException("dbName must be non-null and non-blank.");
		}

		DatabaseConnector connector = DatabaseConnectorFactory.createConnector(Map.of(
				"driver", "h2",
				"database", "mem:" + dbName));
		assertNotNull(connector);

		Database db = new Database();
		db.connect(connector);

		return db;
	}

	private static class VersionRecorder implements IUpdateListener {

		private final String version;
		private final List<String> records;

		public VersionRecorder(String version, List<String> records) {
			this.version = version;
			this.records = records;
		}

		@Override
		public void beforeUpdate(Database database) {
			records.add("Before update: " + version);
		}

		@Override
		public void afterUpdate(Database database) {
			records.add("After update: " + version);
		}
	}
}
