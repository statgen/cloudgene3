package cloudgene.mapred.database.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import cloudgene.mapred.core.User;
import jakarta.validation.constraints.NotNull;
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

	@NotNull
	public Map<String, Long> getAll() {
		String sql = "SELECT name, SUM(`value`) FROM counters GROUP BY name";

		try {
			Map<String, Long> result = queryForMap(sql, new CounterMapper());
			log.debug("find all counters successful. results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("find all counters failed", e);
			return new HashMap<>();
		}
	}

	@NotNull
	public Map<String, Long> getByUser(User user) {
		String sql = "SELECT counters.name AS name, SUM(counters.`value`) AS `value` "
				+ "FROM counters INNER JOIN job ON counters.job_id = job.id "
				+ "WHERE job.user_id = ? "
				+ "GROUP BY counters.name";

		try {
			Map<String, Long> result = queryForMap(sql, new CounterMapper(), user.getId());
			log.debug("Find counters by user successful. Results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("Find counters by user failed", e);
			return new HashMap<>();
		}
	}

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
}
