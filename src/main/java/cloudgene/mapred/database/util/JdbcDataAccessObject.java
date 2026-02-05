/*******************************************************************************
 * Copyright (C) 2009-2016 Lukas Forer and Sebastian Schönherr
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

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
import org.apache.commons.dbutils.handlers.MapListHandler;

public abstract class JdbcDataAccessObject {

	protected QueryRunner runner;

	protected Database database;

	public JdbcDataAccessObject(Database database) {
		this.database = database;
		runner = new QueryRunner(database.getDataSource());
	}

	public <T> T queryForObject(String sql, IRowMapper<T> mapper)
			throws SQLException {
		return runner.query(sql, new ObjectHandler<>(mapper));
	}

	public <T> T queryForObject(String sql, Object[] params, IRowMapper<T> mapper)
			throws SQLException {
		return runner.query(sql, new ObjectHandler<>(mapper), params);
	}

	public <T> List<T> query(String sql, Object[] params, IRowMapper<T> mapper)
			throws SQLException {
		return runner.query(sql, new ListHandler<>(mapper), params);
	}

	public <K, V> Map<K, V> queryForMap(String sql, IRowMapMapper<K, V> mapper)
			throws SQLException {
		return runner.query(sql, new MapHandler<>(mapper));
	}

	public <K, V> Map<K, V> queryForMap(String sql, Object[] params, IRowMapMapper<K, V> mapper)
			throws SQLException {
		return runner.query(sql, new MapHandler<>(mapper), params);
	}

	public <K, V> Map<K, List<V>> queryForGroupedList(String sql, Object[] params,
			IRowMapMapper<K, V> mapper) throws SQLException {
		return runner.query(sql, new GroupedListHandler<>(mapper), params);
	}

	public <K, V> Map<K, List<V>> queryForGroupedList(String sql, IRowMapMapper<K, V> mapper)
			throws SQLException {
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
		ResultSetHandler<List<Integer>> handler = new ResultSetHandler<List<Integer>>() {
			@Override
			public List<Integer> handle(ResultSet rs) throws SQLException {
				List<Integer> identifiers = new ArrayList<Integer>();

				while (rs.next()) {
					identifiers.add(rs.getInt(1));
				}

				return identifiers;
			}
		};

		return runner.insertBatch(sql, handler, params);
	}

	public boolean callProcedure(String sql, Object[] params) throws SQLException {
		try (Connection connection = database.getDataSource().getConnection()) {
			CallableStatement cstmt = connection.prepareCall(sql);
			runner.fillStatement(cstmt, params);
			boolean state = cstmt.execute();
			cstmt.close();
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
}
