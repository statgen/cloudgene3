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

import java.sql.ResultSet;
import java.sql.SQLException;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
=======
import java.util.*;
>>>>>>> origin/statgen-custom-changes

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
