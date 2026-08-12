package cloudgene.mapred.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.JdbcDataAccessObject;

public class CounterHistoryDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(CounterHistoryDao.class);

	public record Entry(Instant timestamp, Map<String, Long> counters) {}

	public CounterHistoryDao(Database database) {
		super(database);
	}

	public boolean insert(long timestamp, String name, long value) {
		String sql = "INSERT INTO counters_history (time_stamp, name, `value`) VALUES (?,?,?)";

		try {
			Object[] params = new Object[3];
			params[0] = timestamp;
			params[1] = name;
			params[2] = value;

			update(sql, params);
			log.debug("insert counter history successful.");
			return true;
		} catch (SQLException e) {
			log.error("insert counter history failed.", e);
			return false;
		}
	}

	public List<Entry> getAll() {
		String sql = "SELECT time_stamp, name, `value` FROM counters_history "
				+ "ORDER BY time_stamp DESC, name";

		try {
			List<Entry> result = new ArrayList<>();
			Instant timestamp = null;
			Map<String, Long> counters = null;

			try (Connection connection = database.getDataSource().getConnection();
					PreparedStatement statement = connection.prepareStatement(sql)) {

				try (ResultSet rs = statement.executeQuery()) {

					// NOTE(Marc): This whole thing is convoluted, so I'm leaving some notes behind.
					//
					// This method grabs the last `limit` entries in `counters_history`, which has
					// time series of the total value of each tracked counter (counters sum up
					// values from all jobs).
					//
					// It then iterates over the list, and merges data with the same timestamp
					// (allegedly different counters at the same sample point) into a single
					// `counters` object.
					//
					// So basically we're going from long form to wide form.
					//
					// On failure it returns an empty collection, which is different behavior from
					// other DAOs (they return `null` even if we're querying a collection).

					while (rs.next()) {
						long newMillis = rs.getLong("time_stamp");
						Instant newTime = Instant.ofEpochMilli(newMillis);

						if (!Objects.equals(timestamp, newTime)) {
							if (timestamp != null && counters != null) {
								result.add(new Entry(timestamp, counters));
							}

							timestamp = newTime;
							counters = new HashMap<>();
						}

						String name = rs.getString("name");
						Long value = rs.getLong("value");
						counters.put(name, value);
					}

					if (timestamp != null && counters != null) {
						result.add(new Entry(timestamp, counters));
					}
				}
			}

			log.debug("find full counter history successful. Results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find full counter history failed", e);
			return new ArrayList<>();
		}
	}

	public List<Entry> getAllBetween(Instant start, Instant end) {
		String sql = "SELECT time_stamp, name, `value` FROM counters_history "
				+ "WHERE time_stamp > ? AND time_stamp < ? "
				+ "ORDER BY time_stamp DESC, name";

		try {
			List<Entry> result = new ArrayList<>();
			Instant timestamp = null;
			Map<String, Long> counters = null;

			try (Connection connection = database.getDataSource().getConnection();
					PreparedStatement statement = connection.prepareStatement(sql)) {

				statement.setLong(1, start.toEpochMilli());
				statement.setLong(2, end.toEpochMilli());

				try (ResultSet rs = statement.executeQuery()) {
					// NOTE(Marc): See explainer in the other method.

					while (rs.next()) {
						long newMillis = rs.getLong("time_stamp");
						Instant newTime = Instant.ofEpochMilli(newMillis);

						if (!Objects.equals(timestamp, newTime)) {
							if (timestamp != null && counters != null) {
								result.add(new Entry(timestamp, counters));
							}

							timestamp = newTime;
							counters = new HashMap<>();
						}

						String name = rs.getString("name");
						Long value = rs.getLong("value");
						counters.put(name, value);
					}

					if (timestamp != null && counters != null) {
						result.add(new Entry(timestamp, counters));
					}
				}
			}

			log.debug("find counter history between times successful. Results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find counter history between times failed", e);
			return new ArrayList<>();
		}
	}
}
