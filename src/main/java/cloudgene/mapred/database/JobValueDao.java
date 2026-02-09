package cloudgene.mapred.database;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.jobs.AbstractJob;
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

	public boolean insert(String name, String value, AbstractJob job) {
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

	public List<JobValue> getAll() {
<<<<<<< HEAD
		String sql = "SELECT name, `value`, COUNT(*) AS n FROM job_values "
				+ "GROUP BY name, `value` ORDER BY name, `value`";

		try {
			List<JobValue> result = query(sql, new ValueMapper());
=======

		StringBuilder sql = new StringBuilder();
		sql.append("select name, `value`, count(*) as n ");
		sql.append("from job_values ");
		sql.append("group by name, `value` ");
		sql.append("order by name, `value` ");

		try {
			List<JobValue> result = query(sql.toString(), new ValueMapper());

>>>>>>> origin/statgen-custom-changes
			log.debug("find counters successful. results: " + result);
			return result;
		} catch (SQLException e) {
			log.error("find all counters failed", e);
			return new ArrayList<>(); // TODO(Marc): This is inconsistent with JobDao. There, we return null
		}
<<<<<<< HEAD
=======

		return new ArrayList<>();
>>>>>>> origin/statgen-custom-changes
	}

	public static class JobValue {

		private String name;
		private String value;
		private int count;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public int getCount() {
			return count;
		}
	}

<<<<<<< HEAD
	static class ValueMapper implements IRowMapper {

=======
	static class ValueMapper implements IRowMapper<JobValue> {
>>>>>>> origin/statgen-custom-changes
		@Override
		public JobValue mapRow(ResultSet rs, int row) throws SQLException {
			JobValue jobValue = new JobValue();

			jobValue.setName(rs.getString("name"));
			jobValue.setValue(rs.getString("value"));
			jobValue.setCount(rs.getInt("n"));

			return jobValue;
		}
	}
}
