package cloudgene.mapred.database.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbutils.ResultSetHandler;

public class MapHandler<K, V> implements ResultSetHandler<Map<K, V>> {

	private final IRowMapMapper<K, V> mapper;

	public MapHandler(IRowMapMapper<K, V> rowMapper) {
		this.mapper = rowMapper;
	}

	public Map<K, V> toBeanList(ResultSet rs) throws SQLException {
		Map<K, V> result = new HashMap<>();

		int row = 0;
		while (rs.next()) {
			K key = mapper.getRowKey(rs, row);
			V value = mapper.getRowValue(rs, row);
			if (value != null) {
				result.put(key, value);
			}
			row++;
		}

		return result;
	}

	@Override
	public Map<K, V> handle(ResultSet rs) throws SQLException {
		return toBeanList(rs);
	}
}
