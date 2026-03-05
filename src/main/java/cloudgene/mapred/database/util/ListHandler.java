package cloudgene.mapred.database.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;

public class ListHandler<T> implements ResultSetHandler<List<T>> {

	private final IRowMapper<T> mapper;

	public ListHandler(IRowMapper<T> rowMapper) {
		this.mapper = rowMapper;
	}

	public List<T> toBeanList(ResultSet rs) throws SQLException {
		List<T> result = new ArrayList<>();

		int row = 0;
		while (rs.next()) {
			T value = mapper.mapRow(rs, row);
			if (value != null) {
				result.add(value);
			}
			row++;
		}

		return result;
	}

	@Override
	public List<T> handle(ResultSet rs) throws SQLException {
		return toBeanList(rs);
	}
}
