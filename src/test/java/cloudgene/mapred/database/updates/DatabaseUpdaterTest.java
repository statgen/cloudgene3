package cloudgene.mapred.database.updates;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import cloudgene.mapred.database.connector.DatabaseConnector;
import cloudgene.mapred.database.connector.DatabaseConnectorFactory;
import cloudgene.mapred.database.util.*;
import io.micronaut.core.annotation.NonNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DatabaseUpdaterTest {

	private static Stream<Arguments> provideForConstructor() throws SQLException, MalformedURLException {
		return Stream.of(
				// None of the arguments can be null.
				arguments(
						null,
						new File("test-data/test-updates.sql").toURI().toURL(),
						"1.0.0",
						"database must be non-null",
						false),
				arguments(
						loadTestDb("DatabaseUpdaterTest_testConstructor"),
						null,
						"1.0.0",
						"updatesFile must be non-null",
						false),
				arguments(
						loadTestDb("DatabaseUpdaterTest_testConstructor"),
						new File("test-data/test-updates.sql").toURI().toURL(),
						null,
						"currentVersion must be non-null and non-blank",
						false),

				// Version must not be blank.
				arguments(
						loadTestDb("DatabaseUpdaterTest_testConstructor"),
						new File("test-data/test-updates.sql").toURI().toURL(),
						"",
						"currentVersion must be non-null and non-blank",
						false),

				// Database must have a non-null connector.
				arguments(
						loadTestDb("DatabaseUpdaterTest_testConstructor", false),
						new File("test-data/test-updates.sql").toURI().toURL(),
						"1.0.0",
						"Database connector must be non-null.",
						false),

				// Version == 0.0.0 (default) -> no update needed.
				arguments(
						loadTestDb("DatabaseUpdaterTest_testConstructor"),
						new File("test-data/test-updates.sql").toURI().toURL(),
						"0.0.0",
						null,
						false),

				// Version > 0.0.0 (default) -> needs update.
				arguments(
						loadTestDb("DatabaseUpdaterTest_testConstructor"),
						new File("test-data/test-updates.sql").toURI().toURL(),
						"1.0.0",
						null,
						true));
	}

	@ParameterizedTest
	@MethodSource("provideForConstructor")
	public void testConstructor(
			Database database,
			URL updatesFile,
			String currentVersion,
			String errMsg,
			boolean needsUpdate) {

		try {
			DatabaseUpdater updater = new DatabaseUpdater(database, updatesFile, currentVersion);
			assertNull(errMsg);

			assertEquals("0.0.0", updater.getOldVersion());
			assertEquals(currentVersion, updater.getCurrentVersion());
			assertEquals(needsUpdate, updater.needsUpdate());
		} catch (Exception e) {
			assertNotNull(errMsg);
			assertTrue(e.getMessage().startsWith(errMsg));
		}
	}

	@Test
	public void testVersionTable() throws SQLException, MalformedURLException {
		Database db = loadTestDb("DatabaseUpdaterTest_testVersionTable");
		URL updatesFile = new File("test-data/test-updates.sql").toURI().toURL();

		DatabaseUpdater updater = new DatabaseUpdater(
				db,
				updatesFile,
				"1.0.0");

		assertFalse(updater.isVersionTableAvailable());
		assertFalse(db.getConnector().tableExists("database_versions"));

		updater.writeVersion("0.0.0");

		assertTrue(updater.isVersionTableAvailable());
		assertTrue(db.getConnector().tableExists("database_versions"));
	}

	private static Stream<Arguments> provideForUpdateDB() throws SQLException, MalformedURLException {
		return Stream.of(
				arguments("0.0.0", false, false, true),
				arguments("0.0.0", true, false, true),
				arguments("1.0.0", false, true, true)

		// TODO(Marc): More cases, more coverage.
		);
	}

	@ParameterizedTest
	@MethodSource("provideForUpdateDB")
	public void testUpdateDB(String version, boolean preloadTable, boolean needsUpdate, boolean result)
			throws SQLException, MalformedURLException {
		Database db = loadTestDb("DatabaseUpdaterTest_testUpdateDB_" + RandomStringUtils.secure().next(8));
		URL updatesFile = new File("test-data/test-updates.sql").toURI().toURL();

		if (preloadTable) {
			DatabaseUpdater dummy = new DatabaseUpdater(db, updatesFile, "0.0.0");
			dummy.writeVersion("0.0.0");
		}

		DatabaseUpdater updater = new DatabaseUpdater(db, updatesFile, version);

		assertEquals(preloadTable, updater.isVersionTableAvailable());
		assertEquals(needsUpdate, updater.needsUpdate());

		boolean observed = updater.updateDB();

		assertEquals(result, observed);
		assertTrue(updater.isVersionTableAvailable()); // Always created.
	}

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

	@Test
	public void testConstructorWithExistingDBData() throws SQLException, MalformedURLException {
		Database db = loadTestDb("DatabaseUpdaterTest_testConstructorWithExistingDBData");
		URL updatesFile = new File("test-data/test-updates.sql").toURI().toURL();

		DatabaseUpdater dummy = new DatabaseUpdater(
				db,
				updatesFile,
				"0.1.0");

		dummy.updateDB();

		DatabaseUpdater updater = new DatabaseUpdater(
				db,
				updatesFile,
				"1.0.0");

		assertTrue(updater.needsUpdate());
		assertEquals("0.1.0", updater.getOldVersion());
		assertEquals("1.0.0", updater.getCurrentVersion());
	}

	private static Database loadTestDb(@NonNull String dbName) throws SQLException {
		return loadTestDb(dbName, true);
	}

	private static Database loadTestDb(@NonNull String dbName, boolean connect) throws SQLException {
		if (dbName == null || dbName.isBlank()) {
			throw new IllegalArgumentException("dbName must be non-null and non-blank.");
		}

		Database db = new Database();

		if (connect) {
			DatabaseConnector connector = DatabaseConnectorFactory.createConnector(Map.of(
					"driver", "h2",
					"database", "mem:" + dbName));

			assertNotNull(connector);
			db.connect(connector);
		}

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
