package cloudgene.mapred.database.dao;

import cloudgene.mapred.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.jobs.JobValue;
import cloudgene.mapred.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JobValueDaoTest {
	private JobValueDao valueDao;
	private UserDao userDao;
	private JobDao jobDao;

	@BeforeEach
	public void setup() throws SQLException {
		TestApplication application = new TestApplication();
		Database db = application.getDatabase();

		valueDao = new JobValueDao(db);
		userDao = new UserDao(db);
		jobDao = new JobDao(db);
	}

	@Test
	public void testInsertAndGetAll() {
		List<JobValue> observed;
		boolean success;

		CloudgeneJob dummyJob = new CloudgeneJob();
		dummyJob.setId("DummyJob-JobValueDaoTest-testInsertAndGetAll");

		// Value DAO starts empty.
		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(List.of(), observed);

		// On first insert, we only see the same key-value pair with count 1.
		success = valueDao.insert("hello", "there", dummyJob);
		assertTrue(success);

		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(
				List.of(new JobValue(null, "hello", "there", 1)),
				observed);

		// Repeating the same key-value pair just increases the count.
		success = valueDao.insert("hello", "there", dummyJob);
		assertTrue(success);

		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(
				List.of(new JobValue(null, "hello", "there", 2)),
				observed);

		// Using the same key but using a different value creates a new entry.
		success = valueDao.insert("hello", "friend", dummyJob);
		assertTrue(success);

		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(
				List.of(
						// Sorted alphabetically key -> value
						new JobValue(null, "hello", "friend", 1),
						new JobValue(null, "hello", "there", 2)),
				observed);

		// Using a different key also creates a new entry.
		success = valueDao.insert("stop", "there", dummyJob);
		assertTrue(success);

		observed = valueDao.getAll();
		assertNotNull(observed);
		assertEquals(
				List.of(
						// Sorted alphabetically key -> value
						new JobValue(null, "hello", "friend", 1),
						new JobValue(null, "hello", "there", 2),
						new JobValue(null, "stop", "there", 1)),
				observed);
	}

	@Test
	public void testGetByUser() {
		Map<String, List<JobValue>> observed;
		boolean success;

		// First we need two distinct users recorded on the DB.

		User alessandro = new User();
		alessandro.setUsername("alessandro");
		alessandro.setPassword(HashUtil.hashPassword("AleAlessandro1234$%^}"));

		success = userDao.insert(alessandro);
		assertTrue(success);

		User bianca = new User();
		bianca.setUsername("bianca");
		bianca.setPassword(HashUtil.hashPassword("BiancaNeve9876^%${"));

		success = userDao.insert(bianca);
		assertTrue(success);

		// Now we need a dummy job per user recorded on the DB.

		CloudgeneJob alessandroJob = new CloudgeneJob();
		alessandroJob.setId("alessandro-job");
		alessandroJob.setUser(alessandro);

		success = jobDao.insert(alessandroJob);
		assertTrue(success);

		CloudgeneJob biancaJob = new CloudgeneJob();
		biancaJob.setId("bianca-job");
		biancaJob.setUser(bianca);

		success = jobDao.insert(biancaJob);
		assertTrue(success);

		// Finally we need some job values per user in the DB.

		success = valueDao.insert("nickname", "Alex", alessandroJob);
		assertTrue(success);
		success = valueDao.insert("nickname", "Alex", alessandroJob);
		assertTrue(success);
		success = valueDao.insert("nickname", "Xander", alessandroJob);
		assertTrue(success);

		success = valueDao.insert("apple", "green", biancaJob);
		assertTrue(success);
		success = valueDao.insert("apple", "green", biancaJob);
		assertTrue(success);
		success = valueDao.insert("apple", "green", biancaJob);
		assertTrue(success);
		success = valueDao.insert("apple", "green", biancaJob);
		assertTrue(success);
		success = valueDao.insert("apple", "red", biancaJob);
		assertTrue(success);
		success = valueDao.insert("apple", "red", biancaJob);
		assertTrue(success);
		success = valueDao.insert("mandarin", "orange", biancaJob);
		assertTrue(success);

		// Now we can check that we only read the data from one user.

		observed = valueDao.getByUser(alessandro);
		assertEquals(
				Map.of("unassigned", List.of(
						new JobValue("unassigned", "nickname", "Alex", 2),
						new JobValue("unassigned", "nickname", "Xander", 1))),
				observed);

		observed = valueDao.getByUser(bianca);
		assertEquals(
				Map.of("unassigned", List.of(
						new JobValue("unassigned", "apple", "green", 4),
						new JobValue("unassigned", "apple", "red", 2),
						new JobValue("unassigned", "mandarin", "orange", 1))),
				observed);
	}
}
