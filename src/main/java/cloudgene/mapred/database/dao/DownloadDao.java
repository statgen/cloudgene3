package cloudgene.mapred.database.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.jobs.CloudgeneParameterOutput;
import cloudgene.mapred.jobs.Download;

public class DownloadDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(DownloadDao.class);

	public DownloadDao(Database database) {
		super(database);
	}

	public boolean insert(Download download) {
		String sql = "INSERT INTO downloads "
				+ "(parameter_id, name, path, hash, count, size, job_id) "
				+ "VALUES (?,?,?,?,?,?,?)";

		try {
			Object[] params = new Object[7];
			params[0] = download.getParameter().getId();
			params[1] = download.getName();
			params[2] = download.getPath();
			params[3] = download.getHash();
			params[4] = download.getCount();
			params[5] = download.getSize();
			params[6] = -1;

			update(sql, params);
			log.debug("insert download successful.");
			return true;
		} catch (SQLException e) {
			log.error("insert download failed.", e);
			return false;
		}
	}

	public boolean update(Download download) {
		String sql = "UPDATE downloads SET count = ? WHERE hash = ?";

		try {
			Object[] params = new Object[2];
			params[0] = download.getCount();
			params[1] = download.getHash();

			update(sql, params);
			log.debug("update download by hash {} successful.", download.getHash());
			return true;
		} catch (SQLException e) {
			log.error("update download by hash {} failed.", download.getHash(), e);
			return false;
		}
	}

	public List<Download> findAllByParameter(CloudgeneParameterOutput parameter) {
		String sql = "SELECT * FROM downloads WHERE parameter_id = ? ORDER BY path";

		Object[] params = new Object[1];
		params[0] = parameter.getId();

		try {
			List<Download> result = query(sql, params, new DownloadMapper());
			log.debug("find downloads by parameter {} successful. results: {}", parameter.getId(), result.size());
			return result;
		} catch (SQLException e) {
			log.error("find downloads by parameter {} failed", parameter.getId(), e);
			return null;
		}
	}

	public Download findByHash(String hash) {
		String sql = "SELECT * FROM downloads WHERE hash = ? ORDER BY path";

		Object[] params = new Object[1];
		params[0] = hash;

		try {
			Download result = queryForObject(sql, params, new DownloadMapper());
			log.debug("find download by hash {} successful. result: {}",
					hash, (result == null) ? null : result.getName());
			return result;
		} catch (SQLException e) {
			log.error("find download by hash {} failed.", hash, e);
			return null;
		}
	}

	public Download findByJobAndPath(String job, String path) {
		String sql = "SELECT * FROM downloads WHERE path = ? ORDER BY path";

		Object[] params = new Object[1];
		params[0] = job + "/" + path;

		try {
			Download result = queryForObject(sql, params, new DownloadMapper());
			log.debug("find download by job {} and path {} successful. result: {}",
					job, path, (result == null) ? null : result.getName());
			return result;
		} catch (SQLException e) {
			log.error("find download by job {} and path {} failed.", job, path, e);
			return null;
		}
	}

	public Download findByParameterAndName(CloudgeneParameterOutput param, String filename) {
		String sql = "SELECT * FROM downloads WHERE name = ? AND parameter_id = ? ORDER BY path";

		Object[] params = new Object[2];
		params[0] = filename;
		params[1] = param.getId();

		try {
			Download result = queryForObject(sql, params, new DownloadMapper());
			log.debug("find download by param {} and filename {} successful. result: {}",
					param.getId(), filename, (result == null) ? null : result.getName());
			return result;
		} catch (SQLException e) {
			log.error("find download by param {} and filename {} failed.", param.getId(), filename, e);
			return null;
		}
	}

	static class DownloadMapper implements IRowMapper<Download> {
		@Override
		public Download mapRow(ResultSet rs, int row) throws SQLException {
			Download result = new Download();

			result.setCount(rs.getInt("count"));
			result.setHash(rs.getString("hash"));
			result.setName(rs.getString("name"));
			result.setPath(rs.getString("path"));
			result.setSize(rs.getString("size"));

			return result;
		}
	}
}
