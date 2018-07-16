package octopus.utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

public class RecordSetHelper {
	private String trueString = "true";
	private ResultSet currResultSet;

	public void setBooleanTrueString(String trueString) {
		this.trueString = trueString;
	}

	public synchronized void preparePipe(ResultSet resultSet, Class<?> targetClass) {
		currResultSet = resultSet;
		ResultSetMetaData metaData;
		try {
			metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			readerList = new ReaderProc[columnCount];
			setterList = new Method[columnCount];
			addtionalData = new Object[columnCount];
			for (int i = 0; i < columnCount; i++) {
				String fieldName = metaData.getColumnLabel(i + 1);
				Method method = getSetter(targetClass, fieldName);
				if (method == null)
					continue;
				Class<?>[] types = method.getParameterTypes();
				if (types.length != 1)
					continue;
				setterList[i] = method;
				if (Enum.class.isAssignableFrom(types[0])) {
					try {
						int dataType = metaData.getColumnType(i + 1);
						if ((dataType == Types.BIGINT) || (dataType == Types.INTEGER) || (dataType == Types.TINYINT)) {
							Method values = types[0].getMethod("values");
							if (values != null) {
								addtionalData[i] = values;
								readerList[i] = setterMap.get(Enum.class);
							}
						} else {
							Method valueOf = types[0].getMethod("valueOf", String.class);
							if (valueOf != null) {
								addtionalData[i] = valueOf;
								readerList[i] = setterMap.get(Enum.class);
							}
						}
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				} else
					readerList[i] = setterMap.get(types[0]);
				if (readerList[i].method == null)
					System.out.println("SQL data type not implemented: " + types[0].getName());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void sendResultSet(Object dao, HttpServletResponse response, Object... openParams) {
		synchronized (dao) {
			ResultSet resultSet = null;
			Class<?> daoClass = dao.getClass();
			Field field = getFieldByName(daoClass, "resultSet");
			if (field == null)
				return;
			Method openMethod = getMethodByName(daoClass, "open");
			if (openMethod == null)
				return;
			try {
				openMethod.invoke(dao, openParams);
				resultSet = (ResultSet) field.get(dao);
				sendResultSet(resultSet, response);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	public static void sendResultSet(ResultSet rs, HttpServletResponse response) {
		SerializerProvider provider = new DefaultSerializerProvider.Impl();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonGenerator jgen = objectMapper.getFactory().createGenerator(response.getWriter());
			ResultSetMetaData rsmd = rs.getMetaData();
			int numColumns = rsmd.getColumnCount();
			String[] columnNames = new String[numColumns];
			int[] columnTypes = new int[numColumns];

			for (int i = 0; i < columnNames.length; i++) {
				columnNames[i] = rsmd.getColumnLabel(i + 1);
				columnTypes[i] = rsmd.getColumnType(i + 1);
			}

			jgen.writeStartArray();
			while (rs.next()) {
				boolean b;
				long l;
				double d;
				jgen.writeStartObject();
				for (int i = 0; i < columnNames.length; i++) {
					jgen.writeFieldName(columnNames[i]);
					switch (columnTypes[i]) {

					case Types.INTEGER:
						l = rs.getInt(i + 1);
						if (rs.wasNull()) {
							jgen.writeNull();
						} else {
							jgen.writeNumber(l);
						}
						break;

					case Types.BIGINT:
						l = rs.getLong(i + 1);
						if (rs.wasNull()) {
							jgen.writeNull();
						} else {
							jgen.writeNumber(l);
						}
						break;

					case Types.DECIMAL:
					case Types.NUMERIC:
						jgen.writeNumber(rs.getBigDecimal(i + 1));
						break;

					case Types.FLOAT:
					case Types.REAL:
					case Types.DOUBLE:
						d = rs.getDouble(i + 1);
						if (rs.wasNull()) {
							jgen.writeNull();
						} else {
							jgen.writeNumber(d);
						}
						break;

					case Types.NVARCHAR:
					case Types.VARCHAR:
					case Types.LONGNVARCHAR:
					case Types.LONGVARCHAR:
						jgen.writeString(rs.getString(i + 1));
						break;

					case Types.BOOLEAN:
					case Types.BIT:
						b = rs.getBoolean(i + 1);
						if (rs.wasNull()) {
							jgen.writeNull();
						} else {
							jgen.writeBoolean(b);
						}
						break;

					case Types.BINARY:
					case Types.VARBINARY:
					case Types.LONGVARBINARY:
						jgen.writeBinary(rs.getBytes(i + 1));
						break;

					case Types.TINYINT:
					case Types.SMALLINT:
						l = rs.getShort(i + 1);
						if (rs.wasNull()) {
							jgen.writeNull();
						} else {
							jgen.writeNumber(l);
						}
						break;

					case Types.DATE:
						provider.defaultSerializeDateValue(rs.getDate(i + 1), jgen);
						break;

					case Types.TIMESTAMP:
						provider.defaultSerializeDateValue(rs.getTime(i + 1), jgen);
						break;

					case Types.BLOB:
						Blob blob = rs.getBlob(i);
						provider.defaultSerializeValue(blob.getBinaryStream(), jgen);
						blob.free();
						break;

					case Types.CLOB:
						Clob clob = rs.getClob(i);
						provider.defaultSerializeValue(clob.getCharacterStream(), jgen);
						clob.free();
						break;

					case Types.ARRAY:
						throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type ARRAY");

					case Types.STRUCT:
						throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type STRUCT");

					case Types.DISTINCT:
						throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type DISTINCT");

					case Types.REF:
						throw new RuntimeException("ResultSetSerializer not yet implemented for SQL type REF");

					case Types.JAVA_OBJECT:
					default:
						provider.defaultSerializeValue(rs.getObject(i + 1), jgen);
						break;
					}
				}
				jgen.writeEndObject();
			}
			jgen.writeEndArray();
			jgen.flush();
		} catch (SQLException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class ReaderProc {
		private Object instance;
		private Method method;

		public ReaderProc(Object instance, Method method) {
			super();
			this.method = method;
			this.instance = instance;
		}

		public Object invoke(ResultSet resultSet, int idx, Object data) {
			try {
				return method.invoke(instance, resultSet, idx, data);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	private Map<Class<?>, ReaderProc> setterMap = new HashMap<Class<?>, ReaderProc>();
	private Method[] setterList;
	private ReaderProc[] readerList;
	private Object[] addtionalData;

	public void assignObjFromResultSet(Object targetObj) {
		Method setter = null;
		for (int i = 0; i < readerList.length; i++)
			try {
				setter = (Method) setterList[i];
				if (setter != null)
					setter.invoke(targetObj, readerList[i].invoke(currResultSet, i + 1, addtionalData[i]));
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
	}

	public RecordSetHelper() {
		super();
		regSetter(String.class, "getValueFromRS_String");
		regSetter(Date.class, "getValueFromRS_Date");
		regSetter(Timestamp.class, "getValueFromRS_Timestamp");
		regSetter(Long.class, "getValueFromRS_Long");
		regSetter(Integer.class, "getValueFromRS_Integer");
		regSetter(Float.class, "getValueFromRS_Float");
		regSetter(Double.class, "getValueFromRS_Double");
		regSetter(Short.class, "getValueFromRS_Short");
		regSetter(Byte.class, "getValueFromRS_Byte");
		regSetter(long.class, "getValueFromRS_long");
		regSetter(int.class, "getValueFromRS_int");
		regSetter(float.class, "getValueFromRS_float");
		regSetter(double.class, "getValueFromRS_double");
		regSetter(short.class, "getValueFromRS_short");
		regSetter(byte.class, "getValueFromRS_byte");
		regSetter(Boolean.class, "getValueFromRS_Boolean");
		regSetter(boolean.class, "getValueFromRS_boolean");
		regSetter(Enum.class, "getValueFromRS_Enum");
	}

	public boolean regSetter(Object owner, Class<?> clazz, String setterName) {
		Method method = getMethodByName(owner.getClass(), setterName);
		if (method == null)
			return false;
		setterMap.put(clazz, new ReaderProc(owner, method));
		return true;
	}

	public boolean regSetter(Class<?> clazz, String setterName) {
		return regSetter(this, clazz, setterName);
	}

	public static Method getMethodByName(Class<?> clazz, String methodName) {
		while (clazz != Object.class) {
			Method[] fields = clazz.getDeclaredMethods();
			clazz = clazz.getSuperclass();
			for (Method method : fields) {
				if (method.getName().equals(methodName)) {
					return method;
				}
			}
		}
		return null;
	}

	public static Field getFieldByName(Class<?> clazz, String fieldName) {
		while (clazz != Object.class) {
			Field[] fields = clazz.getDeclaredFields();
			clazz = clazz.getSuperclass();
			for (Field field : fields) {
				if (field.getName().equals(fieldName)) {
					return field;
				}
			}
		}
		return null;
	}

	public static Method getSetter(Class<?> clazz, String fieldName) {
		String methodName = "set" + StringList.upperFirstChar(fieldName);
		return getMethodByName(clazz, methodName);
	}

	@SuppressWarnings("unchecked")
	synchronized public List<?> resultSetToEntityList(Object dao, OnRecord<?> onRecord, Object... openParams) {
		synchronized (dao) {
			try {
				ResultSet resultSet = null;
				Class<?> daoClass = dao.getClass();
				Field field = getFieldByName(daoClass, "resultSet");
				if (field == null)
					return null;
				Method readMethod = getMethodByName(daoClass, "read");
				if (readMethod == null)
					return null;
				Method openMethod = getMethodByName(daoClass, "open");
				if (openMethod == null)
					return null;
				openMethod.invoke(dao, openParams);
				ParameterizedType genParamType = (ParameterizedType) (onRecord.getClass().getGenericInterfaces()[0]);
				Class<?> clazz = (Class<?>) genParamType.getActualTypeArguments()[0];
				resultSet = (ResultSet) field.get(dao);
				preparePipe(resultSet, clazz);
				Constructor<?> entityCon = clazz.getConstructor();
				List<Object> list = new ArrayList<Object>();
				while ((Boolean) readMethod.invoke(dao)) {
					Object entity = entityCon.newInstance();
					assignObjFromResultSet(entity);
					if (((OnRecord<Object>) onRecord).onRecord(entity))
						list.add(entity);
				}
				return list;
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	synchronized public List<?> resultSetToEntityList(Object dao, Class<?> clazz, Object... openParams) {
		synchronized (dao) {
			try {
				ResultSet resultSet = null;
				Class<?> daoClass = dao.getClass();
				Field field = getFieldByName(daoClass, "resultSet");
				if (field == null)
					return null;
				Method readMethod = getMethodByName(daoClass, "read");
				if (readMethod == null)
					return null;
				Method openMethod = getMethodByName(daoClass, "open");
				if (openMethod == null)
					return null;
				openMethod.invoke(dao, openParams);
				resultSet = (ResultSet) field.get(dao);
				preparePipe(resultSet, clazz);
				Constructor<?> entityCon = clazz.getConstructor();
				List<Object> list = new ArrayList<Object>();
				while ((Boolean) readMethod.invoke(dao)) {
					Object entity = entityCon.newInstance();
					assignObjFromResultSet(entity);
					list.add(entity);
				}
				return list;
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	synchronized public List<?> resultSetToEntityList(ResultSet resultSet, Class<?> clazz) {
		synchronized (resultSet) {
			try {
				preparePipe(resultSet, clazz);
				Constructor<?> entityCon = clazz.getConstructor();
				List<Object> list = new ArrayList<Object>();
				while (resultSet.next()) {
					Object entity = entityCon.newInstance();
					assignObjFromResultSet(entity);
					list.add(entity);
				}
				return list;
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	synchronized public Object recordToEntity(ResultSet resultSet, Class<?> clazz) {
		try {
			preparePipe(resultSet, clazz);
			Constructor<?> entityCon = clazz.getConstructor();
			Object entity = entityCon.newInstance();
			assignObjFromResultSet(entity);
			return entity;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		}
		return null;
	}

	synchronized public Object recordToEntity(Object dao, Class<?> clazz, Object... openParams) {
		synchronized (dao) {
			ResultSet resultSet = null;
			Class<?> daoClass = dao.getClass();
			Field field = getFieldByName(daoClass, "resultSet");
			if (field == null)
				return null;
			Method readMethod = getMethodByName(daoClass, "read");
			if (readMethod == null)
				return null;
			Method openMethod = getMethodByName(daoClass, "open");
			if (openMethod == null)
				return null;
			try {
				openMethod.invoke(dao, openParams);
				if (!(Boolean) readMethod.invoke(dao))
					return null;
				resultSet = (ResultSet) field.get(dao);
				preparePipe(resultSet, clazz);
				Constructor<?> entityCon = clazz.getConstructor();
				Object entity = entityCon.newInstance();
				assignObjFromResultSet(entity);
				return entity;
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			}
			return null;
		}
	}

	public static void invokeSetter(Object obj, Method setter, Object value) {
		try {
			setter.invoke(obj, value);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public Object getValueFromRS_(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getString(idx);
	}

	public Object getValueFromRS_String(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getString(idx);
	}

	public Object getValueFromRS_Date(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getDate(idx);
	}

	public Object getValueFromRS_Timestamp(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getTimestamp(idx);
	}

	public Object getValueFromRS_Long(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getLong(idx);
	}

	public Object getValueFromRS_Integer(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getInt(idx);
	}

	public Object getValueFromRS_Float(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getFloat(idx);
	}

	public Object getValueFromRS_Double(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getDouble(idx);
	}

	public Object getValueFromRS_Short(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getShort(idx);
	}

	public Object getValueFromRS_Byte(ResultSet resultSet, int idx, Object data) throws SQLException {
		return resultSet.getByte(idx);
	}

	public Object getValueFromRS_long(ResultSet resultSet, int idx, Object data) throws SQLException {
		Object result = resultSet.getLong(idx);
		return result;
	}

	public Object getValueFromRS_int(ResultSet resultSet, int idx, Object data) throws SQLException {
		Object result = resultSet.getInt(idx);
		return result;
	}

	public Object getValueFromRS_float(ResultSet resultSet, int idx, Object data) throws SQLException {
		Object result = resultSet.getFloat(idx);
		return result;
	}

	public Object getValueFromRS_double(ResultSet resultSet, int idx, Object data) throws SQLException {
		Object result = resultSet.getDouble(idx);
		return result;
	}

	public Object getValueFromRS_short(ResultSet resultSet, int idx, Object data) throws SQLException {
		Object result = resultSet.getShort(idx);
		return result;
	}

	public Object getValueFromRS_byte(ResultSet resultSet, int idx, Object data) throws SQLException {
		Object result = resultSet.getByte(idx);
		return result;
	}

	public Object getValueFromRS_Boolean(ResultSet resultSet, int idx, Object data) throws SQLException {
		Object resultStr = resultSet.getString(idx);
		if (resultStr == null)
			return null;
		return resultStr.equals(trueString);
	}

	public Object getValueFromRS_boolean(ResultSet resultSet, int idx, Object data) throws SQLException {
		Object resultStr = resultSet.getString(idx);
		if (resultStr == null)
			return false;
		return resultStr.equals(trueString);
	}

	public Object getValueFromRS_Enum(ResultSet resultSet, int idx, Object data) throws SQLException {
		Method method = (Method) data;
		try {
			if (method.getName().equals("valueOf")) {
				String resultStr = resultSet.getString(idx);
				if ((resultStr == null) || resultStr.isEmpty())
					return null;
				return method.invoke(null, resultStr);
			} else if (method.getName().equals("values")) {
				Integer valueIdx = resultSet.getInt(idx);
				Object[] a = ((Object[]) method.invoke(null));
				if ((valueIdx >= 0) && (valueIdx < a.length))
					return a[valueIdx];
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// Input value is invalid
		}
		return null;
	}
}
