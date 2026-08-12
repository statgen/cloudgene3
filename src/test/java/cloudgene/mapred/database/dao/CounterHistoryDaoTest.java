package cloudgene.mapred.database.dao;

import cloudgene.mapred.TestApplication;
import cloudgene.mapred.database.util.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CounterHistoryDaoTest {

	private CounterHistoryDao dao;

	@BeforeEach
	public void setup() throws Exception {
		TestApplication application = new TestApplication();
		Database db = application.getDatabase();
		dao = new CounterHistoryDao(db);
	}

	@Test
	public void testInsertAndGetAll() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		Instant t1 = now.minus(15, ChronoUnit.MINUTES);
		dao.insert(t1.toEpochMilli(), "runningJobs", 8L);

		List<CounterHistoryDao.Entry> entries = dao.getAll();
		assertEquals(1, entries.size());

		CounterHistoryDao.Entry e00 = entries.get(0);
		assertEquals(t1, e00.timestamp());
		assertEquals(1, e00.counters().size());
		assertTrue(e00.counters().containsKey("runningJobs"));
		assertEquals(8L, e00.counters().get("runningJobs"));

		Instant t2 = now;
		dao.insert(t2.toEpochMilli(), "runningJobs", 5L);
		dao.insert(t2.toEpochMilli(), "completeJobs", 25L);
		dao.insert(t2.toEpochMilli(), "users", 67L);

		entries = dao.getAll();
		assertEquals(2, entries.size());

		// Entries are returned latest to newest.

		CounterHistoryDao.Entry e10 = entries.get(0);
		assertEquals(t2, e10.timestamp());
		assertEquals(3, e10.counters().size());
		assertTrue(e10.counters().containsKey("runningJobs"));
		assertEquals(5L, e10.counters().get("runningJobs"));
		assertTrue(e10.counters().containsKey("completeJobs"));
		assertEquals(25L, e10.counters().get("completeJobs"));
		assertTrue(e10.counters().containsKey("users"));
		assertEquals(67L, e10.counters().get("users"));

		CounterHistoryDao.Entry e11 = entries.get(1);
		assertEquals(e00, e11);
	}

	@Test
	public void testGetAllBetween() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		Instant t0 = now.minus(40, ChronoUnit.MINUTES);

		Instant t1 = now.minus(30, ChronoUnit.MINUTES);
		dao.insert(t1.toEpochMilli(), "runningJobs", 7L);
		dao.insert(t1.toEpochMilli(), "completeJobs", 25L);
		dao.insert(t1.toEpochMilli(), "users", 61L);

		Instant t2 = now.minus(25, ChronoUnit.MINUTES);

		Instant t3 = now.minus(20, ChronoUnit.MINUTES);
		dao.insert(t3.toEpochMilli(), "runningJobs", 5L);
		dao.insert(t3.toEpochMilli(), "completeJobs", 30L);
		dao.insert(t3.toEpochMilli(), "users", 62L);

		Instant t4 = now.minus(12, ChronoUnit.MINUTES);

		Instant t5 = now.minus(10, ChronoUnit.MINUTES);
		dao.insert(t5.toEpochMilli(), "runningJobs", 10L);
		dao.insert(t5.toEpochMilli(), "completeJobs", 34L);
		dao.insert(t5.toEpochMilli(), "users", 63L);

		Instant t6 = now.minus(4, ChronoUnit.MINUTES);

		// Sorted latest to oldest.
		List<CounterHistoryDao.Entry> expected = List.of(
				new CounterHistoryDao.Entry(t5, Map.of(
						"runningJobs", 10L,
						"completeJobs", 34L,
						"users", 63L)),
				new CounterHistoryDao.Entry(t3, Map.of(
						"runningJobs", 5L,
						"completeJobs", 30L,
						"users", 62L)),
				new CounterHistoryDao.Entry(t1, Map.of(
						"runningJobs", 7L,
						"completeJobs", 25L,
						"users", 61L)));

		// All entries lie between t0 and now.
		List<CounterHistoryDao.Entry> entries = dao.getAllBetween(t0, now);
		assertEquals(expected, entries);

		// No entries after t6.
		entries = dao.getAllBetween(t6, now);
		assertEquals(List.of(), entries);

		// earliest entry lies between t0 and t2.
		entries = dao.getAllBetween(t0, t2);
		assertEquals(expected.subList(2, 3), entries);

		// Two later entries lie between t2 and t6.
		entries = dao.getAllBetween(t2, t6);
		assertEquals(expected.subList(0, 2), entries);
	}
}
