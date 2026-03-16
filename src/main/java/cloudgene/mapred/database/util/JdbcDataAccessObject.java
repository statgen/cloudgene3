package cloudgene.mapred.database.util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public abstract class JdbcDataAccessObject {

	protected QueryRunner runner;

	protected Database database;

	public JdbcDataAccessObject(Database database) {
		this.database = database;
		runner = new QueryRunner(database.getDataSource());
	}

	public <T> T queryForObject(String sql, IRowMapper<T> mapper) throws SQLException {
		return runner.query(sql, new ObjectHandler<>(mapper));
	}

	public <T> T queryForObject(String sql, Object[] params, IRowMapper<T> mapper) throws SQLException {
		return runner.query(sql, new ObjectHandler<>(mapper), params);
	}

	public <T> List<T> query(String sql, Object[] params, IRowMapper<T> mapper) throws SQLException {
		return runner.query(sql, new ListHandler<>(mapper), params);
	}

	public <K, V> Map<K, V> queryForMap(String sql, IRowMapMapper<K, V> mapper) throws SQLException {
		return runner.query(sql, new MapHandler<>(mapper));
	}

	public <K, V> Map<K, V> queryForMap(String sql, IRowMapMapper<K, V> mapper, Object... params) throws SQLException {
		return runner.query(sql, new MapHandler<>(mapper), params);
	}

	public <K, V> Map<K, List<V>> queryForGroupedList(String sql, Object[] params, IRowMapMapper<K, V> mapper)
			throws SQLException {
		return runner.query(sql, new GroupedListHandler<>(mapper), params);
	}

	public <K, V> Map<K, List<V>> queryForGroupedList(String sql, IRowMapMapper<K, V> mapper) throws SQLException {
		return runner.query(sql, new GroupedListHandler<>(mapper));
	}

	public <T> List<T> query(String sql, IRowMapper<T> mapper) throws SQLException {
		return runner.query(sql, new ListHandler<>(mapper));
	}

	public int update(String sql, Object... params) throws SQLException {
		return runner.update(sql, params);
	}

	public int update(String sql) throws SQLException {
		return runner.update(sql);
	}

	public <T> T insert(String sql, Object[] params, IRowMapper<T> mapper) throws SQLException {
		return runner.insert(sql, new ObjectHandler<>(mapper), params);
	}

	public int insert(String sql, Object[] params) throws SQLException {
		try (Connection connection = database.getDataSource().getConnection()) {
			PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

			runner.fillStatement(statement, params);

			statement.executeUpdate();

			ResultSet rs = statement.getGeneratedKeys();
			rs.beforeFirst();
			rs.next();
			int id = rs.getInt(1);
			connection.close();
			return id;
		}
	}

	public int[] batch(String sql, Object[][] params) throws SQLException {
		return runner.batch(sql, params);
	}

	// DBUtils 1.6 method
	public List<Integer> batchGeneratedKeys(String sql, Object[][] params) throws SQLException {
		ResultSetHandler<List<Integer>> handler = rs -> {
			List<Integer> identifiers = new ArrayList<>();

			while (rs.next()) {
				identifiers.add(rs.getInt(1));
			}

			return identifiers;
		};

		return runner.insertBatch(sql, handler, params);
	}

	public boolean callProcedure(String sql, Object[] params) throws SQLException {
		try (Connection connection = database.getDataSource().getConnection()) {
			CallableStatement statement = connection.prepareCall(sql);
			runner.fillStatement(statement, params);
			boolean state = statement.execute();
			statement.close();
			connection.close();
			return state;
		}
	}

	public static class IntegerMapper implements IRowMapper<Integer> {
		@Override
		public Integer mapRow(ResultSet rs, int row) throws SQLException {
			return rs.getInt(1);
		}
	}

	public static class StringMapper implements IRowMapper<String> {
		@Override
		public String mapRow(ResultSet rs, int row) throws SQLException {
			return rs.getString(1);
		}
	}
}
