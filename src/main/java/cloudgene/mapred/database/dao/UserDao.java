package cloudgene.mapred.database.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.database.util.IRowMapper;
import cloudgene.mapred.database.util.JdbcDataAccessObject;
import cloudgene.mapred.util.PublicUser;

public class UserDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(UserDao.class);

	public UserDao(Database database) {
		super(database);
	}

	public boolean insert(User user) {
		String sql = "INSERT INTO `user` "
				+ "(username, password, full_name, aws_key, aws_secret_key, save_keys, export_to_s3, s3_bucket, mail, "
				+ "role, export_input_to_s3, activation_code, active, api_token, last_login, locked_until, "
				+ "login_attempts, api_token_expires_on) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		try {
			Object[] params = new Object[18];
			params[0] = user.getUsername().toLowerCase();
			params[1] = user.getPassword();
			params[2] = user.getFullName();
			params[3] = null;
			params[4] = null;
			params[5] = false;
			params[6] = false;
			params[7] = null;
			params[8] = user.getMail();
			params[9] = String.join(User.ROLE_SEPARATOR, user.getRoles());
			params[10] = false;
			params[11] = user.getActivationCode();
			params[12] = user.isActive();
			params[13] = user.getApiToken();
			params[14] = user.getLastLogin();
			params[15] = user.getLockedUntil();
			params[16] = user.getLoginAttempts();
			params[17] = user.getApiTokenExpiresOn();

			int id = insert(sql, params);
			user.setId(id);

			log.debug("insert user '{}' successful.", user.getUsername());
			return true;
		} catch (SQLException e) {
			log.error("insert user '{}' failed.", user.getUsername(), e);
			return false;
		}
	}

	public boolean update(User user) {
		String sql = "UPDATE `user` SET "
				+ "username = ?, password = ?, full_name = ?, aws_key = ?, aws_secret_key = ?, save_keys = ?, "
				+ "export_to_s3 = ?, s3_bucket = ?, mail = ?, role = ?, export_input_to_s3 = ?, active = ?, "
				+ "activation_code = ?, api_token = ?, last_login = ?, locked_until = ?, login_attempts = ?, "
				+ "api_token_expires_on = ? "
				+ "WHERE id = ?";

		try {
			Object[] params = new Object[19];
			params[0] = user.getUsername().toLowerCase();
			params[1] = user.getPassword();
			params[2] = user.getFullName();
			params[3] = null;
			params[4] = null;
			params[5] = false;
			params[6] = false;
			params[7] = null;
			params[8] = user.getMail();
			params[9] = String.join(User.ROLE_SEPARATOR, user.getRoles());
			params[10] = false;
			params[11] = user.isActive();
			params[12] = user.getActivationCode();
			params[13] = user.getApiToken();
			params[14] = user.getLastLogin();
			params[15] = user.getLockedUntil();
			params[16] = user.getLoginAttempts();
			params[17] = user.getApiTokenExpiresOn();
			params[18] = user.getId();

			update(sql, params);
			log.debug("update user '{}' successful.", user.getUsername());
			return true;
		} catch (SQLException e) {
			log.error("update user '{}' failed.", user.getUsername(), e);
			return false;
		}
	}

	public @Nullable User findByUsername(@NotNull String username) {
		String sql = "SELECT * FROM `user` WHERE username = ?";

		try {
			Object[] params = new Object[1];
			params[0] = username.toLowerCase();

			User user = queryForObject(sql, params, new UserMapper());
			log.debug("find user by username '{}' successful.", username);
			return user;
		} catch (SQLException e1) {
			log.error("find user by username {}' failed.", username, e1);
			return null;
		}
	}

	public @Nullable User findByMail(@NotNull String mail) {
		String sql = "SELECT * FROM `user` WHERE mail = ?";

		try {
			Object[] params = new Object[1];
			params[0] = mail.toLowerCase();

			User user = queryForObject(sql, params, new UserMapper());
			log.debug("find user by mail '{}' successful.", mail);
			return user;
		} catch (SQLException e1) {
			log.error("find user by mail {}' failed.", mail, e1);
			return null;
		}
	}

	public User findById(int id) {
		String sql = "SELECT * FROM `user` WHERE id = ?";

		try {
			Object[] params = new Object[1];
			params[0] = id;

			User user = queryForObject(sql, params, new UserMapper());
			log.debug("find user by id '{}' successful.", id);
			return user;
		} catch (SQLException e1) {
			log.error("find user by id failed.", e1);
			return null;
		}
	}

	public List<User> findAll() {
		String sql = "SELECT * FROM `user` ORDER BY username";

		try {
			List<User> result = query(sql, new UserMapper());
			log.debug("find all users successful. size = {}", result.size());
			return result;
		} catch (SQLException e1) {
			log.error("find all users failed.", e1);
			return new ArrayList<>(); // TODO(Marc): This is inconsistent with other DAOs (return null).
		}
	}

	public int countAll() {
		String sql = "SELECT COUNT(*) FROM `user`";

		try {
			// TODO(Marc): We might be forcing null -> 0 here. Is that correct?
			int result = queryForObject(sql, new IntegerMapper());
			log.debug("count all users successful. results: {}", result);
			return result;
		} catch (SQLException e) {
			log.error("count all users failed", e);
			return 0;
		}
	}

	public List<User> findByQuery(String query) {
		String sql = "SELECT * FROM `user` "
				+ "WHERE mail LIKE ? OR username LIKE ? OR full_name LIKE ? "
				+ "ORDER BY username";

		try {
			Object[] params = new Object[3];
			params[0] = "%" + query + "%";
			params[1] = params[0];
			params[2] = params[0];

			List<User> result = query(sql, params, new UserMapper());
			log.debug("find users by query successful. size = {}", result.size());
			return result;
		} catch (SQLException e1) {
			log.error("find users by query failed.", e1);
			return new ArrayList<>(); // TODO(Marc): This is inconsistent with other DAOs (return null).
		}
	}

	public List<User> findAll(int offset, int limit) {
		String sql = "SELECT * FROM `user` ORDER BY username LIMIT ?,?";

		try {
			Object[] params = new Object[2];
			params[0] = offset;
			params[1] = limit;

			List<User> result = query(sql, params, new UserMapper());
			log.debug("find all users (paged) successful. size = {}", result.size());
			return result;
		} catch (SQLException e1) {
			log.error("find all users (paged) failed.", e1);
			return new ArrayList<>(); // TODO(Marc): This is inconsistent with other DAOs (return null).
		}
	}

	public boolean delete(User user) {
		// update all older jobs
		User publicUser = PublicUser.getUser(database);

		JobDao jobDao = new JobDao(database);
		boolean result = jobDao.updateUser(user, publicUser);
		if (!result) {
			log.error("delete user failed");
			return false;
		}

		String sql = "DELETE from `user` WHERE id = ?";

		try {
			Object[] params = new Object[1];
			params[0] = user.getId();

			update(sql, params);
			log.debug("delete user successful.");
			return true;
		} catch (SQLException e) {
			log.error("delete user failed", e);
			return false;
		}
	}

	public static class UserMapper implements IRowMapper<User> {
		@Override
		public User mapRow(ResultSet rs, int row) throws SQLException {
			User user = new User();

			user.setId(rs.getInt("user.id"));
			user.setUsername(rs.getString("user.username"));
			user.setPassword(rs.getString("user.password"));
			user.setFullName(rs.getString("user.full_name"));
			user.setMail(rs.getString("user.mail"));

			if (rs.getString("user.role") != null) {
				user.setRoles(rs.getString("user.role").split(User.ROLE_SEPARATOR));
			} else {
				user.setRoles(new String[0]);
			}

			user.setActivationCode(rs.getString("user.activation_code"));
			user.setActive(rs.getBoolean("user.active"));
			user.setApiToken(rs.getString("user.api_token"));
			user.setLastLogin(rs.getTimestamp("user.last_login"));
			user.setLockedUntil(rs.getTimestamp("user.locked_until"));
			user.setLoginAttempts(rs.getInt("user.login_attempts"));
			user.setApiTokenExpiresOn(rs.getTimestamp("user.api_token_expires_on"));

			return user;
		}
	}
}
