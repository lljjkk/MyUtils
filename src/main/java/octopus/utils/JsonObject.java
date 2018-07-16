package octopus.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class JsonObject {

	/**
	 * 添加处理后的字符串
	 * @param str 待转字符串
	 */
	public static void addString(StringBuilder sb, String str) {
		if (str == null || str.length() == 0) {
			sb.append("\"\"");
		} else {
			char b;
			char c = 0;
			String hhhh;
			int i;
			int len = str.length();

			sb.append('\"');
			for (i = 0; i < len; i += 1) {
				b = c;
				c = str.charAt(i);
				switch (c) {
					case '\\':
					case '"':
						sb.append('\\');
						sb.append(c);
						break;
					case '/':
						if (b == '<') {
							sb.append('\\');
						}
						sb.append(c);
						break;
					case '\b':
						sb.append("\\b");
						break;
					case '\t':
						sb.append("\\t");
						break;
					case '\n':
						sb.append("\\n");
						break;
					case '\f':
						sb.append("\\f");
						break;
					case '\r':
						sb.append("\\r");
						break;
					default:
						if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
								|| (c >= '\u2000' && c < '\u2100')) {
							sb.append("\\u");
							hhhh = Integer.toHexString(c);
							// 高位补0
							sb.append("0000", 0, 3 - hhhh.length());
							sb.append(hhhh);
						} else {
							sb.append(c);
						}
				}
			}
			sb.append('"');
		}
	}
	/**
	 * 添加字段的值
	 * @param sb StringBuilder对象
	 * @param field_type 字段类型
	 * @param rs 结果集
	 * @param index 结果集的第几条记录
	 */
	public static void addFieldValue(StringBuilder sb, String field_type, ResultSet rs, int index) {
		try {
			String value = rs.getString(index);
			if("VARCHAR2".equals(field_type) || "CHAR".equals(field_type) || "NVARCHAR2".equals(field_type) || "DATE".equals(field_type) || "TIMESTAMP".equals(field_type) || "ROWID".equals(field_type)) {
				addString(sb, value);
			} else {
				sb.append(value);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 添加单个记录内容
	 * ResultSet中只含有一个元素时, 返回的字符串形式为
	 * key1: value1,
	 * key2: value2,
	 * key3: value3
	 * @param sb StringBuilder
	 * @param rs 结果集
	 */
	public static void addResult(StringBuilder sb, ResultSet rs) {
		// 是否打印逗号
		boolean commanate = false;
		boolean hasNext;
		try {
			hasNext = rs.next();
			if (hasNext) {
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();
		        // 遍历每一列  
		        for (int i = 1; i <= columnCount; i++) {
		        	if(commanate) {
		        		sb.append(",");
		        	}
		        	sb.append("\"");
		        	sb.append(metaData.getColumnLabel(i));
		        	sb.append("\":");
		        	addFieldValue(sb, metaData.getColumnTypeName(i), rs, i);
		        	commanate = true;
		        }
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 添加结果集
	 * @param sb StringBulider
	 * @param rs 结果集
	 */
	public static void addResultSet(StringBuilder sb, ResultSet rs) {
		sb.append("[");
		// 获取列数  
		ResultSetMetaData metaData;
		try {
			metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();
			// 每条记录是否打印逗号
			boolean commanate1 = false;
			while(rs.next()) {
				if(commanate1)
					sb.append(",");
				sb.append("{");
			    // 遍历列
				// 每列是否打印逗号
				boolean commanate2 = false;
		        for (int j = 1; j <= columnCount; j++) {
		        	if(commanate2)
		        		sb.append(",");
		        	sb.append("\"");
		            sb.append(metaData.getColumnLabel(j));
		            sb.append("\":");
		            addFieldValue(sb, metaData.getColumnTypeName(j), rs, j);
		            commanate2 = true;
		        }
				sb.append("}");
		        commanate1 = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		sb.append("]");
	}
}
