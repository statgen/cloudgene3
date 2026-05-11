package cloudgene.mapred.database.dao;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.JobValue;
import io.micronaut.core.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JobValueDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(JobValueDao.class);

	public JobValueDao(Database database) {
		super(database);
	}

	public boolean insert(@NonNull String name, @NonNull String value, @NonNull AbstractJob job) {
		String sql = "INSERT INTO job_values (name, job_id, `value`) VALUES (?,?,?)";

		try {
			Object[] params = new Object[3];
			params[0] = name;
			params[1] = job.getId();
			params[2] = value;

			update(sql, params);

			log.debug("insert value successful.");
			return true;
		} catch (SQLException e) {
			log.error("insert value failed.", e);
			return false;
		}
	}

	@NonNull
	public List<JobValue> getAll() {
		String sql = "SELECT name, `value`, COUNT(*) AS n FROM job_values "
				+ "GROUP BY name, `value` ORDER BY name, `value`";

		try {
			List<JobValue> result = query(sql, new ValueMapper());
			log.debug("Find all values successful. results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("Find all values failed", e);
			return new ArrayList<>(); // TODO(Marc): This is inconsistent with JobDao. There, we return null
		}
	}

	@NonNull
	public List<JobValue> getByUser(@NonNull User user) {
		String sql = "SELECT job_values.name AS `name`, job_values.`value` AS `value`, COUNT(*) AS n "
				+ "FROM job_values INNER JOIN job ON job_values.job_id = job.id "
				+ "WHERE job.user_id = ? GROUP BY job_values.name, job_values.`value` "
				+ "ORDER BY job_values.name, job_values.`value`";

		try {
			Object[] params = new Object[1];
			params[0] = user.getId();

			List<JobValue> result = query(sql, params, new ValueMapper());
			log.debug("Find values by user successful. results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("Find values by user failed", e);
			return new ArrayList<>();
		}
	}

	static class ValueMapper implements IRowMapper<JobValue> {
		@Override
		public JobValue mapRow(ResultSet rs, int row) throws SQLException {
			String name = rs.getString("name");
			String value = rs.getString("value");
			int count = rs.getInt("n");

			return new JobValue(name, value, count);
		}
	}
}
