package cloudgene.mapred.database.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface IRowMapper<T> {
	T mapRow(ResultSet rs, int row) throws SQLException;
}
