package cloudgene.mapred.database.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;

public interface DatabaseConnector {

	public void connect() throws SQLException;

	public void disconnect() throws SQLException;

	public BasicDataSource getDataSource();

	public void executeSQL(InputStream is)
			throws SQLException, IOException, URISyntaxException;

	public String getSchema();

	boolean tableExists(String table) throws SQLException;
}
