package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

	public boolean insert(String name, int value, AbstractJob job) {
		String sql = "INSERT INTO counters (name, job_id, `value`) VALUES (?,?,?)";

		try {
			Object[] params = new Object[3];
			params[0] = name;
			params[1] = job.getId();
			params[2] = value;

			update(sql, params);
			log.debug("insert counter successful.");
			return true;
		} catch (SQLException e) {
			log.error("insert counter failed.", e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Long> getAll() {
		String sql = "SELECT name, SUM(`value`) FROM counters GROUP BY name";

		try {
			Map<String, Long> result = queryForMap(sql, new CounterMapper());
			log.debug("find counters successful. results: " + result);
			return result;
		} catch (SQLException e) {
			log.error("find all counters failed", e);
			return new HashMap<>();
		}
	}

	static class CounterMapper implements IRowMapMapper {

		@Override
		public Object getRowKey(ResultSet rs, int row) throws SQLException {
			return rs.getString(1);
		}

		@Override
		public Object getRowValue(ResultSet rs, int row) throws SQLException {
			return rs.getLong(2);
		}
	}
}
