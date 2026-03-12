package cloudgene.mapred.database.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.Template;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;

public class TemplateDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(TemplateDao.class);

	public TemplateDao(Database database) {
		super(database);
	}

	public boolean insert(Template snippet) {
		String sql = "INSERT INTO html_snippets (`key`, text) VALUES (?,?)";

		try {
			Object[] params = new Object[2];
			params[0] = snippet.getKey();
			params[1] = snippet.getText();

			update(sql, params);
			log.debug("insert html snippet successful.");
			return true;
		} catch (SQLException e) {
			log.error("insert  html snippet  failed.", e);
			return false;
		}
	}

	public boolean update(Template snippet) {
		String sql = "UPDATE html_snippets SET text = ? WHERE `key` = ?";

		try {
			Object[] params = new Object[2];
			params[0] = snippet.getText();
			params[1] = snippet.getKey();

			update(sql, params);
			log.debug("update html snippet successful.");
			return true;
		} catch (SQLException e) {
			log.error("update  html snippet  failed.", e);
			return false;
		}
	}

	public List<Template> findAll() {
		String sql = "SELECT * FROM html_snippets";

		try {
			List<Template> result = query(sql, new TemplateMapper());
			log.debug("find all html snippets successful. results: " + result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all html snippets failed", e);
			return null;
		}
	}

	public Template findByKey(String key) {
		String sql = "SELECT * FROM html_snippets WHERE `key` = ?";

		try {
			Object[] params = new Object[1];
			params[0] = key;

			Template result = queryForObject(sql, params, new TemplateMapper());
			log.debug("find html snippet by key '{}' successful.", key);
			return result;
		} catch (SQLException e1) {
			log.error("find html snippet by key '{}'  failed.", key, e1);
			return null;
		}
	}

	static class TemplateMapper implements IRowMapper<Template> {
		@Override
		public Template mapRow(ResultSet rs, int row) throws SQLException {
			return new Template(rs.getString("key"), rs.getString("text"));
		}
	}
}
