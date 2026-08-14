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
import java.util.*;
import java.util.stream.Collectors;

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
		String sql = "SELECT name, `value`, COUNT(*) AS count "
				+ "FROM job_values "
				+ "GROUP BY name, `value` "
				+ "ORDER BY name, `value`";

		try {
			List<JobValue> result = query(sql, new BasicValueMapper());
			log.debug("Find all values successful. results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("Find all values failed", e);
			return new ArrayList<>(); // TODO(Marc): This is inconsistent with JobDao. There, we return null
		}
	}

	@NonNull
	public Map<String, List<JobValue>> getAllGrouped() {
		String sql = "SELECT "
				+ "    COALESCE(app.`value`, 'unassigned') AS application, "
				+ "    vals.name AS `name`, "
				+ "    vals.`value` AS `value`, "
				+ "    COUNT(*) AS count "
				+ " FROM "
				+ "    job_values AS vals "
				+ "LEFT JOIN "
				+ "    job_values AS app "
				+ "ON "
				+ "    app.job_id = vals.job_id AND "
				+ "    app.name = 'application' "
				+ "WHERE "
				+ "    vals.name <> 'application' "
				+ "GROUP BY application, name, `value` "
				+ "ORDER BY application, name, `value`";

		try {
			List<JobValue> result = query(sql, new ExtendedValueMapper());

			Map<String, List<JobValue>> output = result.stream()
					.collect(Collectors.groupingBy(
							JobValue::application,
							LinkedHashMap::new,
							Collectors.toList()));

			log.debug("Find grouped values successful. results: {}", result);
			return output;
		} catch (SQLException e) {
			log.error("Find grouped values failed", e);
			return new LinkedHashMap<>();
		}
	}

	@NonNull
	public Map<String, List<JobValue>> getByUser(@NonNull User user) {
		String sql = "SELECT "
				+     "COALESCE(vals.application, 'unassigned') AS application, "
				+     "job_values.name AS `name`, "
				+     "job_values.`value` AS `value`, "
				+     "COUNT(*) AS count "
				+ "FROM "
				+     "job_values "
				+     "INNER JOIN job ON job_values.job_id = job.id "
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
				+     "job_values.name <> 'application' "
				+ "GROUP BY application, name, `value` "
				+ "ORDER BY application, name, `value`";

		try {
			Object[] params = new Object[1];
			params[0] = user.getId();

			List<JobValue> result = query(sql, params, new ExtendedValueMapper());

			Map<String, List<JobValue>> output = result.stream()
					.collect(Collectors.groupingBy(
							JobValue::application,
							LinkedHashMap::new,
							Collectors.toList()));

			log.debug("Find values by user successful. results: {}", result);
			return output;
		} catch (SQLException e) {
			log.error("Find values by user failed", e);
			return new LinkedHashMap<>();
		}
	}

	static class BasicValueMapper implements IRowMapper<JobValue> {
		@Override
		public JobValue mapRow(ResultSet rs, int row) throws SQLException {
			String name = rs.getString("name");
			String value = rs.getString("value");
			int count = rs.getInt("count");

			return new JobValue(null, name, value, count);
		}
	}

	static class ExtendedValueMapper implements IRowMapper<JobValue> {
		@Override
		public JobValue mapRow(ResultSet rs, int row) throws SQLException {
			String application = rs.getString("application");
			String name = rs.getString("name");
			String value = rs.getString("value");
			int count = rs.getInt("count");

			return new JobValue(application, name, value, count);
		}
	}
}
