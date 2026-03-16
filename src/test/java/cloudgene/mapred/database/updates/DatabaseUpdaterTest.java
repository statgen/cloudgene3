package cloudgene.mapred.database.updates;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import cloudgene.mapred.database.dao.VersionDao;
import cloudgene.mapred.database.util.*;
import cloudgene.mapred.test.TestDbUtil;
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
						TestDbUtil.getMemDb(),
						null,
						"1.0.0",
						"updatesFile must be non-null",
						false),
				arguments(
						TestDbUtil.getMemDb(),
						new File("test-data/test-updates.sql").toURI().toURL(),
						null,
						"currentVersion must be non-null and non-blank",
						false),

				// Version must not be blank.
				arguments(
						TestDbUtil.getMemDb(),
						new File("test-data/test-updates.sql").toURI().toURL(),
						"",
						"currentVersion must be non-null and non-blank",
						false),

				// Database must have a non-null connector.
				arguments(
						new Database(), // By default, connector is set to null.
						new File("test-data/test-updates.sql").toURI().toURL(),
						"1.0.0",
						"Database connector must be non-null.",
						false),

				// Version == 0.0.0 (default) -> no update needed.
				arguments(
						TestDbUtil.getMemDb(),
						new File("test-data/test-updates.sql").toURI().toURL(),
						"0.0.0",
						null,
						false),

				// Version > 0.0.0 (default) -> needs update.
				arguments(
						TestDbUtil.getMemDb(),
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

	public record UpdateDbTestCase(
			String sqlPath,
			String version,
			boolean preloadTable,
			boolean needsUpdate,
			boolean success) {}

	private static Stream<UpdateDbTestCase> provideForUpdateDB() {
		return Stream.of(
				// All these cases are normal and should result in successful runs.
				new UpdateDbTestCase("test-data/test-updates.sql", "0.0.0", false, false, true),
				new UpdateDbTestCase("test-data/test-updates.sql", "0.0.0", true, false, true),
				new UpdateDbTestCase("test-data/test-updates.sql", "1.0.0", false, true, true),

				// Wrong path to updates file -> updateDB() fails.
				new UpdateDbTestCase("fake-file.404", "1.0.0", false, true, false),
				// Invalid SQL statements -> updateDB() fails.
				new UpdateDbTestCase("test-data/test-updates-broken.sql", "1.0.0", false, true, false)
		);
	}

	@ParameterizedTest
	@MethodSource("provideForUpdateDB")
	public void testUpdateDB(UpdateDbTestCase testCase)
			throws SQLException, MalformedURLException {
		Database db = TestDbUtil.getMemDb();
		VersionDao dao = new VersionDao(db);
		URL updatesFile = new File(testCase.sqlPath).toURI().toURL();

		if (testCase.preloadTable) {
			DatabaseUpdater dummy = new DatabaseUpdater(db, updatesFile, "0.0.0");
			dummy.writeVersion("0.0.0");
		}

		DatabaseUpdater updater = new DatabaseUpdater(db, updatesFile, testCase.version);

		assertEquals(testCase.preloadTable, dao.isTableAvailable());
		assertEquals(testCase.needsUpdate, updater.needsUpdate());

		boolean observed = updater.updateDB();

		assertEquals(testCase.success, observed);

		if (testCase.success) {
			assertTrue(dao.isTableAvailable()); // Always created on success.
		}
	}

	@Test
	public void testListeners() throws SQLException, MalformedURLException {
		Database db = TestDbUtil.getMemDb();
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
		Database db = TestDbUtil.getMemDb();
		URL updatesFile = new File("test-data/test-updates.sql").toURI().toURL();

		DatabaseUpdater dummy = new DatabaseUpdater(
				db,
				updatesFile,
				"0.1.0");

		boolean success = dummy.updateDB();
		assertTrue(success);

		DatabaseUpdater updater = new DatabaseUpdater(
				db,
				updatesFile,
				"1.0.0");

		assertTrue(updater.needsUpdate());
		assertEquals("0.1.0", updater.getOldVersion());
		assertEquals("1.0.0", updater.getCurrentVersion());
	}

	@Test
	public void testIncrementalUpdates() throws SQLException, MalformedURLException {
		Database db = TestDbUtil.getMemDb();
		URL updatesFile = new File("test-data/test-updates.sql").toURI().toURL();
		VersionDao dao = new VersionDao(db);
		boolean success;

		// v0.0.0: nothing to do

		DatabaseUpdater u000 = new DatabaseUpdater(db, updatesFile, "0.0.0");
		assertFalse(u000.needsUpdate());

		success = u000.updateDB();
		assertTrue(success);
		assertFalse(db.getConnector().tableExists("user")); // v0.0.1
		assertFalse(db.getConnector().tableExists("job")); // v0.1.0
		assertEquals("0.0.0", dao.findLatest());

		// v0.0.1: 'user' table created at v0.0.1

		DatabaseUpdater u001 = new DatabaseUpdater(db, updatesFile, "0.0.1");
		assertTrue(u001.needsUpdate());
		assertEquals("0.0.0", dao.findLatest());

		success = u001.updateDB();
		assertTrue(success);
		assertTrue(db.getConnector().tableExists("user")); // v0.0.1
		assertFalse(db.getConnector().tableExists("job")); // v0.1.0
		assertEquals("0.0.1", dao.findLatest());

		// v0.2.3: 'job' table created at v0.1.0

		DatabaseUpdater u023 = new DatabaseUpdater(db, updatesFile, "0.2.3");
		assertTrue(u023.needsUpdate());
		assertEquals("0.0.1", dao.findLatest());

		success = u023.updateDB();
		assertTrue(success);
		assertTrue(db.getConnector().tableExists("user")); // v0.0.1
		assertTrue(db.getConnector().tableExists("job")); // v0.1.0
		assertEquals("0.2.3", dao.findLatest());
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
