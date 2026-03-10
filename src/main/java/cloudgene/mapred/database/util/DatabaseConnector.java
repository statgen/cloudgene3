package cloudgene.mapred.database.util;

import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;

public interface DatabaseConnector {

	void connect() throws SQLException;

	void disconnect() throws SQLException;

	BasicDataSource getDataSource();

	String getSchema();

	boolean tableExists(String table) throws SQLException;
}
