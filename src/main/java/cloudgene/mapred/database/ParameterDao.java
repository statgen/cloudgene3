package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.CloudgeneParameterInput;
import cloudgene.mapred.jobs.CloudgeneParameterOutput;
import cloudgene.mapred.jobs.Download;
import cloudgene.mapred.wdl.WdlParameterInputType;
import cloudgene.mapred.wdl.WdlParameterOutputType;

public class ParameterDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(ParameterDao.class);

	public ParameterDao(Database database) {
		super(database);
	}

	public boolean insert(CloudgeneParameterInput parameter) {
		String sql = "INSERT INTO parameter "
				+ "(name, `value`, input, job_id, type, variable, download, format, admin_only, hash) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?)";

		try {
			Object[] params = new Object[10];

			if (parameter.getDescription() != null) {
				params[0] = parameter.getDescription().substring(0, Math.min(parameter.getDescription().length(), 100));
			} else {
				params[0] = "";
			}

			params[1] = parameter.getValue();
			params[2] = true;
			params[3] = parameter.getJob().getId();
			params[4] = parameter.getType().toString();
			params[5] = parameter.getName();
			params[6] = false;
			params[7] = "";
			params[8] = parameter.isAdminOnly();
			params[9] = parameter.getHash();

			int paramId = insert(sql, params);
			parameter.setId(paramId);

			log.debug("insert parameter '" + parameter.getId() + "' successful.");

		} catch (SQLException e) {
			log.error("insert parameter '" + parameter.getId() + "' failed.", e);
			return false;
		}

		return true;
	}

	public boolean insert(CloudgeneParameterOutput parameter) {
		String sql = "INSERT INTO parameter "
				+ "(name, `value`, input, job_id, type, variable, download, format, admin_only, hash) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?)";

		try {
			Object[] params = new Object[10];
			params[0] = parameter.getDescription().substring(0, Math.min(parameter.getDescription().length(), 100));
			params[1] = parameter.getValue();
			params[2] = false;
			params[3] = parameter.getJob().getId();
			params[4] = parameter.getType().toString();
			params[5] = parameter.getName();
			params[6] = parameter.isDownload();
			params[7] = "";
			params[8] = parameter.isAdminOnly();
			params[9] = parameter.getHash();

			int paramId = insert(sql, params);
			parameter.setId(paramId);

			log.debug("insert parameter '" + parameter.getId() + "' successful.");

		} catch (SQLException e) {
			log.error("insert parameter '" + parameter.getId() + "' failed.", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<CloudgeneParameterInput> findAllInputByJob(AbstractJob job) {
		String sql = "SELECT * FROM parameter WHERE job_id = ? AND input = true";

		Object[] params = new Object[1];
		params[0] = job.getId();

		try {
			List<CloudgeneParameterInput> result = query(sql, params, new ParameterInputMapper());
			log.debug("find all input parameters for job '" + job.getId() + "' successful. results: " + result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all input parameters for job '" + job.getId() + "' failed.", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<CloudgeneParameterOutput> findAllOutputByJob(AbstractJob job) {
		String sql = "SELECT * FROM parameter WHERE job_id = ? AND input = false";

		Object[] params = new Object[1];
		params[0] = job.getId();

		try {
			List<CloudgeneParameterOutput> result = query(sql, params, new ParameterOutputMapper());

			DownloadDao downloadDao = new DownloadDao(database);
			for (CloudgeneParameterOutput parameter : result) {
				List<Download> downloads = downloadDao.findAllByParameter(parameter);
				parameter.setFiles(downloads);
			}

			log.debug("find all output parameters for job '" + job.getId() + "' successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all output parameters for job '" + job.getId() + "' failed.", e);
			return null;
		}
	}

	public CloudgeneParameterOutput findById(int id) {
		String sql = "SELECT * FROM parameter WHERE id = ?";

		Object[] params = new Object[1];
		params[0] = id;

		try {
			CloudgeneParameterOutput result = (CloudgeneParameterOutput) queryForObject(sql, params,
					new ParameterOutputMapper());

			DownloadDao downloadDao = new DownloadDao(database);
			List<Download> downloads = downloadDao.findAllByParameter(result);
			result.setFiles(downloads);

			log.debug("find parameter by id '" + id + "' successful.");

			return result;
		} catch (SQLException e) {
			log.error("find parameter by id '" + id + "' failed.", e);
			return null;
		}
	}

	public CloudgeneParameterOutput findByHash(String hash) {
		String sql = "SELECT * FROM parameter WHERE hash = ?";

		Object[] params = new Object[1];
		params[0] = hash;

		try {
			CloudgeneParameterOutput result = (CloudgeneParameterOutput) queryForObject(sql.toString(), params,
					new ParameterOutputMapper());

			DownloadDao downloadDao = new DownloadDao(database);
			List<Download> downloads = downloadDao.findAllByParameter(result);
			result.setFiles(downloads);

			log.debug("find parameter by hash '" + hash + "' successful.");

			return result;
		} catch (SQLException e) {
			log.error("find parameter by hash '" + hash + "' failed.", e);
			return null;
		}
	}

	public List<CloudgeneParameterOutput> findAllOutput() {
		String sql = "SELECT * FROM parameter WHERE input = false";

		try {
			List<CloudgeneParameterOutput> result = query(sql, new ParameterOutputMapper());

			DownloadDao downloadDao = new DownloadDao(database);
			for (CloudgeneParameterOutput parameter : result) {
				List<Download> downloads = downloadDao.findAllByParameter(parameter);
				parameter.setFiles(downloads);
			}

			log.debug("find all output parameters  successful. results: " + result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all output parameters failed.", e);
			return null;
		}
	}

	public boolean deleteSensitiveByJob(AbstractJob job) {
		// FIXME: Automate/generalize in the future
		// Fully remove any parameters that may contain sensitive information, but only once job is completed.
		// The existing workflow schema (yml file) does not have a flag for "sensitive" parameters. A future fix would
		// automate management of such data by understanding which workflow params are sensitive; this hardcoded list
		// is a temporary workaround based on existing workflows.
		try {
			String sql = "DELETE FROM parameter WHERE job_id = ? AND name LIKE '%password%'";

			Object[] params = new Object[1];
			params[0] = job.getId();

			update(sql, params);

			log.info("Job: Succesfully deleted sensitive parameters for job_id '" + job.getId());

			return true;

		} catch (SQLException e) {
			log.error("Job: Error while deleting parameters for job_id '" + job.getId(), e);
			return false;
		}
	}

	static class ParameterInputMapper implements IRowMapper {

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {
			CloudgeneParameterInput parameter = new CloudgeneParameterInput();

			parameter.setDescription(rs.getString("name"));
			parameter.setValue(rs.getString("value"));
			parameter.setName(rs.getString("variable"));
			parameter.setJobId(rs.getString("job_id"));
			parameter.setType(WdlParameterInputType.getEnum(rs.getString("type")));
			parameter.setId(rs.getInt("id"));
			parameter.setAdminOnly(rs.getBoolean("admin_only"));
			parameter.setHash(rs.getString("hash"));

			return parameter;
		}
	}

	static class ParameterOutputMapper implements IRowMapper {

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {
			CloudgeneParameterOutput parameter = new CloudgeneParameterOutput();

			parameter.setDescription(rs.getString("name"));
			parameter.setValue(rs.getString("value"));
			parameter.setName(rs.getString("variable"));
			parameter.setJobId(rs.getString("job_id"));
			parameter.setType(WdlParameterOutputType.getEnum(rs.getString("type")));
			parameter.setDownload(rs.getBoolean("download"));
			parameter.setId(rs.getInt("id"));
			parameter.setAdminOnly(rs.getBoolean("admin_only"));
			parameter.setHash(rs.getString("hash"));

			return parameter;
		}
	}
}
