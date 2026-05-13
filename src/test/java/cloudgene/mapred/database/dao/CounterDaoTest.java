package cloudgene.mapred.database.dao;

import cloudgene.mapred.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CounterDaoTest {

	private UserDao userDao;
	private JobDao jobDao;
	private CounterDao counterDao;

	@BeforeEach
	public void setup() throws Exception {
		TestApplication application = new TestApplication();
		Database db = application.getDatabase();

		userDao = new UserDao(db);
		jobDao = new JobDao(db);
		counterDao = new CounterDao(db);
	}

	@Test
	public void testInsertAndGetAll() {
		Map<String, Long> observed;
		boolean success;

		CloudgeneJob dummyJob = new CloudgeneJob();
		dummyJob.setId("DummyJob-CounterDaoTest-testInsertAndGetAll");

		// Counter DAO starts empty.
		observed = counterDao.getAll();
		assertNotNull(observed);
		assertEquals(Map.of(), observed);

		// On first insert, we only see the same key-value pair again.
		success = counterDao.insert("hawk-sightings", 2L, dummyJob);
		assertTrue(success);

		observed = counterDao.getAll();
		assertNotNull(observed);
		assertEquals(Map.of("hawk-sightings", 2L), observed);

		// Same if we add a different key: we see both keys and their original values.
		success = counterDao.insert("early-lunches", 1L, dummyJob);
		assertTrue(success);

		observed = counterDao.getAll();
		assertNotNull(observed);
		assertEquals(
				Map.of(
						"hawk-sightings", 2L,
						"early-lunches", 1L),
				observed);

		// If we add again to an existing key, we get the SUM of all inserted values.
		success = counterDao.insert("hawk-sightings", 3L, dummyJob);
		assertTrue(success);

		observed = counterDao.getAll();
		assertNotNull(observed);
		assertEquals(
				Map.of(
						"hawk-sightings", 5L,
						"early-lunches", 1L),
				observed);
	}

	@Test
	public void testGetByUser() {
		Map<String, Map<String, CounterDao.Stats>> observed;
		boolean success;

		// First we need two distinct users recorded on the DB.

		User alice = new User();
		alice.setUsername("alice");
		alice.setPassword(HashUtil.hashPassword("AliceAlice123$%^"));

		success = userDao.insert(alice);
		assertTrue(success);

		User bob = new User();
		bob.setUsername("bob");
		bob.setPassword(HashUtil.hashPassword("BobBob987^%$"));

		success = userDao.insert(bob);
		assertTrue(success);

		// Now we need a dummy job per user recorded on the DB.

		CloudgeneJob aliceJob = new CloudgeneJob();
		aliceJob.setId("alice-job");
		aliceJob.setUser(alice);

		success = jobDao.insert(aliceJob);
		assertTrue(success);

		CloudgeneJob bobJob = new CloudgeneJob();
		bobJob.setId("bob-job");
		bobJob.setUser(bob);

		success = jobDao.insert(bobJob);
		assertTrue(success);

		// Finally we need some counters per user in the DB.

		success = counterDao.insert("alice", 1, aliceJob);
		assertTrue(success);
		success = counterDao.insert("both", 2, aliceJob);
		assertTrue(success);

		success = counterDao.insert("bob", 3, bobJob);
		assertTrue(success);
		success = counterDao.insert("both", 4, bobJob);
		assertTrue(success);

		// Now we can check that we only read the data from one user.

		observed = counterDao.getByUser(alice);
		assertEquals(
				Map.of("unassigned", Map.of(
						"alice", new CounterDao.Stats("unassigned", "alice", 1L, 1.0D),
						"both", new CounterDao.Stats("unassigned", "both", 2L, 2.0D))),
				observed);

		observed = counterDao.getByUser(bob);
		assertEquals(
				Map.of("unassigned", Map.of(
						"bob", new CounterDao.Stats("unassigned", "bob", 3L, 3.0D),
						"both", new CounterDao.Stats("unassigned", "both", 4L, 4.0D))),
				observed);

		// We're returning sums and means over the names:

		success = counterDao.insert("alice", 5, aliceJob);
		assertTrue(success);

		success = counterDao.insert("alice", 6, aliceJob);
		assertTrue(success);

		success = counterDao.insert("both", 7, aliceJob);
		assertTrue(success);

		success = counterDao.insert("bob", 8, bobJob);
		assertTrue(success);

		success = counterDao.insert("both", 9, bobJob);
		assertTrue(success);

		observed = counterDao.getByUser(alice);
		assertEquals(
				Map.of("unassigned", Map.of(
						"alice", new CounterDao.Stats("unassigned", "alice", 12L, 4.0D),
						"both", new CounterDao.Stats("unassigned", "both", 9L, 4.5D))),
				observed);

		observed = counterDao.getByUser(bob);
		assertEquals(
				Map.of("unassigned", Map.of(
						"bob", new CounterDao.Stats("unassigned", "bob", 11L, 5.5D),
						"both", new CounterDao.Stats("unassigned", "both", 13L, 6.5D))),
				observed);
	}
}
