package cloudgene.mapred.database.util.h2;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.database.util.DatabaseConnector;

public class H2Connector implements DatabaseConnector {

	protected static final Logger log = LoggerFactory.getLogger(H2Connector.class);

	private BasicDataSource dataSource;

	private final String path;
	private final String user;
	private final String password;
	private final boolean multiuser;

	public H2Connector(String path, String user, String password, boolean multiuser) {
		this.path = path;
		this.user = user;
		this.password = password;
		this.multiuser = multiuser;
	}

	@Override
	public void connect() throws SQLException {
		log.debug("Establishing connection to " + user + "@" + path);

		if (DbUtils.loadDriver("org.h2.Driver")) {
			try {
				dataSource = new BasicDataSource();
				dataSource.setDriverClassName("org.h2.Driver");

				String newPath;
				if (!path.startsWith("/")) {
					newPath = "./" + path;
				} else {
					newPath = path;
				}

				if (multiuser) {
					dataSource.setUrl("jdbc:h2:" + newPath + ";AUTO_SERVER=TRUE;MODE=MySQL");
				} else {
					dataSource.setUrl("jdbc:h2:" + newPath + ";MODE=MySQL");
				}

				dataSource.setUsername(user);
				dataSource.setPassword(password);
				dataSource.setMaxIdle(10_000);
				dataSource.setDefaultAutoCommit(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			log.error("H2 Driver Class not found");
		}
	}

	@Override
	public void disconnect() throws SQLException {
		dataSource.close();
	}

	@Override
	public void executeSQL(@NotNull InputStream is) throws SQLException, IOException, URISyntaxException {
		String sqlContent = readFileAsString(is);

		if (!sqlContent.isEmpty()) {
			Connection connection = dataSource.getConnection();
			PreparedStatement ps = connection.prepareStatement(sqlContent);
			ps.executeUpdate();
			connection.close();
		}
	}

	public static String readFileAsString(@NotNull InputStream is) throws java.io.IOException, URISyntaxException {
		DataInputStream in = new DataInputStream(is);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		StringBuilder builder = new StringBuilder();

		while ((strLine = br.readLine()) != null) {
			builder.append("\n");
			builder.append(strLine);
		}

		in.close();
		return builder.toString();
	}

	public BasicDataSource getDataSource() {
		return dataSource;
	}

	@Override
	public String getSchema() {
		return null;
	}

	@Override
	public boolean existsTable(String table) throws SQLException {
		Connection connection = dataSource.getConnection();
		DatabaseMetaData meta = connection.getMetaData();
		ResultSet res = meta.getTables(null, null, table.toUpperCase(), new String[] { "TABLE" });
		boolean exists = res.next();
		res.close();
		connection.close();
		if (!exists) {
			log.warn("Table '" + table + "' not found'");
		}
		return exists;
	}
}
