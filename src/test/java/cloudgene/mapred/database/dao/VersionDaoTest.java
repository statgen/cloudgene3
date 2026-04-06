package cloudgene.mapred.database.dao;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.test.TestDbUtil;
import cloudgene.mapred.util.SemVer;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class VersionDaoTest {

	@Test
	public void testVersionTable() throws SQLException {
		boolean result;

		Database db = TestDbUtil.getMemDb();
		VersionDao dao = new VersionDao(db);

		assertFalse(dao.isTableAvailable());
		assertFalse(db.getConnector().tableExists("database_versions"));

		// Creating the table when it doesn't exist succeeds.
		result = dao.createTable();

		assertTrue(result);
		assertTrue(dao.isTableAvailable());
		assertTrue(db.getConnector().tableExists("database_versions"));

		// Trying again raises an internal SQLException, so it fails.
		result = dao.createTable();

		assertFalse(result); // Failed...
		assertTrue(dao.isTableAvailable()); // ...but everything still in place.
		assertTrue(db.getConnector().tableExists("database_versions"));
	}

	@Test
	public void testInsertAndFind() throws SQLException {
		boolean result;
		SemVer observed;

		Database db = TestDbUtil.getMemDb();
		VersionDao dao = new VersionDao(db);

		dao.createTable();
		assertTrue(dao.isTableAvailable());

		observed = dao.findLatest();
		assertNull(observed); // Nothing placed in table yet -> null

		result = dao.insert(SemVer.of(1, 2, 3));
		assertTrue(result);

		observed = dao.findLatest();
		assertEquals(SemVer.of(1, 2, 3), observed);

		result = dao.insert(SemVer.of(1, 4, 3));
		assertTrue(result);

		observed = dao.findLatest();
		assertEquals(SemVer.of(1, 4, 3), observed);
	}
}
