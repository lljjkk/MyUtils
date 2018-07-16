package octopus.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class MySqlUtil {
	Connection conn;
	public static String defaultValueSeperator = "|";

	public boolean dumpTable(String tableName) {
		return dumpTable(tableName, defaultValueSeperator);
	}

	public boolean dumpTable(String tableName, String sep) {
		return printQuery("SELECT * FROM " + tableName, sep);
	}

	public boolean printQuery(String sql) {
		return printQuery(sql, defaultValueSeperator);
	}

	public boolean printQueryTS(String sql) {
		return printQuery(sql, "\t");
	}

	public boolean printQuery(String sql, String sep) {
		try {
			PreparedStatement statement = null;
			statement = conn.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();
			return dumpResultSet(resultSet, sep);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

	}

	public static boolean printRecord(ResultSet resultSet) {
		return printRecord(resultSet, defaultValueSeperator);
	}

	public static boolean printRecord(ResultSet resultSet, String sep) {
		try {
			int columnCount = resultSet.getMetaData().getColumnCount();
			System.out.print(sep);
			for (int i = 1; i <= columnCount; i++) {
				System.out.print(resultSet.getString(i));
				System.out.print(sep);
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean dumpResultSet(ResultSet resultSet) {
		return dumpResultSet(resultSet, defaultValueSeperator);
	}

	public static boolean dumpResultSet(ResultSet resultSet, String sep) {
		try {
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			if (columnCount == 0)
				return false;
			System.out.print(metaData.getColumnLabel(1));
			for (int i = 2; i <= columnCount; i++) {
				System.out.print(sep);
				System.out.print(metaData.getColumnLabel(i));
			}
			System.out.println("");
			while (resultSet.next()) {
				System.out.print(resultSet.getString(1));
				for (int i = 2; i <= columnCount; i++) {
					System.out.print(sep);
					System.out.print(resultSet.getString(i));
				}
				System.out.println("");
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public int execSQL(String sql) {
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			int result = statement.executeUpdate();
			if (!conn.getAutoCommit())
				conn.commit();
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	private QueryLastInsertId queryLastInsertId = new QueryLastInsertId();

	private class QueryLastInsertId {
		String SQL = "select LAST_INSERT_ID()";

		public PreparedStatement statement = null;
		public ResultSet resultSet = null;
		public long f_LastInsertId;

		public boolean open() {
			try {
				if (statement == null)
					statement = (PreparedStatement) conn.prepareStatement(SQL);
				resultSet = statement.executeQuery();
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}

		public boolean read() {
			boolean hasNext;
			try {
				hasNext = resultSet.next();
				if (hasNext) {
					f_LastInsertId = resultSet.getLong(1);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
			return hasNext;
		}
	}

	public long getLastId() {
		synchronized (queryLastInsertId) {
			queryLastInsertId.open();
			if (queryLastInsertId.read())
				return queryLastInsertId.f_LastInsertId;
			else
				return 0;
		}
	}

	public MySqlUtil(Connection conn) {
		super();
		this.conn = conn;
	}

}
