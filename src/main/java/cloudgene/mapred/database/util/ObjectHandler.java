package cloudgene.mapred.database.util;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;

public class ObjectHandler<T> implements ResultSetHandler<T> {

	private final IRowMapper<T> mapper;

	public ObjectHandler(IRowMapper<T> rowMapper) {
		this.mapper = rowMapper;
	}

	public T toBean(ResultSet rs) throws SQLException {
		if (!rs.next()) {
			return null;
		} else {
			return mapper.mapRow(rs, 0);
		}
	}

	@Override
	public T handle(ResultSet rs) throws SQLException {
		return toBean(rs);
	}
}
