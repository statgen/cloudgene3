package cloudgene.mapred.database.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.ResultSetHandler;

public class GroupedListHandler<K, V> implements ResultSetHandler<Map<K, List<V>>> {

	private final IRowMapMapper<K, V> mapper;

	public GroupedListHandler(IRowMapMapper<K, V> rowMapper) {
		this.mapper = rowMapper;
	}

	public Map<K, List<V>> toBeanList(ResultSet rs) throws SQLException {
		Map<K, List<V>> result = new HashMap<>();

		int row = 0;
		while (rs.next()) {
			K key = mapper.getRowKey(rs, row);
			V value = mapper.getRowValue(rs, row);

			if (value != null) {
				List<V> list = result.get(key);

				if (list == null) {
					list = new ArrayList<>();
					result.put(key, list);
				}

				list.add(value);
			}

			row++;
		}

		return result;
	}

	@Override
	public Map<K, List<V>> handle(ResultSet rs) throws SQLException {
		return toBeanList(rs);
	}
}
