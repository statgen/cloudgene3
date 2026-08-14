package cloudgene.mapred.database.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.util.IRowMapper;
import io.micronaut.core.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.jobs.AbstractJob;

public class CounterDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(CounterDao.class);

	public CounterDao(Database database) {
		super(database);
	}

	public boolean insert(String name, long value, AbstractJob job) {
		try {
			update(
					"INSERT INTO counters (name, job_id, `value`) VALUES (?,?,?)",
					name, job.getId(), value);

			log.debug("insert counter successful.");
			return true;
		} catch (SQLException e) {
			log.error("insert counter failed.", e);
			return false;
		}
	}

	/**
	 * Aggregates all counters by name and returns their sum as a map
	 * {@code (counter name) -> (total sum)}
	 * <p>
	 * Note that this will happily aggregate across different apps.
	 */
	@NonNull
	public Map<String, Long> getSum() {
		String sql = "SELECT name, SUM(`value`) FROM counters GROUP BY name";

		try {
			Map<String, Long> result = queryForMap(sql, new CounterMapper());
			log.debug("find sum of counters successful. results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("find sum of counters failed", e);
			return new HashMap<>();
		}
	}

	/**
	 * Gets all counter entries, aggregated by application and counter name.
	 * <p>
	 * Returns a map {@code (application ID without version) -> (counter name) -> stats}.
	 * See {@link CounterDao.Stats}
	 */
	@NonNull
	public Map<String, Map<String, Stats>> getAll() {
		String sql = "SELECT "
				+     "COALESCE(vals.application, 'unassigned') AS application, "
				+     "counters.name AS name, "
				+     "COUNT(counters.`value`) AS count, "
				+     "SUM(counters.`value`) AS total, "
				+     "AVG(counters.`value`) AS mean "
				+ "FROM "
				+     "counters "
				+     "INNER JOIN job ON counters.job_id = job.id "
				+     "LEFT JOIN ( "
				+         "SELECT "
				+             "job_values.job_id AS job_id, "
				+             "job_values.`value` AS application "
				+         "FROM "
				+             "job_values "
				+             "INNER JOIN job ON job_values.job_id = job.id "
				+         "WHERE "
				+             "job_values.name = 'application' "
				+     ") vals ON vals.job_id = job.id "
				+ "GROUP BY application, name "
				+ "ORDER BY application, name";

		try {
			List<Stats> result = query(sql, new CounterStatsMapper());

			// application -> counter -> entries
			Map<String, Map<String, Stats>> output = result.stream()
					.collect(Collectors.groupingBy(
							Stats::application,
							LinkedHashMap::new,
							Collectors.toMap(
									Stats::name,
									Function.identity())));

			log.debug("Find all counters successful. Results: {}", result);
			return output;
		} catch (SQLException e) {
			log.error("Find all counters failed", e);
			return new HashMap<>();
		}
	}

	/**
	 * Gets the counter entries associated with {@code user}, aggregated by
	 * application and counter name.
	 * <p>
	 * Returns a map {@code (application ID without version) -> (counter name) -> stats}.
	 * See {@link CounterDao.Stats}
	 */
	@NonNull
	public Map<String, Map<String, Stats>> getByUser(@NonNull User user) {
		String sql = "SELECT "
				+     "COALESCE(vals.application, 'unassigned') AS application, "
				+     "counters.name AS name, "
				+     "COUNT(counters.`value`) AS count, "
				+     "SUM(counters.`value`) AS total, "
				+     "AVG(counters.`value`) AS mean "
				+ "FROM "
				+     "counters "
				+     "INNER JOIN job ON counters.job_id = job.id "
				+     "LEFT JOIN ( "
				+         "SELECT "
				+             "job_values.job_id AS job_id, "
				+             "job_values.`value` AS application "
				+         "FROM "
				+             "job_values "
				+             "INNER JOIN job ON job_values.job_id = job.id "
				+         "WHERE "
				+             "job_values.name = 'application' "
				+     ") vals ON vals.job_id = job.id "
				+ "WHERE job.user_id = ? "
				+ "GROUP BY application, name "
				+ "ORDER BY application, name";

		try {
			Object[] params = new Object[1];
			params[0] = user.getId();
			List<Stats> result = query(sql, params, new CounterStatsMapper());

			// application -> counter -> entries
			Map<String, Map<String, Stats>> output = result.stream()
					.collect(Collectors.groupingBy(
							Stats::application,
							LinkedHashMap::new,
							Collectors.toMap(
									Stats::name,
									Function.identity())));

			log.debug("Find counters by user successful. Results: {}", result);
			return output;
		} catch (SQLException e) {
			log.error("Find counters by user failed", e);
			return new HashMap<>();
		}
	}

	@NonNull
	public Map<String, Map<String, List<HistoryEntry>>> getHistoryByUser(@NonNull User user) {
		String sql = "SELECT "
				+     "COALESCE(vals.application, 'unassigned') AS application, "
				+     "counters.name AS name, "
				+     "job.finished_on AS time, "
				+     "SUM(counters.`value`) OVER ( "
				+         "PARTITION BY counters.name "
				+         "ORDER BY job.finished_on "
				+         "ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW "
				+     ") AS `value` "
				+ "FROM "
				+     "counters "
				+     "INNER JOIN job ON counters.job_id = job.id "
				+     "LEFT JOIN ( "
				+         "SELECT "
				+             "job_values.job_id AS job_id, "
				+             "job_values.`value` AS application "
				+         "FROM "
				+             "job_values "
				+             "INNER JOIN job ON job_values.job_id = job.id "
				+         "WHERE "
				+             "job_values.name = 'application' "
				+     ") vals ON vals.job_id = job.id "
				+ "WHERE "
				+     "job.user_id = ? AND "
				+     "job.finished_on > 0 "
				+ "ORDER BY application, name, time";

		try {
			Object[] params = new Object[1];
			params[0] = user.getId();
			List<HistoryEntry> result = query(sql, params, new CounterHistoryMapper());

			// application -> counter -> entries
			Map<String, Map<String, List<HistoryEntry>>> output = result.stream()
					.collect(Collectors.groupingBy(
							HistoryEntry::application,
							LinkedHashMap::new,
							Collectors.groupingBy(
									HistoryEntry::counter,
									LinkedHashMap::new,
									Collectors.toList())));

			log.debug("Get counter history by user successful.");
			return output;
		} catch (SQLException e) {
			log.error("Get counter history by user failed", e);
			return new HashMap<>();
		}
	}

	/**
	 * Aggregated counter statistics: {@code application} ID (without version),
	 * counter {@code name}, entry {@code count}, {@code total} sum, {@code mean}.
	 */
	public record Stats(String application, String name, long count, long total, double mean) {}

	public record HistoryEntry(String application, String counter, Instant time, long value) {}

	static class CounterMapper implements IRowMapMapper<String, Long> {
		@Override
		public String getRowKey(ResultSet rs, int row) throws SQLException {
			return rs.getString(1);
		}

		@Override
		public Long getRowValue(ResultSet rs, int row) throws SQLException {
			return rs.getLong(2);
		}
	}

	static class CounterStatsMapper implements IRowMapper<Stats> {
		@Override
		public Stats mapRow(ResultSet rs, int row) throws SQLException {
			String application = rs.getString("application");
			String name = rs.getString("name");
			long count = rs.getLong("count");
			long total = rs.getLong("total");
			double mean = rs.getDouble("mean");

			return new Stats(application, name, count, total, mean);
		}
	}

	static class CounterHistoryMapper implements IRowMapper<HistoryEntry> {
		@Override
		public HistoryEntry mapRow(ResultSet rs, int row) throws SQLException {
			String application = rs.getString("application");
			String name = rs.getString("name");
			Instant time = Instant.ofEpochMilli(rs.getLong("time"));
			long value = rs.getLong("value");

			return new HistoryEntry(application, name, time, value);
		}
	}
}
