package octopus.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnFactory {

	public static Connection GetConn(String db_serverName, String db_port,
			String db_instance, String userName, String password) {
		String db_url = "jdbc:mysql://" + db_serverName + ":" + db_port + "/"
				+ db_instance + "?user=" + userName + "&password=" + password;
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(db_url);
		} catch (SQLException sqlEx) {
			sqlEx.printStackTrace();
		}
		return conn;
	}

	public static Connection GetConn(String url, String user, String password) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, user, password);
		} catch (SQLException sqlEx) {
			sqlEx.printStackTrace();
		}
		return conn;
	}

	public static Connection GetConn(String url) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException sqlEx) {
			sqlEx.printStackTrace();
		}
		return conn;
	}
}
