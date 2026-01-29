package cloudgene.mapred.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.JdbcDataAccessObject;

public class CounterHistoryDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(CounterHistoryDao.class);

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy-MM-dd HH:mm");

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

	public List<Map<String, String>> getAll(int limit) {
		String sql = "SELECT time_stamp, name, `value` "
				+ "FROM counters_history "
				+ "ORDER BY time_stamp DESC, name "
				+ "LIMIT ?";

		try {
			List<Map<String, String>> result = new ArrayList<>();
			Map<String, String> counters = new HashMap<>();
			String old = "";

			Connection connection = database.getDataSource().getConnection();

			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setInt(1, limit);

			ResultSet rs = statement.executeQuery();

			// NOTE(Marc): This whole thing is convoluted, so I'm leaving some notes behind.
			//
			// This method grabs the last `limit` entries in `counters_history`, which has
			// time series of the total value of each tracked counter (counters sum up values
			// from all jobs).
			//
			// It then iterates over the list, and merges data with the same timestamp
			// (allegedly different counters at the same sample point) into a single `counters`
			// object.
			//
			// So basically we're going from long form to wide form.
			//
			// On failure it returns an empty collection, which is different behavior from
			// other DAOs (they return `null` even if we're querying a collection).

			while (rs.next()) {
				String timestamp = rs.getString(1);
				if (!old.equals(timestamp)) {
					counters = new HashMap<>();
					result.add(counters);

					Date date = new Date(rs.getLong(1));
					counters.put("timestamp", DATE_FORMAT.format(date));

					old = rs.getString(1);
				}

				String name = rs.getString(2);
				String value = rs.getString(3);
				counters.put(name, value);
			}

			rs.close();
			connection.close();

			log.debug("find counter history successful. results: " + result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all counter history failed", e);
			return new ArrayList<>();
		}
	}

	public List<Map<String, String>> getAllBeetween(long start, long end) {
		String sql = "SELECT time_stamp, name, `value` FROM counters_history "
				+ "WHERE time_stamp > ? AND time_stamp < ? ORDER BY time_stamp DESC, name";

		try {
			List<Map<String, String>> result = new ArrayList<>();
			Map<String, String> counters = new HashMap<>();
			String old = "";

			Connection connection = database.getDataSource().getConnection();

			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setLong(1, start);
			statement.setLong(2, end);

			ResultSet rs = statement.executeQuery();

			while (rs.next()) {
				// NOTE(Marc): See explainer in the other method.

				if (!old.equals(rs.getString(1))) {
					counters = new HashMap<>();
					result.add(counters);
					counters.put("timestamp",
							DATE_FORMAT.format(new Date(rs.getLong(1))));
					old = rs.getString(1);
				}
				counters.put(rs.getString(2), rs.getString(3));
			}

			rs.close();
			connection.close();

			log.debug("find counter history successful. results: " + result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all counter history failed", e);
			return new ArrayList<>();
		}
	}
}
