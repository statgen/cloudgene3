package cloudgene.mapred.database;

import cloudgene.mapred.core.Banner;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class BannerDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(BannerDao.class);

	public BannerDao(Database database) {
		super(database);
	}

	public Banner insert(@NotNull Banner.Type type, @NotNull String message) {
		String insertSql = "INSERT INTO banners (type, message, position) "
				+ "SELECT ?, ?, COALESCE(MAX(position), 0) + 1 FROM banners";

		String selectSql = "SELECT * FROM banners WHERE id = LAST_INSERT_ID()";

		try {
			Object[] insertParams = new Object[2];
			insertParams[0] = type.toString();
			insertParams[1] = message;

			insert(insertSql, insertParams, new IntegerMapper()); // Result discarded, seems to be total row count.
			Banner banner = queryForObject(selectSql, new BannerMapper());

			log.debug("insert banner successful.");
			return banner;
		} catch (SQLException e) {
			log.error("insert banner failed.", e);
			return null;
		}
	}

	public boolean update(@NotNull Banner banner) {
		if (banner.getId() < 0) {
			throw new IllegalArgumentException("Banner does not have an assigned id yet.");
		}

		try {
			update(
					"UPDATE banners SET type = ?, message = ?, position = ? WHERE id = ?",
					banner.getType().toString(),
					banner.getMessage(),
					banner.getPosition(),
					banner.getId());

			log.debug("update banner successful.");
			return true;
		} catch (SQLException e) {
			log.error("update banner failed.", e);
			return false;
		}
	}

	public List<Banner> findAll() {
		String sql = "SELECT * FROM banners ORDER BY position ASC";

		try {
			List<Banner> result = query(sql, new BannerMapper());
			log.debug("find all banners successful. Results: {}", result.size());
			return result;
		} catch (SQLException e) {
			log.error("find all banners failed.", e);
			return List.of();
		}
	}

	public Banner findById(int id) {
		String sql = "SELECT * FROM banners WHERE id = ?";

		try {
			Object[] params = new Object[1];
			params[0] = id;

			Banner result = queryForObject(sql, params, new BannerMapper());

			log.debug("find banner by id = {} successful.", id);
			return result;
		} catch (SQLException e) {
			log.debug("find banner by id = {} failed.", id, e);
			return null;
		}
	}

	public boolean delete(@NotNull Banner banner) {
		try {
			update(
					"DELETE FROM banners WHERE id = ?",
					banner.getId());

			update(
					"UPDATE banners "
							+ "SET position = position - 1 "
							+ "WHERE position > ?",
					banner.getPosition());

			log.debug("delete banner successful.");
			return true;
		} catch (SQLException e) {
			log.error("delete banner failed.", e);
			return false;
		}
	}

	public boolean swap(@NotNull Banner first, @NotNull Banner second) {
		int delta = first.getPosition() - second.getPosition();
		if (delta != -1 && delta != +1) {
			// Elements are not consecutive and cannot be swapped.
			return false;
		}

		try {
			// NOTE(Marc): Originally, this was written to update both records atomically,
			// so that 'position' could be UNIQUE. However, MySQL / MariaDB does this very
			// dumb thing where uniqueness constraints are checked *for each row modified*
			// instead of at the end of the transaction, so keeping UNIQUE would make this
			// messy. We chose instead to not require UNIQUE in the schema.
			update(
					"UPDATE banners SET position = CASE "
							+ "WHEN id = ? THEN ? "
							+ "WHEN id = ? THEN ? "
							+ "END "
							+ "WHERE id = ? OR id = ?",
					first.getId(), second.getPosition(),
					second.getId(), first.getPosition(),
					first.getId(), second.getId());

			log.debug("swap banners id={} and id={} successful.", first.getId(), second.getId());
			return true;
		} catch (SQLException e) {
			log.error("swap banners id={} and id={} failed.", first.getId(), second.getId(), e);
			return false;
		}
	}

	private static class BannerMapper implements IRowMapper<Banner> {
		@Override
		public Banner mapRow(@NotNull ResultSet rs, int row) throws SQLException {
			return new Banner(
					Banner.Type.of(rs.getString("type")),
					rs.getString("message"),
					rs.getInt("position"),
					rs.getInt("id"));
		}
	}
}
