package cloudgene.mapred.database.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import cloudgene.mapred.jobs.*;
import cloudgene.mapred.jobs.state.JobState;
import cloudgene.mapred.jobs.state.CompletionState;
import cloudgene.mapred.jobs.state.NotificationState;
import cloudgene.mapred.jobs.state.SuccessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao.UserMapper;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;

public class JobDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(JobDao.class);

	public JobDao(Database database) {
		super(database);
	}

	public boolean insert(AbstractJob job) {
		String sql = "INSERT INTO job "
				+ "(id, name, state, start_time, end_time, user_id, s3_url, type, application, "
				+ "application_id, submitted_on, finished_on, setup_start_time, setup_end_time, "
				+ "completion_state, success_state, notification_state, user_agent) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		CompletionState completion = job.getCompletionState();
		SuccessState success = job.getSuccessState();
		NotificationState notification = job.getNotificationState();

		try {
			Object[] params = new Object[18];
			params[0] = job.getId();
			params[1] = job.getName();
			params[2] = job.getState().getValue();
			params[3] = job.getStartTime();
			params[4] = job.getEndTime();
			params[5] = job.getUser().getId();
			params[6] = "";
			params[7] = -1;
			params[8] = job.getApplication();
			params[9] = job.getApplicationId();
			params[10] = job.getSubmittedOn();
			params[11] = job.getEndTime();
			params[12] = -1;
			params[13] = -1;
			params[14] = (completion == null) ? null : completion.getValue();
			params[15] = (success == null) ? null : success.getValue();
			params[16] = (notification == null) ? null : notification.getValue();
			params[17] = trimToLength(job.getUserAgent(), 350);

			update(sql, params);

			log.debug("insert job '{}' successful.", job.getId());
			return true;
		} catch (SQLException e) {
			log.error("insert job '{}' failed.", job.getId(), e);
			return false;
		}
	}

	public boolean update(AbstractJob job) {
		String sql = "UPDATE job SET "
				+ "name = ?, state = ?, start_time = ?, end_time = ?, user_id = ?, s3_url = ?, "
				+ "type = ?, deleted_on = ?, application = ?, application_id = ?, submitted_on = ?, "
				+ "finished_on = ?, setup_start_time = ?, setup_end_time = ?, completion_state = ?, "
				+ "success_state = ?, notification_state = ? "
				+ "WHERE id = ? ";

		CompletionState completion = job.getCompletionState();
		SuccessState success = job.getSuccessState();
		NotificationState notification = job.getNotificationState();

		try {
			Object[] params = new Object[18];
			params[0] = job.getName();
			params[1] = job.getState().getValue();
			params[2] = job.getStartTime();
			params[3] = job.getEndTime();
			params[4] = job.getUser().getId();
			params[5] = "";
			params[6] = -1;
			params[7] = job.getDeletedOn();
			params[8] = job.getApplication();
			params[9] = job.getApplicationId();
			params[10] = job.getSubmittedOn();
			params[11] = job.getEndTime();
			params[12] = -1;
			params[13] = -1;
			params[14] = (completion == null) ? null : completion.getValue();
			params[15] = (success == null) ? null : success.getValue();
			params[16] = (notification == null) ? null : notification.getValue();
			params[17] = job.getId();

			update(sql, params);

			log.debug("update job '{}' successful.", job.getId());
			return true;
		} catch (SQLException e) {
			log.error("update job '{}' failed", job.getId(), e);
			return false;
		}
	}

	public boolean updateUser(User oldUser, User newUser) {
		String sql = "UPDATE job SET user_id = ?, name = ? WHERE user_id = ?";

		try {
			Object[] params = new Object[3];
			params[0] = newUser.getId();
			params[1] = "";
			params[2] = oldUser.getId();

			update(sql, params);

			log.error("move all jobs from '{}' to '{}' successful.", oldUser.getUsername(), newUser.getUsername());
			return true;
		} catch (SQLException e) {
			log.error("move all jobs from '{}' to '{}' failed", oldUser.getUsername(), newUser.getUsername(), e);
			return false;
		}
	}

	public boolean delete(AbstractJob job) {
		String sql = "DELETE FROM job WHERE id = ?";

		try {
			Object[] params = new Object[1];
			params[0] = job.getId();

			update(sql, params);

			log.debug("delete job successful.");
			return true;
		} catch (SQLException e) {
			log.error("delete job failed", e);
			return false;
		}
	}

	public List<AbstractJob> findAllByUser(User user) {
		String sql = "SELECT * FROM job WHERE user_id = ? AND state != ? ORDER BY id DESC";

		Object[] params = new Object[2];
		params[0] = user.getId();
		params[1] = JobState.DELETED.getValue();

		try {
			List<AbstractJob> result = query(sql, params, new JobMapper());
			log.debug("find all jobs by user successful. results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all jobs by user failed", e);
			return null;
		}
	}

	public List<AbstractJob> findAllByUser(User user, int offset, int limit) {
		String sql = "SELECT * FROM job WHERE user_id = ? AND state != ? ORDER BY id DESC LIMIT ?,?";

		Object[] params = new Object[4];
		params[0] = user.getId();
		params[1] = JobState.DELETED.getValue();
		params[2] = offset;
		params[3] = limit;

		try {
			List<AbstractJob> result = query(sql, params, new JobMapper());
			log.debug("find all jobs by user (paged) successful. results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all jobs by user (paged) failed", e);
			return null;
		}
	}

	public int countAllByUser(User user) {
		String sql = "SELECT COUNT(*) FROM job WHERE user_id = ? AND state != ?";

		Object[] params = new Object[2];
		params[0] = user.getId();
		params[1] = JobState.DELETED.getValue();

		try {
			int result = queryForObject(sql, params, new IntegerMapper());
			log.debug("count all jobs by user successful. results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("count all jobs by user failed", e);
			return 0;
		}
	}

	public List<AbstractJob> findAll() {
		String sql = "SELECT * FROM job "
				+ "JOIN `user` ON job.user_id = `user`.id "
				+ "ORDER BY job.id ASC";

		try {
			List<AbstractJob> result = query(sql, new JobAndUserMapper());
			log.debug("find all jobs successful. results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all jobs failed", e);
			return null;
		}
	}

	public List<AbstractJob> findAllNotRetiredJobs() {
		String sql = "SELECT * FROM job "
				+ "JOIN `user` ON job.user_id = `user`.id "
				+ "WHERE state NOT IN (?,?,?,?,?,?,?) "
				+ "ORDER BY job.id DESC";

		// TODO(Marc): Looks like RETIRED and DELETED are duplicated for no reason.
		// Remove?
		Object[] params = new Object[7];
		params[0] = JobState.WAITING.getValue();
		params[1] = JobState.RUNNING.getValue();
		params[2] = JobState.EXPORTING.getValue();
		params[3] = JobState.RETIRED.getValue();
		params[4] = JobState.DELETED.getValue();
		params[5] = JobState.RETIRED.getValue();
		params[6] = JobState.DELETED.getValue();

		try {
			List<AbstractJob> result = query(sql, params, new JobAndUserMapper());
			log.debug("find all non-retired jobs successful. results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all non-retired jobs failed", e);
			return null;
		}
	}

	public List<AbstractJob> findAllNotNotifiedJobs() {
		String sql = "SELECT * FROM job "
				+ "JOIN `user` ON job.user_id = `user`.id "
				+ "WHERE state != ? AND state != ? AND state != ? AND state != ? "
				+ "ORDER BY job.id DESC";

		Object[] params = new Object[4];
		params[0] = JobState.RETIRED.getValue();
		params[1] = JobState.SUCCESS_AND_NOTIFICATION_SENT.getValue();
		params[2] = JobState.FAILED_AND_NOTIFICATION_SENT.getValue();
		params[3] = JobState.DELETED.getValue();

		try {
			List<AbstractJob> result = query(sql, params, new JobAndUserMapper());
			log.debug("find all non-notified jobs successful. results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all non-notified jobs failed", e);
			return null;
		}
	}

	public List<AbstractJob> findAllNotifiedJobs() {
		String sql = "SELECT * FROM job "
				+ "JOIN `user` ON job.user_id = `user`.id "
				+ "WHERE state = ? OR state = ? "
				+ "ORDER BY job.id DESC";

		Object[] params = new Object[2];
		params[0] = JobState.SUCCESS_AND_NOTIFICATION_SENT.getValue();
		params[1] = JobState.FAILED_AND_NOTIFICATION_SENT.getValue();

		try {
			List<AbstractJob> result = query(sql, params, new JobAndUserMapper());
			log.debug("find all notified jobs successful. results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all notified jobs failed", e);
			return null;
		}
	}

	public List<AbstractJob> findAllOlderThan(long time, JobState state) {
		String sql = "SELECT * FROM job "
				+ "JOIN `user` ON job.user_id = `user`.id "
				+ "WHERE state = ? AND finished_on != 0 AND finished_on < ? "
				+ "ORDER BY job.id DESC ";

		Object[] params = new Object[2];
		params[0] = state.getValue();
		params[1] = time;

		try {
			List<AbstractJob> result = query(sql, params, new JobAndUserMapper());
			log.debug("find all old jobs successful. results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all old jobs failed", e);
			return null;
		}
	}

	public List<AbstractJob> findAllByState(JobState state) {
		String sql = "SELECT * FROM job "
				+ "JOIN `user` ON job.user_id = `user`.id "
				+ "WHERE state = ? ORDER BY job.id DESC";

		Object[] params = new Object[1];
		params[0] = state.getValue();

		try {
			List<AbstractJob> result = query(sql, params, new JobAndUserMapper());
			log.debug("find all jobs by state successful. results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all jobs by state failed", e);
			return null;
		}
	}

	public AbstractJob findById(String id) {
		return findById(id, true);
	}

	public AbstractJob findById(String id, boolean loadParams) {
		String sql = "SELECT * FROM job "
				+ "JOIN `user` ON job.user_id = `user`.id "
				+ "WHERE job.id = ? AND state != ?";

		Object[] params = new Object[2];
		params[0] = id;
		params[1] = JobState.DELETED.getValue();

		try {
			AbstractJob job = queryForObject(sql, params, new JobAndUserMapper());

			if (loadParams && job != null) {
				ParameterDao parameterDao = new ParameterDao(database);
				List<CloudgeneParameterInput> inputParams = parameterDao.findAllInputByJob(job);
				List<CloudgeneParameterOutput> outputParams = parameterDao.findAllOutputByJob(job);
				job.setInputParams(inputParams);
				job.setOutputParams(outputParams);

				if (job instanceof CloudgeneJob) {
					StepDao stepDao = new StepDao(database);
					List<Step> steps = stepDao.findAllByJob((CloudgeneJob) job);
					job.setSteps(steps);
				}
			}

			if (job instanceof CloudgeneJob) {
				((CloudgeneJob) job).updateProgress();
			}

			if (job != null) {
				log.debug("find job by id '{}' successful.", id);
			} else {
				log.debug("job '{}' not found", id);
			}

			return job;
		} catch (SQLException e) {
			log.error("find job by id '{}' failed", id, e);
			return null;
		}
	}

	static class JobMapper implements IRowMapper<AbstractJob> {
		@Override
		public AbstractJob mapRow(ResultSet rs, int row) throws SQLException {
			AbstractJob job = new CloudgeneJob();

			job.setId(rs.getString("job.id"));
			job.setName(rs.getString("job.name"));
			job.setRawState(JobState.of(rs.getInt("job.state")));
			job.setCompletionState(CompletionState.of(rs.getString("job.completion_state")));
			job.setSuccessState(SuccessState.of(rs.getString("job.success_state")));
			job.setNotificationState(NotificationState.of(rs.getString("job.notification_state")));
			job.setStartTime(rs.getLong("job.start_time"));
			job.setEndTime(rs.getLong("job.end_time"));
			job.setDeletedOn(rs.getLong("job.deleted_on"));
			job.setApplication(rs.getString("job.application"));
			job.setApplicationId(rs.getString("job.application_id"));
			job.setSubmittedOn(rs.getLong("job.submitted_on"));
			job.setUserAgent(rs.getString("job.user_agent"));

			return job;
		}
	}

	static class JobAndUserMapper implements IRowMapper<AbstractJob> {

		private final JobMapper jobMapper = new JobMapper();
		private final UserMapper userMapper = new UserMapper();

		@Override
		public AbstractJob mapRow(ResultSet rs, int row) throws SQLException {
			AbstractJob job = jobMapper.mapRow(rs, row);

			User user = userMapper.mapRow(rs, row);
			job.setUser(user);

			return job;
		}
	}

	public String trimToLength(String string, int maxLength) {
		return string.substring(0, Math.min(string.length(), maxLength));
	}
}
