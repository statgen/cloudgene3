package cloudgene.mapred.database.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface IRowMapMapper<K, V> {

	public K getRowKey(ResultSet rs, int row) throws SQLException;

	public V getRowValue(ResultSet rs, int row) throws SQLException;

}
