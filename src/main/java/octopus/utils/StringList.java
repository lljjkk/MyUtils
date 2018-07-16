package octopus.utils;

import groovy.lang.Binding;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;

public class StringList extends ArrayList<String> {
	private static final long serialVersionUID = 1L;
	public static StringList singleton = new StringList();

	/***
	 * This function will split a string into a map, if the string is consist
	 * of delimiter separated name=value pairs.
	 * Note: the key and value strings will be automatically trimed 
	 * @param str
	 * @return
	 */
	public static Map<String, String> splitAsMap(String str, String delimiter) {
		Map<String, String> map = new HashMap<String, String>();
		String[] strings = str.split(delimiter);
		for (String s : strings) {
			int idx = s.indexOf("=");
			if (idx > 0)
				map.put(s.substring(0, idx).trim(),
						s.substring(idx + 1).trim());
			else
				map.put("__default", s);
		}
		return map;
	}

	public static void makeDir(String path) {
		File file = new File(path);
		if (!file.exists() && !file.isDirectory())
			file.mkdir();
	}

	/***
	 * This function will split a string into a map, if the string is consist
	 * of delimiter separated name=value pairs.
	 * Note: the key and value strings will be automatically trimed 
	 * @param str
	 * @return
	 */
	public static Map<String, String> splitAsMap(String str) {
		Map<String, String> map = new HashMap<String, String>();
		StringList strings = new StringList();
		strings.setText(str);
		for (String s : strings) {
			int idx = s.indexOf("=");
			if (idx > 0)
				map.put(s.substring(0, idx).trim(),
						s.substring(idx + 1).trim());
			else
				map.put("__default", s);
		}
		return map;
	}

	public String[] toArray() {
		String[] result = new String[count()];
		for (int i = 0; i < count(); i++)
			result[i] = get(i);
		return result;
	}

	public Map<String, String> splitAsMap() {
		return splitAsMap(text());
	}

	public static interface ILoadFromString {
		public void loadFromString(String str);
	}

	public interface OnStringReplace {
		public String onReplace(String oriStr);
	}

	public static String replaceBetween(String content, String start, String end, OnStringReplace proc) {
		String result = content;
		String contentBetween = "";
		int offset = 0;
		int lStart = start.length();
		int lEnd = end.length();
		int l = result.length();
		while (true) {
			int idx1 = result.indexOf(start, offset);
			if (idx1 >= 0) {
				idx1 += lStart;
				int idx2 = result.indexOf(end, idx1);
				if (idx2 >= idx1) {
					contentBetween = result.substring(idx1, idx2);
					String newStr = proc.onReplace(contentBetween);
					result = result.substring(0, idx1) + newStr + result.substring(idx2);
					offset = idx1 + lStart + newStr.length() + lEnd;
					if (offset >= l)
						break;
				} else
					break;
			} else
				break;
		}
		return result;
	}

	public static String replaceBetween(String content, String start, String end, String newStr) {
		String result = content;
		int offset = 0;
		int lStart = start.length();
		int lEnd = end.length();
		int l = result.length();
		while (true) {
			int idx1 = result.indexOf(start, offset);
			if (idx1 >= 0) {
				idx1 += lStart;
				int idx2 = result.indexOf(end, idx1);
				if (idx2 >= idx1) {
					result = result.substring(0, idx1) + newStr + result.substring(idx2);
					offset = idx1 + lStart + newStr.length() + lEnd;
					if (offset >= l)
						break;
				} else
					break;
			} else
				break;
		}
		return result;
	}

	public static String escapeForRegExpr(String str) {
		String result = str;
		result = result.replaceAll("\"", "\\\"");
		result = result.replaceAll("<", "\\<");
		return result;
	}

	/***
	 * Get the part of string that's between begin and end
	 * @param source
	 * @param begin
	 * @param end
	 * @return
	 */
	public static String getBetween(String source, String begin, String end) {
		int idx1;
		if (begin.isEmpty())
			idx1 = 0;
		else
			idx1 = source.indexOf(escapeForRegExpr(begin));

		if (idx1 < 0)
			return "";
		idx1 = idx1 + begin.length();

		int idx2;
		if (end.isEmpty())
			idx2 = source.length();
		else
			idx2 = source.indexOf(escapeForRegExpr(end), idx1);
		if (idx2 < 0)
			return "";
		return source.substring(idx1, idx2);
	}

	/***
	 * Get the part of string that's between begin and end
	 * @param source
	 * @param begin
	 * @param end
	 * @return
	 */
	public static String setBetween(String source, String begin, String end, String newContent) {
		int idx1;
		if (begin.isEmpty())
			idx1 = 0;
		else
			idx1 = source.indexOf(begin);
		if (idx1 < 0)
			return source;
		idx1 = idx1 + begin.length();

		int idx2;
		if (end.isEmpty())
			idx2 = source.length();
		else
			idx2 = source.indexOf(end, idx1);
		if (idx2 < 0)
			return source;

		StringBuilder result = new StringBuilder();
		result.append(source.substring(0, idx1));
		result.append(newContent);
		result.append(source.substring(idx2));
		return result.toString();
	}

	public static boolean fileExists(String fileName) {
		File dataFile = new File(fileName);
		return dataFile.exists();
	}

	/***
	 * Get the part of string that's between begin and end
	 * @param source
	 * @param begin
	 * @param end
	 * @return
	 */
	public static String setBetweenIgnoreCase(String source, String begin, String end, String newContent) {
		String srcLower = source.toLowerCase();
		int idx1;
		if (begin.isEmpty())
			idx1 = 0;
		else
			idx1 = srcLower.indexOf(begin);
		if (idx1 < 0)
			return source;
		idx1 = idx1 + begin.length();

		int idx2;
		if (end.isEmpty())
			idx2 = source.length();
		else
			idx2 = srcLower.indexOf(end, idx1);
		if (idx2 < 0)
			return source;

		StringBuilder result = new StringBuilder();
		result.append(source.substring(0, idx1));
		result.append(newContent);
		result.append(source.substring(idx2));
		return result.toString();
	}

	/***
	 * Get the part of string that's between begin and end, case insensitive.
	 * @param source
	 * @param begin
	 * @param end
	 * @return
	 */
	public static String getBetweenIgnoreCase(String source, String begin, String end) {
		String src = source.toLowerCase();
		int idx1;
		if (begin.isEmpty())
			idx1 = 0;
		else
			idx1 = src.indexOf(begin.toLowerCase());
		if (idx1 < 0)
			return "";
		idx1 = idx1 + begin.length();

		int idx2;
		if (end.isEmpty())
			idx2 = source.length();
		else
			idx2 = src.indexOf(end.toLowerCase(), idx1);
		if (idx2 < 0)
			return "";
		return source.substring(idx1, idx2);
	}

	/***
	 * This function will setup the field values in the class by looking
	 * up the value in the input map object.
	 * @param values
	 */
	public static int loadFields(Object obj, Map<String, String> values) {
		int count = 0;
		Class<?> clazz = obj.getClass();
		while (clazz != Object.class) {
			Field[] fields = clazz.getDeclaredFields();
			clazz = clazz.getSuperclass();
			for (Field field : fields) {
				String cellValue = values.get(field.getName());
				if (cellValue == null)
					cellValue = values.get(upperFirstChar(field.getName()));
				if (cellValue != null)
					try {
						field.setAccessible(true);
						Class<?> fieldType = field.getType();
						if (fieldType == String.class) {
							field.set(obj, cellValue);
						} else if (fieldType == int.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setInt(obj, Integer.parseInt(cellValue));
						} else if (fieldType == long.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setLong(obj, Long.parseLong(cellValue));
						} else if (fieldType == byte.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setByte(obj, Byte.parseByte(cellValue));
						} else if (fieldType == short.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setShort(obj, Short.parseShort(cellValue));
						} else if (fieldType == boolean.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "false";
							field.setBoolean(obj, Boolean.parseBoolean(cellValue));
						} else if (fieldType == double.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setDouble(obj, Double.parseDouble(cellValue));
						} else if (fieldType == float.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setFloat(obj, Float.parseFloat(cellValue));
						} else {
							if (ILoadFromString.class.isAssignableFrom(field.getType())) {
								try {
									Object valueObj = field.getType().newInstance();
									((ILoadFromString) valueObj).loadFromString(cellValue);
									field.set(obj, valueObj);
								} catch (InstantiationException e) {
									e.printStackTrace();
								}
							} else
								field.set(obj, cellValue);
						}
						count++;
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
			}
		}
		return count;
	}

	/***
	 * This function will setup the field values in the class by looking
	 * up the value in the input map object.
	 * @param values
	 */
	public static int loadFields(Object obj, String[] names, String[] values) {
		int count = 0;
		Class<?> clazz = obj.getClass();
		while (clazz != Object.class) {
			Field[] fields = clazz.getDeclaredFields();
			clazz = clazz.getSuperclass();
			for (Field field : fields) {
				int nameIdx = -1;
				String fieldName = field.getName();
				String cellValue = null;
				for (int i = 0; i < names.length; i++)
					if (names[i].equalsIgnoreCase(fieldName)) {
						nameIdx = i;
						if (i < values.length)
							cellValue = values[i];
						break;
					}

				if (nameIdx >= 0)
					try {
						field.setAccessible(true);
						Class<?> fieldType = field.getType();
						if (fieldType == String.class) {
							field.set(obj, cellValue);
						} else if (fieldType == int.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setInt(obj, Integer.parseInt(cellValue));
						} else if (fieldType == long.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setLong(obj, Long.parseLong(cellValue));
						} else if (fieldType == byte.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setByte(obj, Byte.parseByte(cellValue));
						} else if (fieldType == short.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setShort(obj, Short.parseShort(cellValue));
						} else if (fieldType == boolean.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "false";
							field.setBoolean(obj, Boolean.parseBoolean(cellValue));
						} else if (fieldType == double.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setDouble(obj, Double.parseDouble(cellValue));
						} else if (fieldType == float.class) {
							if ((cellValue == null) || cellValue.trim().isEmpty())
								cellValue = "0";
							field.setFloat(obj, Float.parseFloat(cellValue));
						} else {
							if (ILoadFromString.class.isAssignableFrom(field.getType())) {
								try {
									Object valueObj = field.getType().newInstance();
									((ILoadFromString) valueObj).loadFromString(cellValue);
									field.set(obj, valueObj);
								} catch (InstantiationException e) {
									e.printStackTrace();
								}
							} else
								field.set(obj, cellValue);
						}
						count++;
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
			}
		}
		return count;
	}

	public static StringList newFromFile(String fileName) {
		StringList result = new StringList();
		result.loadFromFile(fileName);
		return result;
	}

	public static String lowerFirstChar(String expr) {
		if (expr.length() > 1)
			expr = expr.substring(0, 1).toLowerCase() + expr.substring(1);
		else
			expr = expr.toLowerCase();
		return expr;
	}

	public static String upperFirstChar(String expr) {
		if (expr.length() > 1)
			expr = expr.substring(0, 1).toUpperCase() + expr.substring(1);
		else
			expr = expr.toUpperCase();
		return expr;
	}

	public static void saveTextToFile(String fileName, String text) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			fos.write(text.getBytes());
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static String extractFileName(String path) {
		String result = path;
		int idx = path.lastIndexOf("/");
		if (idx < 0)
			idx = path.lastIndexOf("\\");
		if (idx >= 0)
			result = path.substring(idx + 1);
		return result;
	}

	public static String loadStringFromFile(String fileName) {
		StringBuilder result = new StringBuilder();
		FileReader reader;
		char[] cbuf = new char[256];
		try {
			reader = new FileReader(fileName);
			BufferedReader br = new BufferedReader(reader);
			try {
				int len = br.read(cbuf);
				while (len > 0) {
					result.append(cbuf, 0, len);
					len = br.read(cbuf);
				}
			} finally {
				br.close();
				reader.close();
			}
			return result.toString();
		} catch (FileNotFoundException e2) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String loadStringFromStream(InputStream input) {
		StringBuilder result = new StringBuilder();
		InputStreamReader reader;
		char[] cbuf = new char[256];
		try {
			reader = new InputStreamReader(input);
			BufferedReader br = new BufferedReader(reader);
			int len = br.read(cbuf);
			while (len > 0) {
				result.append(cbuf, 0, len);
				len = br.read(cbuf);
			}
			return result.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	/***
	 * Similar with split(), but will trim each item and ignore empty items
	 * @param text
	 * @param splitter
	 */
	public void splitLoose(String text, String splitter) {
		clear();
		String[] strings = text.split(splitter);
		for (int i = 0; i < strings.length; i++)
			if (!strings[i].isEmpty())
				add(strings[i].trim());
	}

	/***
	 * Similar with splitLoose(), but will return a new StringList instance
	 * @param text
	 * @param splitter
	 */
	public static StringList splitLooseText(String text, String splitter) {
		StringList result = new StringList();
		result.splitLoose(text, splitter);
		return result;
	}

	/***
	 * Split text by splitter, the result is in StringList instance itself
	 * @param splitter
	 * @param text
	 */
	public void split(String text, String splitter) {
		clear();
		String[] strings = text.split(splitter);
		for (String s : strings)
			add(s);
	}

	/***
	 * Similar with split(), but will return a new StringList instance
	 * @param text
	 * @param splitter
	 */
	public static StringList splitText(String text, String splitter) {
		StringList result = new StringList();
		result.split(text, splitter);
		return result;
	}

	/***
	 * The string list as a whole string
	 * @return
	 */
	public String text() {
		StringBuilder result = new StringBuilder();
		if (this.size() > 0) {
			result.append(get(0));
			for (int i = 1; i < size(); i++) {
				result.append("\r\n");
				result.append(get(i));
			}
		}
		return result.toString();
	}

	/***
	 * The string list as a whole string(with no '\r' chars at the line ends)
	 * @return
	 */
	public String text2() {
		StringBuilder result = new StringBuilder();
		if (this.size() > 0) {
			result.append(get(0));
			for (int i = 1; i < size(); i++) {
				result.append("\n");
				result.append(get(i));
			}
		}
		return result.toString();
	}

	public String connectedText(String seperator) {
		StringBuilder result = new StringBuilder();
		if (this.size() > 0) {
			result.append(get(0));
			for (int i = 1; i < size(); i++) {
				result.append(seperator);
				result.append(get(i));
			}
		}
		return result.toString();
	}

	/***
	 * Put the string to the string list, splitted by return
	 * @param value
	 */
	public void setText(String value) {
		clear();
		String[] lines = value.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String aline = lines[i];
			if ((!aline.isEmpty()) && (aline.charAt(aline.length() - 1) == '\r'))
				aline = aline.substring(0, aline.length() - 1);
			add(aline);
		}
	}

	/***
	 * Load string list from a text file
	 * @param fileName
	 */
	public void loadFromFile(String fileName) {
		String line;
		FileReader reader;
		try {
			reader = new FileReader(fileName);
			BufferedReader br = new BufferedReader(reader);
			clear();
			try {
				line = br.readLine();
				while (line != null) {
					try {
						add(line);
						line = br.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				try {
					br.close();
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e2) {
			throw new RuntimeException("File not found:" + e2.getMessage());
		}
	}

	public interface IProcessLine {
		void processLine(String line);
	}

	public interface IProcessDelimited {
		void processLine(String[] values);
	}

	public interface IProcessFile {
		void processFile(String fileName);
	}

	/***
	 * A utility function to process a text file's each line
	 * @param fileName
	 * @param proc
	 */
	public static void processLines(String fileName, IProcessLine proc) {
		String line;
		FileReader reader;
		try {
			reader = new FileReader(fileName);
			BufferedReader br = new BufferedReader(reader);
			try {
				line = br.readLine();
				while (line != null) {
					try {
						proc.processLine(line);
						line = br.readLine();
					} catch (IOException e) {
					}
				}
			} catch (IOException e1) {
			} finally {
				try {
					br.close();
					reader.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}

		} catch (FileNotFoundException e2) {
			throw new RuntimeException("File not found:" + e2.getMessage());
		}
	}

	/***
	 * Guess an appropriate type by the string value
	 * @param value
	 * @return
	 */
	public static String guessTypeByValue(String value) {
		if (value == null)
			return "String";
		String bValue = value.trim().toLowerCase();
		if (bValue.equals("true") || bValue.equals("false"))
			return "boolean";
		try {
			Integer.parseInt(value);
			return "int";
		} catch (Exception e) {
		}
		try {
			Long.parseLong(value);
			return "long";
		} catch (Exception e) {
		}
		try {
			Double.parseDouble(value);
			return "double";
		} catch (Exception e) {
		}
		try {
			Float.parseFloat(value);
			return "float";
		} catch (Exception e) {
		}
		return "String";
	}

	/***
	 * Get a Java field declaration list from a string map
	 * @param fieldValues
	 * @param modifier Field modifier such as public, protected or private.
	 * @return
	 */
	public static String getJavaDclFromMap(Map<String, String> fieldValues, String modifier) {
		StringBuilder result = new StringBuilder();
		for (String fieldName : fieldValues.keySet()) {
			if (fieldName.trim().isEmpty())
				continue;
			if (!(fieldName.toUpperCase().equals(fieldName) && (fieldName.length() > 1)))
				fieldName = StringList.lowerFirstChar(fieldName);
			String fieldValue = fieldValues.get(fieldName);
			String fieldType = guessTypeByValue(fieldValue);
			if (!modifier.isEmpty())
				result.append(modifier).append(" ");
			result.append(fieldType)
					.append(" ")
					.append(fieldName)
					.append(" = ");
			if (fieldType.equals("String"))
				result.append("\"\"");
			else if (fieldType.equals("boolean"))
				result.append("false");
			else
				result.append("0");
			result.append(";\r\n");
		}
		return result.toString();
	}

	public static void setField(Object obj, Field field, String strValue, Object defaultVal) throws IllegalArgumentException, IllegalAccessException {
		if (field == null)
			return;
		Class<?> fieldType = field.getType();
		if (fieldType == String.class) {
			field.set(obj, strValue);
		} else if (ILoadFromString.class.isAssignableFrom(field.getType())) {
			try {
				Object valueObj = field.getType().newInstance();
				((ILoadFromString) valueObj).loadFromString(strValue);
				field.set(obj, valueObj);
			} catch (InstantiationException e) {
				e.printStackTrace();
			}
		} else
			try {
				boolean isNull = (strValue == null) || strValue.trim().isEmpty();
				if (fieldType == int.class) {
					if (isNull)
						field.set(obj, defaultVal);
					else
						field.setInt(obj, Integer.parseInt(strValue));
				} else if (fieldType == long.class) {
					if (isNull)
						field.set(obj, defaultVal);
					else
						field.setLong(obj, Long.parseLong(strValue));
				} else if (fieldType == byte.class) {
					if (isNull)
						field.set(obj, defaultVal);
					else
						field.setByte(obj, Byte.parseByte(strValue));
				} else if (fieldType == short.class) {
					if (isNull)
						field.set(obj, defaultVal);
					else
						field.setShort(obj, Short.parseShort(strValue));
				} else if (fieldType == boolean.class) {
					if (isNull)
						field.set(obj, defaultVal);
					else
						field.setBoolean(obj, Boolean.parseBoolean(strValue));
				} else if (fieldType == double.class) {
					if (isNull)
						field.set(obj, defaultVal);
					else
						field.setDouble(obj, Double.parseDouble(strValue));
				} else if (fieldType == float.class) {
					if (isNull)
						field.set(obj, defaultVal);
					else
						field.setFloat(obj, Float.parseFloat(strValue));
				} else if (fieldType == Integer.class) {
					if (isNull)
						field.set(obj, null);
					else
						field.set(obj, Integer.parseInt(strValue));
				} else if (fieldType == Long.class) {
					if (isNull)
						field.set(obj, null);
					else
						field.set(obj, Long.parseLong(strValue));
				} else if (fieldType == Byte.class) {
					if (isNull)
						field.set(obj, null);
					else
						field.set(obj, Byte.parseByte(strValue));
				} else if (fieldType == Short.class) {
					if (isNull)
						field.set(obj, null);
					else
						field.set(obj, Short.parseShort(strValue));
				} else if (fieldType == Boolean.class) {
					if (isNull)
						field.set(obj, null);
					else
						field.set(obj, Boolean.parseBoolean(strValue));
				} else if (fieldType == Double.class) {
					if (isNull)
						field.set(obj, null);
					else
						field.set(obj, Double.parseDouble(strValue));
				} else if (fieldType == Float.class) {
					if (isNull)
						field.set(obj, null);
					else
						field.set(obj, Float.parseFloat(strValue));
				} else {
					if (isNull)
						field.set(obj, null);
					else
						field.set(obj, strValue);
				}
			} catch (Exception e) {
				field.set(obj, defaultVal);
			}
	}

	public void loopTSV() {
		loopDelimited("\t");
		String src = getClipBoardText();
		setClipBoardText(src.substring(2));
	}

	public boolean loopTSV(Looper looper) {
		return loopDelimited("\t", looper);
	}

	public boolean loopDelimited(String delimiter) {
		return loopDelimited(delimiter, (Looper) null);
	}

	public boolean loopDelimited(String delimiter, Looper looper) {
		int index = 0;
		if (count() == 0)
			return false;
		String header = get(0);
		String line = "";
		if (count() > 1)
			line = get(1);
		String[] names = header.split(delimiter);
		String[] values = line.split(delimiter);
		Field[] targetFields = new Field[names.length];
		Object[] defaults = new Object[names.length];

		if (looper == null) {
			Map<String, String> sampleValues = new HashMap<String, String>();
			for (int i = 0; i < names.length; i++) {
				names[i] = normalizeForVarName(names[i]);
				if (i < values.length)
					sampleValues.put(names[i], values[i]);
			}
			StringBuilder sb = new StringBuilder();
			sb.append(", new Looper() {\r\n")
					.append(getJavaDclFromMap(sampleValues, ""))
					.append("public void loop(){\r\n\r\n")
					.append("}\r\n}");
			StringList.setClipBoardText(sb.toString());
			System.out.println("============================================================");
			System.out.println("===========(^@^): Following code is in clipboard.===========");
			System.out.println("============================================================");
			System.out.print(sb.toString());
			System.out.println("");
			System.out.println("");
			return false;
		}

		Method loopMethod = null;
		for (Method _method : looper.getClass().getMethods()) {
			if (_method.getName().equals("loop")) {
				loopMethod = _method;
				loopMethod.setAccessible(true);
				break;
			}
		}

		if (loopMethod == null) {
			System.out.println("Method 'loop' missing!");
			StringList.setClipBoardText("public void loop(){\r\n\r\n}");
			return false;
		}

		Object[] params = new Object[0];
		looper.doContinue = true;
		looper.before();

		if (looper.shell == null)
			looper.shell = new SnippetRunner(looper);
		Binding context = looper.shell.getContext();
		looper.initSnippetContext(context);

		int count = 0;
		Class<?> clazz = looper.getClass();
		while (clazz != Object.class) {
			Field[] fields = clazz.getDeclaredFields();
			clazz = clazz.getSuperclass();
			for (Field field : fields) {
				String fieldName = field.getName();
				for (int i = 0; i < names.length; i++)
					if (names[i].equalsIgnoreCase(fieldName)) {
						targetFields[i] = field;
						field.setAccessible(true);
						try {
							defaults[i] = field.get(looper);
						} catch (IllegalArgumentException e) {
						} catch (IllegalAccessException e) {
						}
						count++;
						break;
					}
			}
		}
		if (count == 0) {
			Map<String, String> sampleValues = new HashMap<String, String>();
			for (int i = 0; i < names.length; i++)
				if (i < values.length)
					sampleValues.put(names[i], values[i]);
			StringList.setClipBoardText(getJavaDclFromMap(sampleValues, ""));
			System.out.println("============================================================");
			System.out.println("=============(^@^): Data class fields generated!============");
			System.out.println("============================================================");
			return false;
		} else
			for (int j = 1; j < count(); j++) {
				line = get(j);
				values = line.split(delimiter);
				looper.index = index++;
				try {
					for (int i = 0; i < names.length; i++) {
						Field field = targetFields[i];
						if (field != null) {
							String cellValue = null;
							if (i < values.length)
								cellValue = values[i];
							setField(looper, field, cellValue, defaults[i]);
						}
					}
					loopMethod.invoke(looper, params);
					if (!looper.doContinue)
						break;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		looper.after();
		return true;
	}

	public static int loopTSV(String fileName) {
		return loopDelimited(fileName, "\t");
	}

	public static int loopTSV(String fileName, Looper looper) {
		return loopDelimited(fileName, "\t", looper);
	}

	public static int loopDelimited(String fileName, String delimiter) {
		return loopDelimited(fileName, delimiter, null);
	}

	public static int loopDelimited(String fileName, String delimiter, Looper looper) {
		int index = 0;
		FileReader reader;
		try {
			reader = new FileReader(fileName);
			BufferedReader br = new BufferedReader(reader);
			try {
				String header = br.readLine();
				String line = br.readLine();
				String[] names = header.split(delimiter);
				String[] values = line.split(delimiter);
				Field[] targetFields = new Field[names.length];
				Object[] defaults = new Object[names.length];

				if (looper == null) {
					Map<String, String> sampleValues = new HashMap<String, String>();
					for (int i = 0; i < names.length; i++) {
						names[i] = normalizeForVarName(names[i]);
						if (i < values.length)
							sampleValues.put(names[i], values[i]);
					}
					StringBuilder sb = new StringBuilder();
					sb.append(", new Looper() {\r\n")
							.append(getJavaDclFromMap(sampleValues, ""))
							.append("public void loop(){\r\n\r\n")
							.append("}\r\n}");
					StringList.setClipBoardText(sb.toString());
					System.out.println("============================================================");
					System.out.println("===========(^@^): Following code is in clipboard.===========");
					System.out.println("============================================================");
					System.out.print(sb.toString());
					System.out.println("");
					System.out.println("");
					return 0;
				}

				Method loopMethod = null;
				for (Method _method : looper.getClass().getMethods()) {
					if (_method.getName().equals("loop")) {
						loopMethod = _method;
						loopMethod.setAccessible(true);
						break;
					}
				}

				if (loopMethod == null) {
					System.out.println("Method 'loop' missing!");
					StringList.setClipBoardText("public void loop(){\r\n\r\n}");
					return 0;
				}

				Object[] params = new Object[0];
				looper.doContinue = true;
				looper.before();

				if (looper.shell == null)
					looper.shell = new SnippetRunner(looper);
				Binding context = looper.shell.getContext();
				looper.initSnippetContext(context);

				int count = 0;
				Class<?> clazz = looper.getClass();
				while (clazz != Object.class) {
					Field[] fields = clazz.getDeclaredFields();
					clazz = clazz.getSuperclass();
					for (Field field : fields) {
						String fieldName = field.getName();
						for (int i = 0; i < names.length; i++)
							if (names[i].equalsIgnoreCase(fieldName)) {
								targetFields[i] = field;
								field.setAccessible(true);
								try {
									defaults[i] = field.get(looper);
								} catch (IllegalArgumentException e) {
								} catch (IllegalAccessException e) {
								}
								count++;
								break;
							}
					}
				}
				if (count == 0) {
					Map<String, String> sampleValues = new HashMap<String, String>();
					for (int i = 0; i < names.length; i++)
						if (i < values.length)
							sampleValues.put(names[i], values[i]);
					StringList.setClipBoardText(getJavaDclFromMap(sampleValues, ""));
					System.out.println("============================================================");
					System.out.println("=============(^@^): Data class fields generated!============");
					System.out.println("============================================================");
					return 0;
				} else
					while (line != null) {
						try {
							looper.index = index++;
							try {
								for (int i = 0; i < names.length; i++) {
									Field field = targetFields[i];
									if (field != null) {
										String cellValue = null;
										if (i < values.length)
											cellValue = values[i];
										setField(looper, field, cellValue, defaults[i]);
									}
								}
								loopMethod.invoke(looper, params);
								if (!looper.doContinue)
									break;
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							}
							line = br.readLine();
							values = line.split(delimiter);
						} catch (IOException e) {
						}
					}
				looper.after();
				return index;
			} catch (IOException e1) {
				e1.printStackTrace();
				return 0;
			} finally {
				try {
					br.close();
					reader.close();
					return index;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e2) {
			throw new RuntimeException("File not found:" + e2.getMessage());
		}
	}

	public static void processDelimited(String fileName, String delimiter, IProcessDelimited proc) {
		String line;
		FileReader reader;
		try {
			reader = new FileReader(fileName);
			BufferedReader br = new BufferedReader(reader);
			try {
				line = br.readLine();
				while (line != null) {
					try {
						proc.processLine(line.split(delimiter));
						line = br.readLine();
					} catch (IOException e) {
					}
				}
			} catch (IOException e1) {
			} finally {
				try {
					br.close();
					reader.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}

		} catch (FileNotFoundException e2) {
			throw new RuntimeException("File not found:" + e2.getMessage());
		}
	}

	public static void processFiles(String dir, String wildCard, IProcessFile proc) {
		File dirObj = new File(dir);
		FileFilter fileFilter = new WildcardFileFilter(wildCard);
		File[] files = dirObj.listFiles(fileFilter);
		if (files != null)
			try {
				for (int i = 0; i < files.length; i++)
					proc.processFile(files[i].getPath());
			} catch (Exception e) {

			}
	}

	/***
	 * To add all qualified file path to the list
	 * The function will not clear the list automatically. So we need to
	 * clear the path list explicitly if we need.
	 * @param dir
	 * @param wildCard
	 * @param pathList
	 */
	public void getFiles(String dir, String wildCard) {
		File dirObj = new File(dir);
		FileFilter fileFilter = new WildcardFileFilter(wildCard);
		File[] files = dirObj.listFiles(fileFilter);
		if (files != null)
			try {
				for (int i = 0; i < files.length; i++)
					add(files[i].getPath());
			} catch (Exception e) {

			}
	}

	abstract public static class OnBetween {
		public int index = 0;
		public boolean doContinue = true;

		abstract public void onBetween(String content);
	}

	public static void processBetweens(String content, String start, String end, OnBetween proc)
	{
		String contentBetween = "";
		if (content.isEmpty() || start.isEmpty() || end.isEmpty())
			return;
		int offset = 0;
		int lStart = start.length();
		int lEnd = end.length();
		int l = content.length();
		proc.index = -1;
		while (true) {
			int idx1 = content.indexOf(start, offset);
			if (idx1 >= 0) {
				idx1 += lStart;
				int idx2 = content.indexOf(end, idx1);
				if (idx2 >= idx1) {
					contentBetween = content.substring(idx1, idx2);
					offset = idx2 + lEnd;
					if (offset >= l)
						break;
					proc.index++;
					proc.onBetween(contentBetween);
					if (!proc.doContinue)
						break;
				} else
					break;
			} else
				break;
		}
	}

	public static String unescapeHtml(String src) {
		String result = src;
		result = result.replace("&lt;", "<");
		result = result.replace("&gt;", ">");
		result = result.replace("&quot;", "\"");
		result = result.replace("&#39;", "'");
		result = result.replace("&apos;", "'");
		result = result.replace("<br>", "\n");
		result = result.replace("<BR>", "\n");
		result = result.replace("&amp;", "&");
		return result;
	}

	public static String escapeHtml(String src) {
		String result = src;
		result = result.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		result = result.replace(">", "&gt;");
		result = result.replace("\"", "&quot;");
		result = result.replace("'", "&apos;");
		result = result.replace("\r\n", "<br>");
		result = result.replace("\n", "<br>");
		return result;
	}

	public static String translateHtml(String src) {
		String result = unescapeHtml(src);
		result = result.replace("\r", "");
		result = result.replace("\n", "");
		result = result.replace("<br/>", "\n");
		result = result.replace("<br>", "\n");
		result = result.replace("<p>", "\n");
		result = result.replace("</p>", "");
		result = result.replace("&nbsp;", " ");
		return result;
	}

	/***
	 * Count of the items in StringList
	 * @return
	 */
	public int count() {
		return size();
	}

	/***
	 * Save the StringList to external file
	 * @param fileName
	 */
	public void saveToFile(String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			fos.write(text().getBytes());
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/***
	 * Save the StringList to external file. 
	 * This function will not add \r at the line ends
	 * @param fileName
	 */
	public void saveToFile2(String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			fos.write(text2().getBytes());
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static class SimpleWriter {
		private FileWriter out;
		private boolean isEmpty = true;
		private boolean fileExists = false;

		/***
		 * Clear all the contents in the buffer and push them into disk file
		 */
		public void flush() {
			if (out != null)
				try {
					out.flush();
				} catch (IOException e) {
					throw new RuntimeException(e.getMessage());
				}
		}

		/***
		 * Create a writer of a text file
		 * @param fileName
		 * @param append
		 */
		public SimpleWriter(String fileName, boolean append) {
			try {
				isEmpty = !append;
				out = new FileWriter(fileName, append);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		@Override
		protected void finalize() throws Throwable {
			out.flush();
			out.close();
			super.finalize();
		}

		public void write(String str) {
			try {
				if (str != null) {
					out.write(str);
					if (!str.isEmpty())
						isEmpty = false;
				}
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		public void writeln(String str) {
			try {
				if (!isEmpty)
					out.write("\r\n");
				isEmpty = false;
				if (str != null)
					out.write(str);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		}
	}

	public static class TsvLogger {
		protected SimpleWriter writer;
		private boolean isFirstValue = true;
		private boolean fileExists;

		public TsvLogger(String fileName, String... tsvHeaders) {
			fileExists = fileExists(fileName);
			if (!fileExists) {
				writer = new SimpleWriter(fileName, false);
				add(tsvHeaders);
			} else
				writer = new SimpleWriter(fileName, true);
		}

		public boolean fileAlreadyExists() {
			return fileExists;
		}

		public void add(String... values) {
			if (values.length > 0) {
				if (!isFirstValue)
					writer.write("\t");
				writer.write(values[0]);
				for (int i = 1; i < values.length; i++) {
					writer.write("\t");
					writer.write(values[i]);
				}
				isFirstValue = false;
			}
		}

		public void addRow(String... values) {
			writer.writeln("");
			isFirstValue = true;
			add(values);
		}

		public void flush() {
			writer.flush();
		}
	}

	/***
	 * Get JavaScript string constant expression, but without quotes(").
	 * 
	 * @param str
	 * @return
	 */
	public static String toJsString(String str) {
		final String[] strMap = { "\\0", "\\u0001", "\\u0002", "\\u0003",
				"\\u0004", "\\u0005", "\\u0006", "\\u0007", "\\b", "\\t",
				"\\n", "\\u000B", "\\f", "\\r", "\\u000E", "\\u000F",
				"\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014",
				"\\u0015", "\\u0016", "\\u0017", "\\u0018", "\\u0019",
				"\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E",
				"\\u001F", " ", "!", "\\\"", "#", "$", "%", "&", "\'", "(",
				")", "*", "+", ",", "-", ".", "/", "0", "1", "2", "3", "4",
				"5", "6", "7", "8", "9", ":", ";", "<", "=", ">", "?", "@",
				"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
				"M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
				"Y", "Z", "[", "\\\\", "]", "^", "_", "`", "a", "b", "c", "d",
				"e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
				"q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "{", "|",
				"}", "~", "\\u007F" };
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c < 128)
				result.append(strMap[c]);
			else
				result.append(c);
		}
		return result.toString();
	}

	/***
	 * Convert to js string
	 * 
	 * @param x
	 * @return
	 */
	public static String toJsString(int x) {
		return "" + x;
	}

	/***
	 * Convert to js string
	 * 
	 * @param x
	 * @return
	 */
	public static String toJsString(Boolean x) {
		return x.toString();
	}

	/***
	 * Get Java string constant expression, but without quotes(").
	 * 
	 * @param str
	 * @return
	 */
	public static String toJavaString(String str) {
		final String[] strMap = { "\\0", "\\u0001", "\\u0002", "\\u0003",
				"\\u0004", "\\u0005", "\\u0006", "\\u0007", "\\b", "\\t",
				"\\n", "\\u000B", "\\f", "\\r", "\\u000E", "\\u000F",
				"\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014",
				"\\u0015", "\\u0016", "\\u0017", "\\u0018", "\\u0019",
				"\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E",
				"\\u001F", " ", "!", "\\\"", "#", "$", "%", "&", "\\\'", "(",
				")", "*", "+", ",", "-", ".", "/", "0", "1", "2", "3", "4",
				"5", "6", "7", "8", "9", ":", ";", "<", "=", ">", "?", "@",
				"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
				"M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
				"Y", "Z", "[", "\\\\", "]", "^", "_", "`", "a", "b", "c", "d",
				"e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
				"q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "{", "|",
				"}", "~", "\\u007F" };
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c < 128)
				result.append(strMap[c]);
			else
				result.append(c);
		}
		return result.toString();
	}

	/**
	 * To change a specific URL parameter's value.
	 * 
	 * @author yangdo
	 * 
	 */
	public static String setUrlParam(String oriURL, String ParamName,
			String ParamValue) {
		int idx = oriURL.indexOf("?" + ParamName + "=");
		if (idx < 0)
			idx = oriURL.indexOf("&" + ParamName + "=");
		if (idx < 0) {
			if (oriURL.indexOf("?") >= 0)
				return oriURL + "&" + ParamName + "=" + ParamValue;
			else
				return oriURL + "?" + ParamName + "=" + ParamValue;
		} else {
			int idx2 = oriURL.indexOf("&", idx + 1);
			if (idx2 < 0) {
				return oriURL.substring(0, idx + ParamName.length() + 2)
						+ ParamValue;
			} else {
				return oriURL.substring(0, idx + ParamName.length() + 2)
						+ ParamValue + oriURL.substring(idx2, oriURL.length());
			}
		}
	}

	/**
	 * To change a specific URL parameter's value.
	 * 
	 * @author yangdo
	 * 
	 */
	public static String getUrlParam(String oriURL, String ParamName) {
		String result = "";
		ParamName = ParamName.trim();
		int idx = oriURL.indexOf("?" + ParamName + "=");
		if (idx < 0)
			idx = oriURL.indexOf("&" + ParamName + "=");

		int idx2 = oriURL.indexOf("&", idx + 1);
		if (idx >= 0) {
			idx += ParamName.length() + 2;
			if (idx2 > 0)
				result = oriURL.substring(idx, idx2);
			else
				result = oriURL.substring(idx);
		}
		return result;
	}

	/***
	 * To change the web color format rgb(xx,xx,xx) to #xxxxxx
	 * @param webColor
	 * @return The color in unified format
	 */
	public static String unifyWebColor(String webColor) {
		webColor = webColor.trim().toLowerCase();
		String result = webColor;
		int idx = webColor.indexOf("rgb(");
		int idx2 = webColor.indexOf(")");
		if ((idx >= 0) && (idx2 > 0)) {
			result = result.substring(idx + 4, idx2);
			String args[] = result.split(",");
			result = "#";
			for (String arg : args) {
				arg = Integer.toHexString(Integer.parseInt(arg.trim()));
				if (arg.length() == 1)
					arg = "0" + arg;
				result += arg;
			}
		}
		return result;
	}

	/***
	 * Remove a parameter from a URL
	 * 
	 * @author yangdo
	 * @param oriURL
	 * @param ParamName
	 * @return
	 */
	public static String deleteUrlParam(String oriURL, String ParamName) {
		int idx = oriURL.indexOf("?" + ParamName + "=");
		if (idx < 0)
			idx = oriURL.indexOf("&" + ParamName + "=");
		if (idx >= 0) {
			int idx2 = oriURL.indexOf("&", idx + 1);
			if (idx2 < 0) {
				return oriURL.substring(0, idx);
			} else {
				return oriURL.substring(0, idx + 1)
						+ oriURL.substring(idx2 + 1, oriURL.length());
			}
		}
		return oriURL;
	}

	/***
	 * Extract multiple value occurence of specific param from a URL
	 * @param url
	 * @param paramName
	 * @return
	 */
	public static List<String> extractMultiParamValues(String url,
			String paramName) {
		List<String> result = new ArrayList<String>();
		String url_lower = url.toLowerCase();
		String pStr = "&" + paramName.toLowerCase() + "=";
		int startIdx = url_lower.indexOf("?" + paramName.toLowerCase() + "=");
		if (startIdx < 0)
			startIdx = url_lower.indexOf(pStr);
		while (startIdx >= 0) {
			int idx2 = url_lower.indexOf("&", startIdx + 1);
			String paramValue;
			if (idx2 >= 0) {
				paramValue = url.substring(startIdx + pStr.length(), idx2);
				startIdx = url_lower.indexOf(pStr, idx2);
				try {
					paramValue = URLDecoder.decode(paramValue, "UTF-8");
				} catch (UnsupportedEncodingException e) {
				}
				result.add(paramValue);
			} else {
				paramValue = url.substring(startIdx + pStr.length());
				try {
					paramValue = URLDecoder.decode(paramValue, "UTF-8");
				} catch (UnsupportedEncodingException e) {
				}
				result.add(paramValue);
				break;
			}
		}
		return result;
	}

	public static void setClipBoardText(String text) {
		Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(text), null);
	}

	public static String getClipBoardText() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		String readText;
		try {
			readText = (String) t.getTransferData(DataFlavor.stringFlavor);
			return readText;
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
		}
		return "";
	}

	public static String normalizeForVarName(String content) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);
			boolean sectionStart = false;
			boolean isLower = (c >= 'a') && (c <= 'z');
			boolean isUpper = (c >= 'A') && (c <= 'Z');
			boolean isNumber = (c >= '0') && (c <= '9');
			if (isLower || isUpper || isNumber || (c == '_')) {
				if (sectionStart) {
					if (isLower && (result.length() > 0))
						result.append(c + ('A' - 'a'));
					else
						result.append(c);
					sectionStart = false;
				} else
					result.append(c);
			} else
				sectionStart = true;
		}
		return result.toString();
	}

	public static String getFileNameExt(String fileName) {
		int idx = fileName.lastIndexOf('.');
		if ((idx >= 0) && (fileName.length() > 1))
			return fileName.substring(idx + 1);
		else
			return "";
	}

	public static interface OnFormatField {
		public String onFormatField(String fieldName, int columnIndex, ResultSet resultSet);
	}

	public static boolean dumpResultSet(ResultSet resultSet, String sep, String fileName) {
		return dumpResultSet(resultSet, sep, fileName, null);
	}

	public static boolean dumpResultSet(ResultSet resultSet, String sep, String fileName, OnFormatField formatter) {
		StringBuilder lineBuf = new StringBuilder();
		StringList.SimpleWriter writer = new StringList.SimpleWriter(fileName, false);
		long idx = 0;
		try {
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			if (columnCount == 0)
				return false;
			lineBuf.append(metaData.getColumnName(1));
			for (int i = 2; i <= columnCount; i++) {
				lineBuf.append(sep);
				lineBuf.append(metaData.getColumnName(i));
			}
			writer.writeln(lineBuf.toString());
			while (resultSet.next()) {
				lineBuf.setLength(0);
				lineBuf.append(resultSet.getString(1));
				for (int i = 2; i <= columnCount; i++) {
					lineBuf.append(sep);
					String result = null;
					if (formatter != null)
						result = formatter.onFormatField(metaData.getColumnName(i), i, resultSet);
					if (result == null)
						result = resultSet.getString(i);
					lineBuf.append(result);
				}
				writer.writeln(lineBuf.toString());
				if (idx % 10000 == 0) {
					System.out.println("");
					writer.flush();
				}
				if (idx % 100 == 0)
					System.out.print(">");
				idx++;
			}
			writer.flush();
			System.out.println("");
			System.out.println("File [" + fileName + "] exported!");
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/***
	 * This expert will generate setter codes with member reference codes
	 * @param obj
	 */
	public static void s(Object obj, String rootName) {
		String codes;
		if (obj.getClass() == Class.class)
			codes = getObjectSetterCodes((Class<?>) obj, rootName);
		else
			codes = getObjectSetterCodes(obj.getClass(), rootName);
		StringList.setClipBoardText(codes);
	}

	/***
	 * This expert will generate setter codes with aggregated constructing codes
	 * @param obj
	 */
	public static void ss(Object obj, String rootName) {
		String codes;
		if (obj.getClass() == Class.class)
			codes = getObjectSetterCodes2((Class<?>) obj, rootName);
		else
			codes = getObjectSetterCodes2(obj.getClass(), rootName);
		StringList.setClipBoardText(codes);
	}

	/***
	 * This function is trying to extract all the setter functions and their 
	 * arguments into a string, and generate a bunch of 'setXXX(XXX);' codes.
	 * @param obj
	 * @return
	 */
	public static String getObjectSetterCodes(Class<?> clazz, String rootName) {
		class R {
			Map<String, String> dclToImpl = new HashMap<String, String>();
			{
				dclToImpl.put("Map", "HashMap");
				dclToImpl.put("List", "ArrayList");
			}
			List<String> classList = new ArrayList<String>();
			public StringBuilder sbTable = new StringBuilder();
			public StringBuilder sbCode = new StringBuilder();
			public StringBuilder sbObjs = new StringBuilder();
			public StringBuilder sbVars = new StringBuilder();

			boolean isExist(String key) {
				return classList.indexOf(key) >= 0;
			}

			String getShortName(String longName) {
				int idx = longName.lastIndexOf(".");
				if (idx >= 0)
					return longName.substring(idx + 1);
				else
					return "";
			}

			String getMainName(String longName) {
				int idx = longName.indexOf("<");
				if (idx >= 0)
					return getShortName(longName.substring(0, idx));
				else
					return getShortName(longName);
			}

			String getGenericDecl(ParameterizedType paramType, boolean isActual) {
				StringBuilder dcl = new StringBuilder();
				String mainType = getMainName(paramType.toString());
				if (isActual) {
					String mainTypeImpl = dclToImpl.get(mainType);
					if (mainTypeImpl != null)
						mainType = mainTypeImpl;
				}
				dcl.append(mainType).append("<");
				Type[] gtypes = (paramType).getActualTypeArguments();
				for (int j = 0; j < gtypes.length; j++) {
					String tname;
					if (gtypes[j] instanceof ParameterizedType) {
						tname = getGenericDecl((ParameterizedType) gtypes[j], isActual);
					} else {
						tname = gtypes[j].toString();
						tname = getShortName(tname);
					}
					if (j == 0)
						dcl.append(tname);
					else
						dcl.append(", ").append(tname);
				}
				dcl.append(">");
				return dcl.toString();
			}

			public void Recurse(Class<?> clazz, String objName) {
				classList.add(clazz.getName() + "->" + objName);
				if (clazz.getTypeParameters().length == 0)
					sbObjs.append(clazz.getSimpleName() + " " + objName + " = new " + clazz.getSimpleName() + "();\r\n");
				for (Method method : clazz.getMethods()) {
					if (method.getName().startsWith("set")) {
						Class<?>[] types = method.getParameterTypes();
						Type[] paramTypeList = method.getGenericParameterTypes();
						String header;
						String propName = method.getName().substring(3);
						if (propName.equalsIgnoreCase("name")) {
							propName = objName + propName;
							header = StringList.upperFirstChar(propName);
						} else
							header = propName;
						propName = StringList.lowerFirstChar(propName);
						if (types.length == 1) {
							Type paramType = paramTypeList[0];
							Class<?> type = types[0];
							if (Number.class.isAssignableFrom(type)
									|| Enum.class.isAssignableFrom(type)
									|| type == Integer.class
									|| type == Byte.class
									|| type == Long.class
									|| type == Short.class
									|| type == Boolean.class
									|| type == Float.class
									|| type == Double.class
									|| type == int.class
									|| type == byte.class
									|| type == long.class
									|| type == short.class
									|| type == boolean.class
									|| type == float.class
									|| type == double.class
									|| type == Date.class
									|| type == String.class) {
								if (sbTable.length() > 0)
									sbTable.append("\t");
								sbTable.append(header);
								sbVars.append(types[0].getSimpleName() + " " + propName + ", ");
								sbCode.append(objName + "." + method.getName() + "(" + propName + ");\r\n");
							} else if (paramType instanceof ParameterizedType) {
								String gtype = getGenericDecl((ParameterizedType) paramType, false);
								sbObjs.append(gtype + " " + propName + " = " + objName + ".g" + method
										.getName().substring(1) + "();\r\n");
							} else {
								sbObjs.append("//" + types[0].getSimpleName() + " " + propName + " = " + objName + ".g" + method
										.getName().substring(1) + "();\r\n");
								if (!isExist(types[0].getName() + "->" + propName))
									Recurse(types[0], propName);
								sbCode.append(objName + "." + method.getName() + "(" + propName + ");\r\n");
							}
						}
					}
				}
			}
		}
		R r = new R();
		r.Recurse(clazz, rootName);
		return //"//" + r.sbTable.toString() + "\r\n" + r.sbVars.toString() + "\r\n\r\n" + 
		r.sbObjs.toString() + "\r\n" + r.sbCode.toString();
	}

	/***
	 * This function is trying to extract all the setter functions and their 
	 * arguments into a string, and generate a bunch of 'setXXX(XXX);' codes.
	 * @param obj
	 * @return
	 */
	public static String getObjectSetterCodes2(Class<?> clazz, String rootName) {
		class R {
			Map<String, String> dclToImpl = new HashMap<String, String>();
			{
				dclToImpl.put("Map", "HashMap");
				dclToImpl.put("List", "ArrayList");
			}
			List<Class<?>> classList = new ArrayList<Class<?>>();
			public StringBuilder sbTable = new StringBuilder();
			public StringBuilder sbCode = new StringBuilder();
			public StringBuilder sbObjs = new StringBuilder();
			public StringBuilder sbVars = new StringBuilder();

			boolean isExist(Class<?> clazz) {
				for (Class<?> c : classList)
					if (c == clazz)
						return true;
				return false;
			}

			String getShortName(String longName) {
				int idx = longName.lastIndexOf(".");
				if (idx >= 0)
					return longName.substring(idx + 1);
				else
					return "";
			}

			String getMainName(String longName) {
				int idx = longName.indexOf("<");
				if (idx >= 0)
					return getShortName(longName.substring(0, idx));
				else
					return getShortName(longName);
			}

			String getGenericDecl(ParameterizedType paramType, boolean isActual) {
				StringBuilder dcl = new StringBuilder();
				String mainType = getMainName(paramType.toString());
				if (isActual) {
					String mainTypeImpl = dclToImpl.get(mainType);
					if (mainTypeImpl != null)
						mainType = mainTypeImpl;
				}
				dcl.append(mainType).append("<");
				Type[] gtypes = (paramType).getActualTypeArguments();
				for (int j = 0; j < gtypes.length; j++) {
					String tname;
					if (gtypes[j] instanceof ParameterizedType) {
						tname = getGenericDecl((ParameterizedType) gtypes[j], isActual);
					} else {
						tname = gtypes[j].toString();
						tname = getShortName(tname);
					}
					if (j == 0)
						dcl.append(tname);
					else
						dcl.append(", ").append(tname);
				}
				dcl.append(">");
				return dcl.toString();
			}

			public void Recurse(Class<?> clazz, String objName) {
				classList.add(clazz);
				if (clazz.getTypeParameters().length == 0)
					sbObjs.append(clazz.getSimpleName() + " " + objName + " = new " + clazz.getSimpleName() + "();\r\n");
				for (Method method : clazz.getMethods()) {
					if (method.getName().startsWith("set")) {
						Class<?>[] types = method.getParameterTypes();
						Type[] paramTypeList = method.getGenericParameterTypes();
						String header;
						String propName = method.getName().substring(3);
						if (propName.equalsIgnoreCase("name")) {
							propName = objName + propName;
							header = StringList.upperFirstChar(propName);
						} else
							header = propName;
						propName = StringList.lowerFirstChar(propName);
						if (types.length == 1) {
							Type paramType = paramTypeList[0];
							Class<?> type = types[0];
							if (Number.class.isAssignableFrom(type)
									|| Enum.class.isAssignableFrom(type)
									|| type == Integer.class
									|| type == Byte.class
									|| type == Long.class
									|| type == Short.class
									|| type == Boolean.class
									|| type == Float.class
									|| type == Double.class
									|| type == int.class
									|| type == byte.class
									|| type == long.class
									|| type == short.class
									|| type == boolean.class
									|| type == float.class
									|| type == double.class
									|| type == Date.class
									|| type == String.class) {
								if (sbTable.length() > 0)
									sbTable.append("\t");
								sbTable.append(header);
								sbVars.append(types[0].getSimpleName() + " " + propName + ", ");
							} else if (paramType instanceof ParameterizedType) {
								String gtype = getGenericDecl((ParameterizedType) paramType, false);
								String gtypeActual = getGenericDecl((ParameterizedType) paramType, true);
								sbObjs.append(gtype + " " + propName + " = (" + gtype + ")new " + gtypeActual + "();\r\n");
							} else {
								if (!isExist(types[0]))
									Recurse(types[0], propName);
							}
							sbCode.append(objName + "." + method.getName() + "(" + propName + ");\r\n");
						}
					}
				}
			}
		}
		R r = new R();
		r.Recurse(clazz, rootName);
		return //"//" + r.sbTable.toString() + "\r\n" + r.sbVars.toString() + "\r\n\r\n" + 
		r.sbObjs.toString() + "\r\n" + r.sbCode.toString();
	}

	public static void main(String[] args) {

	}
}
