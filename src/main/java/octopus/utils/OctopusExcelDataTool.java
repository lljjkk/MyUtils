package octopus.utils;

import groovy.lang.Binding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;

public class OctopusExcelDataTool extends OctopusScriptedTool {
	public static int MaxColCout = 1024;
	public static int MaxRowCount = 65536;
	public static String defaultCaseTabName = "@Main";
	public Workbook currData;
	public FormulaEvaluator currEvaluator;
	public String currDataFileName = "";
	private final Map<String, List<Map<String, String>>> dataSetPool = new HashMap<String, List<Map<String, String>>>();
	public List<List<String>> caseDataSet = new ArrayList<List<String>>();
	public WriteBack defaultWriteBack = null;
	private String lastCalledBeforeMethod = "";
	public Map<String, String> tempVars = new HashMap<String, String>();

	public boolean isXSSF() {
		return currData instanceof XSSFWorkbook;
	}

	/***
	 * This function is for descendant overriding to read more data from the
	 * spreadsheet file. When overridden, we can read what ever we want from the
	 * spreadsheet file
	 * @author yangdo
	 * @param wb
	 */
	public void loadAdditionalData(Workbook wb) {
	};

	public static class WriteBack extends octopus.utils.WriteBack {

		public WriteBack(Sheet sheet, String keyField, int fromCol, int fromRow) {
			super(sheet, keyField, fromCol, fromRow);
		}
	}

	public class ODataSet<ODataType extends OData> extends ArrayList<ODataType> {
		private static final long serialVersionUID = 6598346337929649764L;

		public boolean containsKey(String key) {
			return keyMap.containsKey(key);
		}

		public boolean containsValue(ODataType value) {
			return keyMap.containsValue(value);
		}

		public ODataType get(String key) {
			return keyMap.get(key);
		}

		@Override
		public void clear() {
			keyMap.clear();
			super.clear();
		}

		public Set<String> keySet() {
			return keyMap.keySet();
		}

		public final Map<String, ODataType> keyMap = new LinkedHashMap<String, ODataType>();

		@Override
		public void add(int index, ODataType element) {
			super.add(index, element);
			keyMap.put(element.key, element);
		}

		@Override
		public boolean add(ODataType e) {
			keyMap.put(e.key, e);
			return super.add(e);
		}

		@Override
		public ODataType remove(int index) {
			ODataType e = get(index);
			keyMap.remove(e.key);
			return super.remove(index);
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(Object o) {
			keyMap.remove(((ODataType) o).key);
			return super.remove(o);
		}
	}

	public static class AreaDataSet extends ArrayList<Map<String, String>> {
		private static final long serialVersionUID = 6332374568632814627L;
		public WriteBack writeBack = null;

		public AreaDataSet(Sheet sheet, int fromCol, int fromRow) {
			super();
			writeBack = new WriteBack(sheet, null, fromCol, fromRow);
		}
	}

	public WriteBack createWriteBack(String sheetName, String keyField, int fromCol, int fromRow) {
		return new WriteBack(currData.getSheet(sheetName), keyField, fromCol, fromRow);
	}

	public WriteBack createWriteBack(String sheetName, int fromCol, int fromRow) {
		return createWriteBack(sheetName, null, fromCol, fromRow);
	}

	public WriteBack createWriteBack(String sheetName, int fromRow) {
		return createWriteBack(sheetName, null, 0, fromRow);
	}

	public WriteBack createWriteBack(String sheetName) {
		return createWriteBack(sheetName, null, 0, 0);
	}

	public WriteBack createWriteBack(String sheetName, String keyField, int fromRow) {
		return createWriteBack(sheetName, keyField, 0, fromRow);
	}

	public WriteBack createWriteBack(String sheetName, String keyField) {
		return createWriteBack(sheetName, keyField, 0, 0);
	}

	public boolean setDataCellValue(String sheet, int col, int row, String value) {
		if (currData == null)
			return false;
		Sheet currSheet = null;
		Row currRow = null;
		try {
			currSheet = currData.getSheet(sheet);
			if (currRow == null)
				currSheet = currData.createSheet(sheet);

			currRow = currSheet.getRow(row);
			if (currRow == null)
				currRow = currSheet.createRow(row);

			Cell cell = currRow.getCell(col);
			if (cell == null)
				cell = currRow.createCell(col);

			cell.setCellValue(value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean setDataCellValue(int page, int col, int row, String value) {
		if (currData == null)
			return false;
		Sheet currSheet = null;
		Row currRow = null;
		try {
			currSheet = currData.getSheetAt(page);
			if (currSheet == null)
				return false;

			currRow = currSheet.getRow(row);
			if (currRow == null)
				currRow = currSheet.createRow(row);

			Cell cell = currRow.getCell(col);
			if (cell == null)
				cell = currRow.createCell(col);

			cell.setCellValue(value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean setDataCellColor(String sheet, int col, int row, Short colorIdx) {
		if (currData == null)
			return false;
		Sheet currSheet = null;
		Row currRow = null;
		try {
			currSheet = currData.getSheet(sheet);
			if (currRow == null)
				currSheet = currData.createSheet(sheet);

			currRow = currSheet.getRow(row);
			if (currRow == null)
				currRow = currSheet.createRow(row);

			Cell cell = currRow.getCell(col);
			if (cell == null)
				cell = currRow.createCell(col);

			CellStyle cellStyle = cell.getCellStyle();
			if (cellStyle == null) {
				cellStyle = currData.createCellStyle();
				cell.setCellStyle(cellStyle);
			}
			cellStyle.setFillBackgroundColor(colorIdx);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean setDataCellColor(int page, int col, int row, Short colorIdx) {
		if (currData == null)
			return false;
		Sheet currSheet = null;
		Row currRow = null;
		try {
			currSheet = currData.getSheetAt(page);
			if (currSheet == null)
				return false;

			currRow = currSheet.getRow(row);
			if (currRow == null)
				currRow = currSheet.createRow(row);

			Cell cell = currRow.getCell(col);
			if (cell == null)
				cell = currRow.createCell(col);

			CellStyle cellStyle = cell.getCellStyle();
			if (cellStyle == null) {
				cellStyle = currData.createCellStyle();
				cell.setCellStyle(cellStyle);
			}
			cellStyle.setFillBackgroundColor(colorIdx);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean setDataCellBorder(String sheet, int col, int row, Boolean withBorder,
			Short colorIdx) {
		if (currData == null)
			return false;
		Sheet currSheet = null;
		Row currRow = null;
		try {
			currSheet = currData.getSheet(sheet);
			if (currRow == null)
				currSheet = currData.createSheet(sheet);

			currRow = currSheet.getRow(row);
			if (currRow == null)
				currRow = currSheet.createRow(row);

			Cell cell = currRow.getCell(col);
			if (cell == null)
				cell = currRow.createCell(col);

			WriteBack.setCellBorder(cell, withBorder, colorIdx);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean setDataCellColor(int page, int col, int row, Boolean withBorder, Short colorIdx) {
		if (currData == null)
			return false;
		Sheet currSheet = null;
		Row currRow = null;
		try {
			currSheet = currData.getSheetAt(page);
			if (currSheet == null)
				return false;

			currRow = currSheet.getRow(row);
			if (currRow == null)
				currRow = currSheet.createRow(row);

			Cell cell = currRow.getCell(col);
			if (cell == null)
				cell = currRow.createCell(col);

			WriteBack.setCellBorder(cell, withBorder, colorIdx);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean setDataTextStyle(String sheet, int fromCol, int fromRow, Short colorIdx,
			Boolean bold, Boolean underline, Boolean strikeOut) {
		if (currData == null)
			return false;
		Sheet currSheet = null;
		Row currRow = null;
		try {
			currSheet = currData.getSheet(sheet);
			if (currRow == null)
				currSheet = currData.createSheet(sheet);

			currRow = currSheet.getRow(fromRow);
			if (currRow == null)
				currRow = currSheet.createRow(fromRow);

			Cell cell = currRow.getCell(fromCol);
			if (cell == null)
				cell = currRow.createCell(fromCol);

			CellStyle cellStyle = currData.createCellStyle();
			cellStyle.cloneStyleFrom(cell.getCellStyle());
			cell.setCellStyle(cellStyle);

			Font oldFont = currData.getFontAt(cellStyle.getFontIndex());
			Font font = currData.createFont();
			WriteBack.cloneFont(font, oldFont);
			cellStyle.setFont(font);
			if (colorIdx != null)
				font.setColor(colorIdx);
			if (bold != null) {
				if (bold)
					font.setBold(true);
				else
					font.setBold(false);
			}
			if (underline != null) {
				if (underline)
					font.setUnderline((byte) 1);
				else
					font.setUnderline((byte) 0);
			}
			if (strikeOut != null)
				font.setStrikeout(strikeOut);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean setDataTextColor(int page, int fromCol, int fromRow, Short colorIdx,
			Boolean bold, Boolean underline, Boolean strikeOut) {
		if (currData == null)
			return false;
		Sheet currSheet = null;
		Row currRow = null;
		try {
			currSheet = currData.getSheetAt(page);
			if (currSheet == null)
				return false;

			currRow = currSheet.getRow(fromRow);
			if (currRow == null)
				currRow = currSheet.createRow(fromRow);

			Cell cell = currRow.getCell(fromCol);
			if (cell == null)
				cell = currRow.createCell(fromCol);

			CellStyle cellStyle = currData.createCellStyle();
			cellStyle.cloneStyleFrom(cell.getCellStyle());
			cell.setCellStyle(cellStyle);

			Font oldFont = currData.getFontAt(cellStyle.getFontIndex());
			Font font = currData.createFont();
			WriteBack.cloneFont(font, oldFont);
			cellStyle.setFont(font);
			if (colorIdx != null)
				font.setColor(colorIdx);
			if (bold != null) {
				if (bold)
					font.setBold(true);
				else
					font.setBold(false);
			}
			if (underline != null) {
				if (underline)
					font.setUnderline((byte) 1);
				else
					font.setUnderline((byte) 0);
			}
			if (strikeOut != null)
				font.setStrikeout(strikeOut);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public List<Map<String, String>> readDataSetFromDS(DataSource ds) {
		Sheet sheet = currData.getSheet(ds.sheet());
		if (sheet == null)
			throw new RuntimeException("Sheet [" + ds.sheet() + "] not found!");
		if (ds != null)
			switch (ds.mode()) {
			case horizontal:
				return readDataSetFromSS(sheet, ds.col(), ds.row());
			case vertical:
				return readDataSetVertical(sheet, ds.col(), ds.row());
			case verticalStandalone:
				return readDataSetVertical2(sheet, ds.col(), ds.row());
			}
		return null;
	}

	public class OData {
		public Map<String, String> data;
		public String key = "";

		public void loadFromMap(Map<String, String> map, String keyField) {
			data = map;
			int count = StringList.loadFields(this, data);
			if ((keyField != null) && !keyField.equals("key")) {
				String keyValue = null;
				keyValue = map.get(StringList.upperFirstChar(keyField));
				if (keyValue == null) {
					keyValue = map.get(keyField);
					if (keyValue == null)
						keyValue = map.get(keyField.toUpperCase());
				}
				if (keyValue != null)
					key = keyValue;
			}
			if ((count <= 1) && !data.isEmpty()) {
				f(data);
				System.out.println("============================================================");
				System.out.println("=============(^@^): Data class fields generated!============");
				System.out.println("============================================================");
			}
			afterLoaded();
		}

		public void loadFromArea(String sheet, int fromCol, int fromRow) {
			loadFromMap(readMapFromSS(currData, sheet, fromRow, fromCol), "key");
		}

		public void afterLoaded() {
		}

		@SuppressWarnings("unchecked")
		public void loadAnnotatedFields() {
			Class<?> clazz = getClass();
			while (clazz != Object.class) {
				Field[] fields = clazz.getDeclaredFields();
				clazz = clazz.getSuperclass();
				for (Field field : fields) {
					try {
						Class<?> fieldType = field.getType();
						Class<?> compType = fieldType.getComponentType();
						if (fieldType.isArray() && OData.class.isAssignableFrom(compType)) {
							field.setAccessible(true);
							if (field.isAnnotationPresent(DataSource.class)) {
								DataSource ds = field.getAnnotation(DataSource.class);
								List<Map<String, String>> data = readDataSetFromDS(ds);
								Object objArray = Array.newInstance(compType, data.size());
								field.set(this, objArray);
								for (int i = 0; i < data.size(); i++) {
									try {
										OData child = (OData) compType
												.getConstructors()[0]
												.newInstance(OctopusExcelDataTool.this);
										Array.set(objArray, i, child);
										child.loadFromMap(data.get(i), ds.keyField());
										child.loadAnnotatedFields();
									} catch (InstantiationException e) {
										e.printStackTrace();
									} catch (InvocationTargetException e) {
										e.printStackTrace();
									} catch (SecurityException e) {
										e.printStackTrace();
									}
								}
							}
						} else if (OData.class.isAssignableFrom(fieldType)) {
							field.setAccessible(true);
							OData child = (OData) field.get(this);
							if (child == null) {
								try {
									child = (OData) fieldType.getConstructors()[0]
											.newInstance(OctopusExcelDataTool.this);
									field.set(this, child);
								} catch (InstantiationException e) {
									e.printStackTrace();
								} catch (InvocationTargetException e) {
									e.printStackTrace();
								} catch (SecurityException e) {
									e.printStackTrace();
								}
							}
							child.loadAnnotatedFields();
							if (field.isAnnotationPresent(DataSource.class)) {
								DataSource ds = field.getAnnotation(DataSource.class);
								child.loadFromArea(ds.sheet(), ds.col(), ds.row());
							}
						} else if (ODataSet.class.isAssignableFrom(fieldType)) {
							field.setAccessible(true);
							if (field.isAnnotationPresent(DataSource.class)) {
								DataSource ds = field.getAnnotation(DataSource.class);
								List<Map<String, String>> data = readDataSetFromDS(ds);
								ODataSet<OData> dataSet;
								try {
									dataSet = (ODataSet<OData>) fieldType
											.getConstructors()[0].newInstance(OctopusExcelDataTool.this);
									ParameterizedType genParamType = (ParameterizedType) field
											.getGenericType();
									Class<?> pType = (Class<?>) genParamType.getActualTypeArguments()[0];
									field.set(this, dataSet);
									for (int i = 0; i < data.size(); i++) {
										try {
											OData child = (OData) pType.getConstructors()[0]
													.newInstance(OctopusExcelDataTool.this);
											child.loadFromMap(data.get(i), ds.keyField());
											child.loadAnnotatedFields();
											dataSet.add(child);
										} catch (InstantiationException e) {
											e.printStackTrace();
										} catch (InvocationTargetException e) {
											e.printStackTrace();
										} catch (SecurityException e) {
											e.printStackTrace();
										}
									}
								} catch (InstantiationException e1) {
									e1.printStackTrace();
								} catch (InvocationTargetException e1) {
									e1.printStackTrace();
								} catch (SecurityException e1) {
									e1.printStackTrace();
								}
							}
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void loadAnnotatedFields() {
		Class<?> clazz = getClass();
		while (clazz != Object.class) {
			Field[] fields = clazz.getDeclaredFields();
			clazz = clazz.getSuperclass();
			for (Field field : fields) {
				try {
					Class<?> fieldType = field.getType();
					Class<?> compType = fieldType.getComponentType();
					if (fieldType.isArray() && OData.class.isAssignableFrom(compType)) {
						field.setAccessible(true);
						if (field.isAnnotationPresent(DataSource.class)) {
							DataSource ds = field.getAnnotation(DataSource.class);
							List<Map<String, String>> data = readDataSetFromDS(ds);
							Object objArray = Array.newInstance(compType, data.size());
							field.set(this, objArray);
							for (int i = 0; i < data.size(); i++) {
								try {
									OData child = (OData) compType
											.getConstructors()[0]
											.newInstance(OctopusExcelDataTool.this);
									Array.set(objArray, i, child);
									child.loadFromMap(data.get(i), ds.keyField());
									child.loadAnnotatedFields();
								} catch (InstantiationException e) {
									e.printStackTrace();
								} catch (InvocationTargetException e) {
									e.printStackTrace();
								} catch (SecurityException e) {
									e.printStackTrace();
								}
							}
						}
					} else if (OData.class.isAssignableFrom(fieldType)) {
						field.setAccessible(true);
						OData child = (OData) field.get(this);
						if (child == null) {
							try {
								child = (OData) fieldType.getConstructors()[0].newInstance(this);
								field.set(this, child);
							} catch (InstantiationException e) {
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							} catch (SecurityException e) {
								e.printStackTrace();
							}
						}
						child.loadAnnotatedFields();
						if (field.isAnnotationPresent(DataSource.class)) {
							DataSource ds = field.getAnnotation(DataSource.class);
							child.loadFromArea(ds.sheet(), ds.col(), ds.row());
						}
					} else if (ODataSet.class.isAssignableFrom(fieldType)) {
						field.setAccessible(true);
						if (field.isAnnotationPresent(DataSource.class)) {
							DataSource ds = field.getAnnotation(DataSource.class);
							List<Map<String, String>> data = readDataSetFromDS(ds);
							ODataSet<OData> dataSet;
							try {
								dataSet = (ODataSet<OData>) fieldType
										.getConstructors()[0].newInstance(OctopusExcelDataTool.this);
								ParameterizedType genParamType = (ParameterizedType) field.getGenericType();
								Class<?> pType = (Class<?>) genParamType.getActualTypeArguments()[0];
								field.set(this, dataSet);
								for (int i = 0; i < data.size(); i++) {
									try {
										OData child = (OData) pType.getConstructors()[0]
												.newInstance(OctopusExcelDataTool.this);
										child.loadFromMap(data.get(i), ds.keyField());
										child.loadAnnotatedFields();
										dataSet.add(child);
									} catch (InstantiationException e) {
										e.printStackTrace();
									} catch (InvocationTargetException e) {
										e.printStackTrace();
									} catch (SecurityException e) {
										e.printStackTrace();
									}
								}
							} catch (InstantiationException e1) {
								e1.printStackTrace();
							} catch (InvocationTargetException e1) {
								e1.printStackTrace();
							} catch (SecurityException e1) {
								e1.printStackTrace();
							}
						}
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/***
	 * Get cell value from the default data file
	 * @param sheet
	 * @param col
	 * @param row
	 * @return
	 */
	public String getString(String sheet, int col, int row) {
		Sheet sheetObj = currData.getSheet(sheet);
		if (sheetObj == null)
			return null;
		return getCellValueAsString(sheetObj.getRow(row).getCell(col), currEvaluator);
	}

	/***
	 * Get cell value from the default data file
	 * @param sheetIdx
	 * @param col
	 * @param row
	 * @return
	 */
	public String getString(int sheetIdx, int col, int row) {
		Sheet sheetObj = currData.getSheetAt(sheetIdx);
		if (sheetObj == null)
			return null;
		return getCellValueAsString(sheetObj.getRow(row).getCell(col), currEvaluator);
	}

	/***
	 * Read a spread sheet's cell's value as a string
	 * @param cell The cell object
	 * @return
	 */
	public static String getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
		if (cell == null)
			return "";
		switch (cell.getCellTypeEnum()) {
		case BOOLEAN:
			return cell.getBooleanCellValue() ? "TRUE" : "FALSE";
		case NUMERIC: {
			DecimalFormat df = new DecimalFormat("######0.###");
			return df.format(cell.getNumericCellValue());
		}
		case STRING:
			return cell.getStringCellValue();
		case FORMULA: {
			if (evaluator == null)
				return null;
			CellValue cellValue = evaluator.evaluate(cell);
			if (cellValue == null)
				return null;
			String result = null;
			switch (cellValue.getCellTypeEnum()) {
			case BOOLEAN:
				result = cellValue.getBooleanValue() ? "TRUE" : "FALSE";
				break;
			case NUMERIC:
				DecimalFormat df = new DecimalFormat("######0.###");
				result = df.format(cellValue.getNumberValue());
				break;
			case STRING:
				result = cellValue.getStringValue();
				break;
			default:
				throw new RuntimeException(
						"(^@^):Not recognized data format:\r\n"
								+ "[" + cell.getSheet().getSheetName()
								+ "]/Row:" + cell.getRowIndex() + "/Col:"
								+ cell.getColumnIndex());
			}
			return result;
		}
		default:
			return cell.getStringCellValue();
		}
	}

	/***
	 * Read a matrix area from excel file
	 * @author yangdo
	 * @param sheet Tab sheet object to read
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit The upper boundary limit of row number
	 * @return The name-value pair map list
	 */
	public static AreaDataSet readDataSetFromSS(Sheet sheet,
			int fromCol, int fromRow, int upperRowLimit) {
		if (upperRowLimit <= 0)
			upperRowLimit = MaxRowCount;
		List<String> columns = new ArrayList<String>();
		AreaDataSet result = new AreaDataSet(sheet, fromCol, fromRow);

		Row row;
		Cell cell;
		if (sheet != null) {
			FormulaEvaluator evaluator;
			if (sheet instanceof XSSFSheet)
				evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) sheet.getWorkbook());
			else
				evaluator = new HSSFFormulaEvaluator((HSSFWorkbook) sheet.getWorkbook());

			row = sheet.getRow(fromRow);
			if (row != null) {
				for (int i = fromCol; i <= MaxColCout; i++) {
					cell = row.getCell(i);
					if (cell == null)
						break;
					String cellData = getCellValueAsString(cell, evaluator);
					if (cellData.isEmpty())
						break;
					cellData = StringList.normalizeForVarName(cellData);
					columns.add(cellData);
				}
				for (int j = fromRow + 1; j < upperRowLimit; j++) {
					try {
						row = sheet.getRow(j);
						if (row == null)
							break;
						cell = row.getCell(fromCol);
						if (cell == null)
							break;
						String rowKey = getCellValueAsString(cell, evaluator);
						if (rowKey.trim().isEmpty())
							break;
						else {
							Map<String, String> map = new LinkedHashMap<String, String>();
							result.add(map);
							for (int k = 0; k < columns.size(); k++) {
								cell = row.getCell(fromCol + k);
								String cellData = "";
								if (cell != null)
									cellData = getCellValueAsString(cell, evaluator);
								map.put(columns.get(k), cellData);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
				}
			}
		}
		return result;
	}

	/***
	 * Read a map vertically from an area in the spread sheet
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @return
	 */
	public static List<Map<String, String>> readDataSetVertical(Sheet sheet,
			int fromCol, int fromRow) {
		int upperRowLimit = MaxRowCount;
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();

		Row keyRow;
		Map<String, String> map;
		if (sheet != null) {
			FormulaEvaluator evaluator;
			if (sheet instanceof XSSFSheet)
				evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) sheet.getWorkbook());
			else
				evaluator = new HSSFFormulaEvaluator((HSSFWorkbook) sheet.getWorkbook());

			int offset = 0;
			String keyValue = null;
			keyRow = sheet.getRow(fromRow);
			if (keyRow != null)
				keyValue = getCellValueAsString(keyRow
						.getCell(fromCol + 1 + offset), evaluator);

			boolean hasNext = (keyValue != null) && (!keyValue.trim().isEmpty());
			while (hasNext) {
				map = new LinkedHashMap<String, String>();
				Row row;
				for (int j = fromRow; j < upperRowLimit; j++) {
					try {
						row = sheet.getRow(j);
						if (row == null)
							break;
						Cell headerCell = row.getCell(fromCol);
						if (headerCell == null)
							break;
						String aName = StringList.normalizeForVarName(getCellValueAsString(headerCell, evaluator));
						String aValue = getCellValueAsString(row
								.getCell(fromCol + 1 + offset), evaluator);

						if (aName.trim().isEmpty())
							break;
						else
							map.put(aName, aValue);
					} catch (Exception e) {
						break;
					}
				}
				result.add(map);
				offset++;
				keyValue = getCellValueAsString(keyRow
						.getCell(fromCol + 1 + offset), evaluator);
				hasNext = (keyValue != null) && (!keyValue.trim().isEmpty());
			}
		}
		return result;
	}

	/***
	 * Read a map vertically from an area in the spread sheet. This function assume
	 * there's standalone header column on the left side of each value column
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @return
	 */
	public static List<Map<String, String>> readDataSetVertical2(Sheet sheet,
			int fromCol, int fromRow) {
		int upperRowLimit = MaxRowCount;
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();

		Row keyRow;
		Map<String, String> map;
		if (sheet != null) {
			FormulaEvaluator evaluator;
			if (sheet instanceof XSSFSheet)
				evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) sheet.getWorkbook());
			else
				evaluator = new HSSFFormulaEvaluator((HSSFWorkbook) sheet.getWorkbook());

			int offset = 0;
			String keyHeader = null;
			keyRow = sheet.getRow(fromRow);
			if (keyRow != null)
				keyHeader = getCellValueAsString(keyRow
						.getCell(fromCol + (offset * 2)), evaluator);

			boolean hasNext = (keyHeader != null) && (!keyHeader.trim().isEmpty());
			while (hasNext) {
				map = new LinkedHashMap<String, String>();
				Row row;
				for (int j = fromRow; j < upperRowLimit; j++) {
					try {
						row = sheet.getRow(j);
						if (row == null)
							break;
						Cell headerCell = row.getCell(fromCol + (offset * 2));
						if (headerCell == null)
							break;
						String aName = StringList.normalizeForVarName(getCellValueAsString(headerCell, evaluator));
						String aValue = getCellValueAsString(row
								.getCell(fromCol + (offset * 2) + 1), evaluator);

						if (aName.trim().isEmpty())
							break;
						else
							map.put(aName, aValue);
					} catch (Exception e) {
						break;
					}
				}
				result.add(map);
				offset++;
				keyHeader = getCellValueAsString(keyRow
						.getCell(fromCol + (offset * 2)), evaluator);
				hasNext = (keyHeader != null) && (!keyHeader.trim().isEmpty());
			}
		}
		return result;
	}

	/***
	 * Read a matrix area from excel file
	 * @author yangdo
	 * @param wb The work book object
	 * @param page Page number
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return The name-value pair map list
	 */
	public static List<Map<String, String>> readDataSetFromSS(Workbook wb,
			int page, int fromCol, int fromRow) {
		return readDataSetFromSS(wb.getSheetAt(page), fromCol, fromRow);
	}

	/***
	 * Read a matrix area from excel file
	 * @author yangdo
	 * @param sheet Tab sheet object to read
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return The name-value pair map list
	 */
	public static AreaDataSet readDataSetFromSS(Sheet sheet,
			int fromCol, int fromRow) {
		return readDataSetFromSS(sheet, fromCol, fromRow, -1);
	}

	/***
	 * Read a matrix area from excel file
	 * @author yangdo
	 * @param wb The work book object
	 * @param page Page number
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit The upper boundary limit of row number
	 * @return The name-value pair map list
	 */
	public static AreaDataSet readDataSetFromSS(Workbook wb,
			int page, int fromCol, int fromRow, int upperRowLimit) {
		return readDataSetFromSS(wb.getSheetAt(page), fromCol, fromRow,
				upperRowLimit);
	}

	/***
	 * Typicall Usage:
	 * loopDataSetFromSS(yourMap, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSet(List<Map<String, String>> dataset, Looper looper) {
		if (dataset == null)
			return false;

		if (looper == null) {
			if (dataset.size() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append(", new Looper() {\r\n")
						.append(StringList.getJavaDclFromMap(dataset.get(0), ""))
						.append("public void loop(){\r\n\r\n")
						.append("}\r\n}");
				StringList.setClipBoardText(sb.toString());
				System.out.println("============================================================");
				System.out.println("===========(^@^): Following code is in clipboard.===========");
				System.out.println("============================================================");
				System.out.print(sb.toString());
				System.out.println("");
				System.out.println("");
			}
			return false;
		}

		Method method = null;
		for (Method _method : looper.getClass().getMethods()) {
			if (_method.getName().equals("loop")) {
				method = _method;
				method.setAccessible(true);
				break;
			}
		}

		if ((dataset != null) && (method != null)) {
			Object[] params = new Object[0];
			looper.doContinue = true;
			looper.before();

			if (looper.shell == null) {
				looper.shell = new SnippetRunner(looper);
				if (dataset.getClass().isAssignableFrom(AreaDataSet.class))
					looper.shell.writeBack = ((AreaDataSet) dataset).writeBack;
			}
			Binding context = looper.shell.getContext();
			looper.initSnippetContext(context);

			for (int index = 0; index < dataset.size(); index++) {
				Map<String, String> row = dataset.get(index);
				int count = StringList.loadFields(looper, row);

				if (count == 0) {
					f(row);
					System.out.println("============================================================");
					System.out.println("=============(^@^): Data class fields generated!============");
					System.out.println("============================================================");
					return false;
				}
				looper.index = index;
				looper.shell.writeBack.gotoIndex(index);
				try {
					method.invoke(looper, params);
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
		return false;
	}

	/***
	 * Typicall Usage:
	 * loopDataSetFromSS(wb.getSheet("Sheet1"), 0, 0,-1, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSetFromSS(Sheet sheet,
			int fromCol, int fromRow, int upperRowLimit, Looper looper) {
		if (sheet == null)
			return false;
		return loopDataSet(readDataSetFromSS(sheet, fromCol, fromRow,
				upperRowLimit), looper);
	}

	/***
	 * Load Excel file by file name
	 * @param fileName
	 * @return
	 */
	public static Workbook loadWorkBook(String fileName) {
		Workbook wb = null;
		File dataFile = new File(fileName);
		if (dataFile.exists())
			try {
				try {
					String ext = StringList.getFileNameExt(fileName).toLowerCase();
					if (ext.equals(".xls"))
						wb = new HSSFWorkbook(new FileInputStream(dataFile));
					else if (ext.equals(".xlsx"))
						wb = new XSSFWorkbook(new FileInputStream(dataFile));
				} catch (Exception e) {
					try {
						wb = new XSSFWorkbook(new FileInputStream(dataFile));
					} catch (Exception ex) {
						wb = new HSSFWorkbook(new FileInputStream(dataFile));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		return wb;
	}

	public static boolean loopDataSetFromSS(String fileName, int page, int fromCol, int fromRow,
			Looper looper) {
		try {
			Workbook wb = loadWorkBook(fileName);
			if (wb == null)
				return false;
			return loopDataSetFromSS(wb.getSheetAt(page), fromCol, fromRow, -1, looper);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/***
	 * loopDataSetFromSS(YourFile, "Sheet1", 0, 0, new Looper() {
	 * 
	 * });
	 * @param fileName
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSetFromSS(String fileName, String pageName, int fromCol, int fromRow,
			Looper looper) {
		return loopDataSetFromSS(fileName, pageName, fromCol, fromRow, -1, looper);
	}

	/***
	 * loopDataSetFromSS(YourFile, "Sheet1", 0, 0, -1, new Looper() {
	 * 
	 * });
	 * @param fileName
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSetFromSS(String fileName,
			String pageName, int fromCol, int fromRow, int upperRowLimit, Looper looper) {
		Workbook wb;
		try {
			wb = loadWorkBook(fileName);
			if (wb == null)
				return false;
			Sheet sheet = wb.getSheet(pageName);
			if (sheet == null)
				throw new RuntimeException("Sheet [" + pageName + "] not found!");
			return loopDataSetFromSS(sheet, fromCol, fromRow, upperRowLimit, looper);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/***
	 * Typicall Usage:
	 * loopDataSetFromSS(wb.getSheet("Sheet1"), 0, 0, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSetFromSS(Sheet sheet,
			int fromCol, int fromRow, Looper looper) {
		return loopDataSetFromSS(sheet, fromCol, fromRow, -1, looper);
	}

	/***
	 * Typical Usage:
	 * loopDataArea("Sheet1", 0, 0, new Looper() {
	 * 
	 * });
	 * This function will loop from column 0
	 * @param sheet
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public boolean loopArea(String pageName, int fromRow, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSetFromSS(sheet, 0, fromRow, -1, looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 * @param fromRow
	 * @return
	 */
	public void loopArea(String pageName, int fromRow) {
		loopArea(pageName, fromRow, null);
	}

	/***
	 * Typical Usage:
	 * loopDataArea("Sheet1", 0, 0, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public boolean loopArea(String pageName, int fromCol, int fromRow, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSetFromSS(sheet, fromCol, fromRow, -1, looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 */
	public void loopArea(String pageName, int fromCol, int fromRow) {
		loopArea(pageName, fromCol, fromRow, null);
	}

	/***
	 * Typical Usage:
	 * loopAreaVert("Sheet1", 0, 0, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public boolean loopAreaVert(String pageName, int fromCol, int fromRow, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSet(readDataSetVertical(sheet, fromCol, fromRow), looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 */
	public void loopAreaVert(String pageName, int fromCol, int fromRow) {
		loopAreaVert(pageName, fromCol, fromRow, null);
	}

	/***
	 * Typical Usage:
	 * loopAreaVert("Sheet1", 0, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public boolean loopAreaVert(String pageName, int fromRow, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSet(readDataSetVertical(sheet, 0, fromRow), looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 */
	public void loopAreaVert(String pageName, int fromRow) {
		loopAreaVert(pageName, fromRow, null);
	}

	/***
	 * Typical Usage:
	 * loopSheetVert("Sheet1", new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public boolean loopSheetVert(String pageName, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSet(readDataSetVertical(sheet, 0, 0), looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 */
	public void loopSheetVert(String pageName) {
		loopSheetVert(pageName, null);
	}

	/***
	 * Typical Usage:
	 * loopSheet("MySheet", new Looper() {
	 * 
	 * });
	 * @param page
	 * @param looper
	 * @return
	 */
	public boolean loopSheet(String pageName, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSetFromSS(sheet, 0, 0, -1, looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 */
	public void loopSheet(String pageName) {
		loopSheet(pageName, null);
	}

	/***
	 * Typical Usage:
	 * loopSheet(sheetIdx, new Looper() {
	 * 
	 * });
	 * @param page
	 * @param looper
	 * @return
	 */
	public boolean loopSheet(int page, Looper looper) {
		return loopDataSetFromSS(currData.getSheetAt(page), 0, 0, -1, looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param page
	 */
	public void loopSheet(int page) {
		loopSheet(page, null);
	}

	/***
	 * Typicall Usage:
	 * loopDataSetFromSS2(wb.getSheet("Sheet1"), 0, 0,-1, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSetFromSS2(Sheet sheet,
			int fromCol, int fromRow, int upperRowLimit, Looper looper) {
		if (sheet == null)
			return false;

		AreaDataSet dataset = readDataSetFromSS(sheet, fromCol, fromRow,
				upperRowLimit);
		if (dataset == null)
			return false;

		if (looper == null) {
			StringBuilder sb = new StringBuilder();
			sb.append(", new Looper() {\r\n")
					.append("public void loop(")
					.append("int index, ")
					.append(getProposedParams3(sheet, fromCol, fromRow))
					.append("){\r\n\r\n")
					.append("}\r\n}");
			StringList.setClipBoardText(sb.toString());
			System.out.println("============================================================");
			System.out.println("=============(^@^): Please copy following codes.============");
			System.out.println("============================================================");
			System.out.print(sb.toString());
			System.out.println("");
			System.out.println("");
			return false;
		}

		Method method = null;
		for (Method _method : looper.getClass().getMethods()) {
			if (_method.getName().equals("loop")) {
				method = _method;
				method.setAccessible(true);
				break;
			}
		}
		if (method == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("@SuppressWarnings(\"unused\")\r\n")
					.append("public void loop(")
					.append("int index, ")
					.append(getProposedParams3(sheet, fromCol, fromRow))
					.append("){\r\n")
					.append("\r\n")
					.append("}");
			StringList.setClipBoardText(sb.toString());
			System.out.println("============================================================");
			System.out.println("=============(^@^): Please copy following codes.============");
			System.out.println("============================================================");
			System.out.print(sb.toString());
			System.out.println("");
			System.out.println("");
			return false;
		}

		if (method.getParameterTypes().length == 0) {
			System.out.println("============================================================");
			System.out.println("=============(^@^): Please copy following params.===========");
			System.out.println("============================================================");
			System.out.println("");
			System.out.println("");
			String params = getProposedParams3(sheet, fromCol, fromRow);
			System.out.println(params);
			StringList.setClipBoardText(params);
		}

		Class<?>[] types = method.getParameterTypes();
		int pCount = types.length;
		Object[] params = null;
		boolean useMapAsParam = (pCount == 1) && (types[0] == Map.class);

		if (dataset != null) {
			ClassPool pool = ClassPool.getDefault();
			try {
				CtClass cc = pool.get(looper.getClass().getName());
				CtClass[] ctTypes = new CtClass[pCount];
				for (int i = 0; i < pCount; i++)
					ctTypes[i] = pool.get(types[i].getName());
				CtMethod cm = cc.getDeclaredMethod(method.getName(), ctTypes);
				MethodInfo methodInfo = cm.getMethodInfo();
				CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
				LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
						.getAttribute(LocalVariableAttribute.tag);
				looper.doContinue = true;
				looper.before();
				if (looper.shell == null) {
					looper.shell = new SnippetRunner(looper);
					if (dataset.getClass().isAssignableFrom(AreaDataSet.class))
						looper.shell.writeBack = ((AreaDataSet) dataset).writeBack;
				}
				for (int index = 0; index < dataset.size(); index++) {
					Map<String, String> row = dataset.get(index);
					if (useMapAsParam) {
						params = new Object[1];
						params[0] = row;
					} else {
						if (pCount == 0)
							continue;
						int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
						params = new Object[pCount];
						for (int i = 0; i < pCount; i++) {
							String cellValue = row.get(attr.variableName(i + pos));
							if (cellValue == null)
								cellValue = row.get(StringList.upperFirstChar(attr.variableName(i + pos)));
							Class<?> t = types[i];
							try {
								if ((cellValue == null) && attr.variableName(i + pos)
										.equals("index")) {
									params[i] = index;
								} else if (t == SnippetRunner.class) {
									SnippetRunner shell = new SnippetRunner(looper);
									shell.writeBack = looper.shell.writeBack;
									shell.writeBack.gotoIndex(index);
									params[i] = shell;
								} else if (t == int.class) {
									if ((cellValue == null) || cellValue.trim().isEmpty())
										cellValue = "0";
									params[i] = Integer.parseInt(cellValue.trim());
								} else if (t == long.class) {
									if ((cellValue == null) || cellValue.trim().isEmpty())
										cellValue = "0";
									params[i] = Long.parseLong(cellValue.trim());
								} else if (t == byte.class) {
									if ((cellValue == null) || cellValue.trim().isEmpty())
										cellValue = "0";
									params[i] = Byte.parseByte(cellValue.trim());
								} else if (t == short.class) {
									if ((cellValue == null) || cellValue.trim().isEmpty())
										cellValue = "0";
									params[i] = Short.parseShort(cellValue.trim());
								} else if (t == boolean.class) {
									if ((cellValue == null) || cellValue.trim().isEmpty())
										cellValue = "false";
									params[i] = Boolean.parseBoolean(cellValue.trim());
								} else if (t == double.class) {
									if ((cellValue == null) || cellValue.trim().isEmpty())
										cellValue = "0";
									params[i] = Double.parseDouble(cellValue.trim());
								} else if (t == float.class) {
									if ((cellValue == null) || cellValue.trim().isEmpty())
										cellValue = "0";
									params[i] = Float.parseFloat(cellValue.trim());
								} else
									params[i] = cellValue;
							} catch (RuntimeException e) {
								System.out.println("Invalid field value for field [" +
										attr.variableName(i + pos) + "]:\r\n" + cellValue);
								throw e;
							}
						}
						for (int i = 0; i < pCount; i++)
							if (types[i] == SnippetRunner.class) {
								Binding context = ((SnippetRunner) params[i]).getContext();
								for (int j = 0; j < pCount; j++) {
									String varName = attr.variableName(j + pos);
									if (types[j] != SnippetRunner.class)
										context.setVariable(varName, params[j]);
								}
								looper.initSnippetContext(context);
							}

						Binding context = looper.shell.getContext();
						for (int j = 0; j < pCount; j++) {
							String varName = attr.variableName(j + pos);
							if (types[j] != SnippetRunner.class)
								context.setVariable(varName, params[j]);
						}
						looper.initSnippetContext(context);

					}
					try {
						looper.shell.writeBack.gotoIndex(index);
						method.invoke(looper, params);
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
			} catch (NotFoundException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	public static boolean loopDataSetFromSS2(String fileName, int page, int fromCol, int fromRow,
			Looper looper) {
		Workbook wb;
		try {
			wb = loadWorkBook(fileName);
			if (wb == null)
				return false;
			return loopDataSetFromSS2(wb.getSheetAt(page), fromCol, fromRow, -1, looper);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/***
	 * loopDataSetFromSS2(YourFile, "Sheet1", 0, 0, new Looper() {
	 * 
	 * });
	 * @param fileName
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSetFromSS2(String fileName, String pageName, int fromCol, int fromRow,
			Looper looper) {
		return loopDataSetFromSS2(fileName, pageName, fromCol, fromRow, -1, looper);
	}

	/***
	 * loopDataSetFromSS2(YourFile, "Sheet1", 0, 0, -1, new Looper() {
	 * 
	 * });
	 * @param fileName
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSetFromSS2(String fileName,
			String pageName, int fromCol, int fromRow, int upperRowLimit, Looper looper) {
		try {
			Workbook wb = loadWorkBook(fileName);
			if (wb == null)
				return false;
			Sheet sheet = wb.getSheet(pageName);
			if (sheet == null)
				throw new RuntimeException("Sheet [" + pageName + "] not found!");
			return loopDataSetFromSS2(sheet, fromCol, fromRow, upperRowLimit, looper);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/***
	 * Typicall Usage:
	 * loopDataSetFromSS2(wb.getSheet("Sheet1"), 0, 0, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public static boolean loopDataSetFromSS2(Sheet sheet,
			int fromCol, int fromRow, Looper looper) {
		return loopDataSetFromSS2(sheet, fromCol, fromRow, -1, looper);
	}

	/***
	 * Typical Usage:
	 * loopDataArea("Sheet1", 0, 0, new Looper() {
	 * 
	 * });
	 * This function will loop from column 0
	 * @param sheet
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public boolean loopArea2(String pageName, int fromRow, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSetFromSS2(sheet, 0, fromRow, -1, looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 * @param fromRow
	 * @return
	 */
	public void loopArea2(String pageName, int fromRow) {
		loopArea2(pageName, fromRow, null);
	}

	/***
	 * Typical Usage:
	 * loopDataArea("Sheet1", 0, 0, new Looper() {
	 * 
	 * });
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @param upperRowLimit
	 * @param looper
	 * @return
	 */
	public boolean loopArea2(String pageName, int fromCol, int fromRow, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSetFromSS2(sheet, fromCol, fromRow, -1, looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 */
	public void loopArea2(String pageName, int fromCol, int fromRow) {
		loopArea2(pageName, fromCol, fromRow, null);
	}

	public boolean loopSheet2(String pageName, Looper looper) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return loopDataSetFromSS2(sheet, 0, 0, -1, looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param pageName
	 */
	public void loopSheet2(String pageName) {
		loopSheet2(pageName, null);
	}

	public boolean loopSheet2(int page, Looper looper) {
		return loopDataSetFromSS2(currData.getSheetAt(page), 0, 0, -1, looper);
	}

	/***
	 * Wizard function that produce looper codes.
	 * @param page
	 */
	public void loopSheet2(int page) {
		loopSheet2(page, null);
	}

	/***
	 * This function is trying to extract all the setter functions and their 
	 * arguments into a string, and generate a bunch of 'setXXX(XXX);' codes.
	 * @param obj
	 * @return
	 */
	public void s() {
		String fieldName = StringList.getClipBoardText();
		Class<?> clazz = getClass();
		while (clazz != Object.class) {
			Field[] fields = clazz.getDeclaredFields();
			clazz = clazz.getSuperclass();
			for (Field field : fields) {
				if (field.getName().equals(fieldName)) {
					try {
						s(field.get(this), fieldName);
					} catch (IllegalArgumentException e) {
					} catch (IllegalAccessException e) {
					}
					break;
				}
			}
		}
	}

	/***
	 * This is a wizard function to generate a field declaration list in clip board.
	 */
	public void f(String sheet, int fromCol, int fromRow) {
		Map<String, String> map = readMapFromSS(currData, sheet, fromRow, fromCol);
		f(map);
	}

	/***
	 * This function is trying to extract all the setter functions and their 
	 * arguments into a string, and generate a bunch of 'setXXX(XXX);' codes.
	 * @param obj
	 * @return
	 */
	public static void s(Class<?> clazz) {
		StringList.setClipBoardText(getObjectSetterCodes(clazz));
	}

	/***
	 * This function is trying to extract all the setter functions and their 
	 * arguments into a string, and generate a bunch of 'setXXX(XXX);' codes.
	 * @param obj
	 * @return
	 */
	public static void s(Object obj) {
		s(obj, "");
	}

	/***
	 * This function is trying to extract all the setter functions and their 
	 * arguments into a string, and generate a bunch of 'setXXX(XXX);' codes.
	 * @param obj
	 * @return
	 */
	public static void s(Object obj, String fieldName) {
		for (Method method : obj.getClass().getMethods()) {
			if (method.getName().equals("call")) {
				Class<?>[] types = method.getParameterTypes();
				if (types.length == 1) {
					Class<?> type = types[0];
					if (type.getSimpleName().equals("Envelope"))
						continue;
					String codes = getObjectSetterCodes(type);
					StringList
							.setClipBoardText(codes + "\r\n"
									+ method.getReturnType().getSimpleName()
									+ " resp = " + fieldName + "call.call(request);");
				}
				break;
			}
		}
	}

	/***
	 * This function is trying to extract all the setter functions and their 
	 * arguments into a string, and generate a bunch of 'setXXX(XXX);' codes.
	 * @param obj
	 * @return
	 */
	public static String getObjectSetterCodes(Class<?> clazz) {
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
									|| type == int.class
									|| type == byte.class
									|| type == long.class
									|| type == short.class
									|| type == boolean.class
									|| type == float.class
									|| type == double.class
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
		r.Recurse(clazz, "request");
		return "//" + r.sbTable.toString() + "\r\n" + r.sbVars.toString() + "\r\n\r\n" + r.sbObjs
				.toString() + "\r\n"
				+ r.sbCode.toString();
	}

	/***
	 * Read a matrix area from excel file
	 * @author yangdo
	 * @param wb The work book object
	 * @param pageName Page name
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return The name-value pair map list
	 */
	public static List<Map<String, String>> readDataSetFromSS(Workbook wb,
			String pageName, int fromCol, int fromRow) {
		Sheet sheet = wb.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return readDataSetFromSS(sheet, fromCol, fromRow);
	}

	/***
	 * Read an array from specific position in a spread sheet
	 * @author yangdo
	 * @param sheet Tab sheet object
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit The upper boundary limit of row number
	 * @return
	 */
	public static List<String> readArrayFromSS(Sheet sheet,
			int fromRow, int fromCol, int upperRowLimit) {
		if (upperRowLimit <= 0)
			upperRowLimit = MaxRowCount;
		List<String> array = new ArrayList<String>();
		Row row;
		Cell cell;
		if (sheet != null) {
			FormulaEvaluator evaluator;
			if (sheet instanceof XSSFSheet)
				evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) sheet.getWorkbook());
			else
				evaluator = new HSSFFormulaEvaluator((HSSFWorkbook) sheet.getWorkbook());

			for (int j = fromRow; j < upperRowLimit; j++) {
				try {
					row = sheet.getRow(j);
					if (row == null)
						break;
					cell = row.getCell(fromCol);
					if (cell == null)
						break;
					String aValue = getCellValueAsString(cell, evaluator);
					if (aValue.trim().isEmpty())
						break;
					else
						array.add(aValue);
				} catch (Exception e) {
					break;
				}
			}
		}
		return array;
	}

	/***
	 * Read an array horizontally from specific position in a spread sheet
	 * @author yangdo
	 * @param sheet Tab sheet object
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperColLimit The upper boundary limit of col number
	 * @return
	 */
	public static List<String> readArrayFromRow(Sheet sheet,
			int fromRow, int fromCol, int upperColLimit) {
		if (upperColLimit <= 0)
			upperColLimit = MaxRowCount;
		List<String> array = new ArrayList<String>();
		Row row;
		Cell cell;
		row = sheet.getRow(fromRow);
		if ((sheet != null) && (row != null)) {
			FormulaEvaluator evaluator;
			if (sheet instanceof XSSFSheet)
				evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) sheet.getWorkbook());
			else
				evaluator = new HSSFFormulaEvaluator((HSSFWorkbook) sheet.getWorkbook());

			for (int j = fromCol; j < upperColLimit; j++) {
				try {
					cell = row.getCell(j);
					if (cell == null)
						break;
					String aValue = getCellValueAsString(cell, evaluator);
					if (aValue.trim().isEmpty())
						break;
					else
						array.add(aValue);
				} catch (Exception e) {
					break;
				}
			}
		}
		return array;
	}

	/***
	 * Read an array horizontally from specific position in a spread sheet
	 * @author yangdo
	 * @param sheet Tab sheet object
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return
	 */
	public static List<String> readArrayFromRow(Sheet sheet,
			int fromRow, int fromCol) {
		return readArrayFromRow(sheet, fromRow, fromCol, -1);
	}

	/***
	 * Read an array from specific position in a spread sheet
	 * @author yangdo
	 * @param sheet Tab sheet object
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return
	 */
	public static List<String> readArrayFromSS(Sheet sheet,
			int fromRow, int fromCol) {
		return readArrayFromSS(sheet, fromRow, fromCol, -1);
	}

	/***
	 * Read an array from specific position in a spread sheet
	 * @author yangdo
	 * @param wb The work book object
	 * @param pageName Page name
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return
	 */
	public static List<String> readArrayFromSS(Workbook wb, String pageName,
			int fromRow, int fromCol) {
		Sheet sheet = wb.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return readArrayFromSS(sheet, fromRow, fromCol);
	}

	/***
	 * Read an array from specific position in a spread sheet
	 * @author yangdo
	 * @param wb The work book object
	 * @param pageName Page name
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit The upper boundary limit of row number
	 * @return
	 */
	public static List<String> readArrayFromSS(Workbook wb, String pageName,
			int fromRow, int fromCol, int upperRowLimit) {
		Sheet sheet = wb.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return readArrayFromSS(sheet, fromRow, fromCol,
				upperRowLimit);
	}

	/***
	 * Read an array from specific position in a spread sheet
	 * @author yangdo
	 * @param wb The work book object
	 * @param page Page number
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return
	 */
	public static List<String> readArrayFromSS(Workbook wb, int page, int fromRow,
			int fromCol, int size) {
		return readArrayFromSS(wb.getSheetAt(page), fromRow, fromCol);
	}

	/***
	 * Read an array from specific position in a spread sheet
	 * @author yangdo
	 * @param wb The work book object
	 * @param page Page number
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit The upper boundary limit of row number
	 * @return The array as List
	 */
	public static List<String> readArrayFromSS(Workbook wb, int page, int fromRow,
			int fromCol, int size, int upperRowLimit) {
		return readArrayFromSS(wb.getSheetAt(page), fromRow, fromCol,
				upperRowLimit);
	}

	/***
	 * Load data block from the main spread sheet file and create a map list, then 
	 * put the map list into internal pool
	 * @param entry The entry key of the data set
	 * @param pageName Name of the page where read data
	 * @param fromCol Read from which column
	 * @param fromRow Read from which row
	 */
	public void loadDataSet(DataSource dataSource) {
		if (currData != null) {
			List<Map<String, String>> ds = readDataSetFromDS(dataSource);
			dataSetPool.put(dataSource.sheet() + "_" + dataSource.col() + "_" + dataSource.row(), ds);
		}
	}

	/***
	 * Load data block from the main spread sheet file and create a map list, then 
	 * put the map list into internal pool
	 * @param entry The entry key of the data set
	 * @param pageIdx Index of the page where read data
	 * @param fromCol Read from which column
	 * @param fromRow Read from which row
	 */
	public void loadDataSet(String entry, int pageIdx,
			int fromCol, int fromRow) {
		if (currData != null) {
			List<Map<String, String>> ds = readDataSetFromSS(
					currData, pageIdx, fromCol, fromRow);
			dataSetPool.put(entry, ds);
		}
	}

	/***
	 * This function is for automatically generating test function parameter list.
	 * With this function's help, we no longer need to write those boring parameters by ourselves.
	 * @param pageName
	 * @param fromCol
	 * @param fromRow
	 * @return
	 */
	public String getProposedParams(String pageName, int fromCol, int fromRow, int firstLineIndent) {
		Sheet sheet = currData.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return getProposedParams(sheet, fromCol, fromRow, firstLineIndent);
	}

	/***
	 * This function is for automatically generating test function parameter list.
	 * With this function's help, we no longer need to write those boring parameters by ourselves.
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @return
	 */
	public static String getProposedParams(Sheet sheet, int fromCol, int fromRow, int firstLineIndent) {
		List<String> headers = readArrayFromRow(sheet, fromRow, fromCol);
		if (headers.size() == 0)
			return "";
		StringBuilder result = new StringBuilder();
		String fieldName = headers.get(0).trim();
		if (fieldName.toUpperCase().equals(fieldName) && (fieldName.length() > 1))
			result.append("String ").append(fieldName);
		else
			result.append("String ").append(StringList.lowerFirstChar(fieldName));
		int lineWidth = firstLineIndent + result.toString().length();
		for (int i = 1; i < headers.size(); i++) {
			result.append(", ");
			lineWidth += 2;
			if (lineWidth > 60) {
				result.append("\r\n");
				lineWidth = 0;
			}
			fieldName = headers.get(i).trim();
			if (fieldName.toUpperCase().equals(fieldName) && (fieldName.length() > 1))
				result.append("String ").append(fieldName);
			else
				result.append("String ").append(StringList.lowerFirstChar(fieldName));
			lineWidth += headers.get(i).length() + 7;
		}
		result.append(", SnippetRunner shell");
		return result.toString();
	}

	/***
	 * This function is for automatically generating test function parameter list.
	 * With this function's help, we no longer need to write those boring parameters by ourselves.
	 * The only difference with getProposedParams is, this function will not add \r\n
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @return
	 */
	public static String getProposedParams2(Sheet sheet, int fromCol, int fromRow) {
		List<String> headers = readArrayFromRow(sheet, fromRow, fromCol);
		if (headers.size() == 0)
			return "";
		StringBuilder result = new StringBuilder();
		String fieldName = headers.get(0).trim();
		if (fieldName.toUpperCase().equals(fieldName) && (fieldName.length() > 1))
			result.append("String ").append(fieldName);
		else
			result.append("String ").append(StringList.lowerFirstChar(fieldName));
		for (int i = 1; i < headers.size(); i++) {
			result.append(", ");
			fieldName = headers.get(i).trim();
			if (fieldName.toUpperCase().equals(fieldName) && (fieldName.length() > 1))
				result.append("String ").append(fieldName);
			else
				result.append("String ").append(StringList.lowerFirstChar(fieldName));
		}
		result.append(", SnippetRunner shell");
		return result.toString();
	}

	/***
	 * This function is for automatically generating test function parameter list.
	 * With this function's help, we no longer need to write those boring parameters by ourselves.
	 * The only difference with getProposedParams is, this function will not add \r\n
	 * @param sheet
	 * @param fromCol
	 * @param fromRow
	 * @return
	 */
	public static String getProposedParams3(Sheet sheet, int fromCol, int fromRow) {
		List<String> headers = readArrayFromRow(sheet, fromRow, fromCol);
		if (headers.size() == 0)
			return "";
		StringBuilder result = new StringBuilder();
		String fieldName = headers.get(0).trim();
		if (fieldName.toUpperCase().equals(fieldName) && (fieldName.length() > 1))
			result.append("String ").append(fieldName);
		else
			result.append("String ").append(StringList.lowerFirstChar(fieldName));
		for (int i = 1; i < headers.size(); i++) {
			result.append(", ");
			fieldName = headers.get(i).trim();
			if (fieldName.toUpperCase().equals(fieldName) && (fieldName.length() > 1))
				result.append("String ").append(fieldName);
			else
				result.append("String ").append(StringList.lowerFirstChar(fieldName));
		}
		return result.toString();
	}

	/***
	 * Dectecting methods that need to get data from certain area of 
	 * spread sheet file. The method must add 'DataSource' annotation.
	 * @author yangdo
	 */
	public final void loadDefaultDataSets() {
		for (Method method : getClass().getMethods()) {
			if (method.isAnnotationPresent(DataSource.class)) {
				DataSource ds = method.getAnnotation(DataSource.class);
				if (method.getParameterTypes().length == 0) {
					System.out.println("============================================================");
					System.out.println("=============(^@^): Please copy following params============");
					System.out.println("============================================================");
					String params = getProposedParams(ds.sheet(), ds.col(), ds.row(),
							method.getName().length() + 5);
					System.out.println(params);
					System.out.println("");
					System.out.println("");
					StringList.setClipBoardText(params);
				}
				loadDataSet(ds);
			}
		}
	};

	/***
	 * Read map from spread sheet from a certain position
	 * @author yangdo
	 * @param sheet Tab sheet object to read
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit Upper limit boundary of row number
	 * @return
	 */
	public static Map<String, String> readMapFromSS(Sheet sheet, int fromRow,
			int fromCol, int upperRowLimit) {
		if (upperRowLimit <= 0)
			upperRowLimit = MaxRowCount;
		Map<String, String> map = new LinkedHashMap<String, String>();
		Row row;
		Cell cell;
		if (sheet != null) {
			FormulaEvaluator evaluator;
			if (sheet instanceof XSSFSheet)
				evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) sheet.getWorkbook());
			else
				evaluator = new HSSFFormulaEvaluator((HSSFWorkbook) sheet.getWorkbook());

			for (int j = fromRow; j < upperRowLimit; j++) {
				try {
					row = sheet.getRow(j);
					if (row == null)
						break;
					cell = row.getCell(fromCol);
					if (cell == null)
						break;
					String aName = StringList.normalizeForVarName(getCellValueAsString(cell, evaluator));
					String aValue = getCellValueAsString(row
							.getCell(fromCol + 1), evaluator);
					if (aName.trim().isEmpty())
						break;
					else
						map.put(aName, aValue);
				} catch (Exception e) {
					break;
				}
			}
		}
		return map;
	}

	/***
	 * Read map from spread sheet from a certain position
	 * @author yangdo
	 * @param sheet Tab sheet object to read
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return
	 */
	public static Map<String, String> readMapFromSS(Sheet sheet, int fromRow,
			int fromCol) {
		return readMapFromSS(sheet, fromRow, fromCol, -1);
	}

	/***
	 * Read map from spread sheet from a certain position
	 * @author yangdo
	 * @param wb The workbook object
	 * @param page Index of the page
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return
	 */
	public static Map<String, String> readMapFromSS(Workbook wb, int page,
			int fromRow, int fromCol) {
		return readMapFromSS(wb.getSheetAt(page), fromRow, fromCol);
	}

	/***
	 * Read map from spread sheet from a certain position
	 * @author yangdo
	 * @param wb The workbook object
	 * @param pageName Index of the page
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit The upper boundary limit of row number
	 * @return
	 */
	public static Map<String, String> readMapFromSS(Workbook wb, int page,
			int fromRow, int fromCol, int upperRowLimit) {
		return readMapFromSS(wb.getSheetAt(page), fromRow, fromCol,
				upperRowLimit);
	}

	/***
	 * Read map from spread sheet from a certain position
	 * @author yangdo
	 * @param wb The workbook object
	 * @param pageName Name of the page
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @return
	 */
	public static Map<String, String> readMapFromSS(Workbook wb, String pageName,
			int fromRow, int fromCol) {
		Sheet sheet = wb.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return readMapFromSS(sheet, fromRow, fromCol);
	}

	/***
	 * Read map from spread sheet from a certain position
	 * @author yangdo
	 * @param wb The workbook object
	 * @param pageName Name of the page
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit The upper boundary limit of row number
	 * @return
	 */
	public static Map<String, String> readMapFromSS(Workbook wb, String pageName,
			int fromRow, int fromCol, int upperRowLimit) {
		Sheet sheet = wb.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return readMapFromSS(sheet, fromRow, fromCol,
				upperRowLimit);
	}

	/***
	 * Read a matrix area from excel file
	 * @author yangdo
	 * @param wb The work book object
	 * @param pageName Page name
	 * @param fromRow From row position
	 * @param fromCol From column position
	 * @param upperRowLimit The upper boundary limit of row number
	 * @return The name-value pair map list
	 */
	public static AreaDataSet readDataSetFromSS(Workbook wb,
			String pageName, int fromCol, int fromRow, int upperRowLimit) {
		Sheet sheet = wb.getSheet(pageName);
		if (sheet == null)
			throw new RuntimeException("Sheet [" + pageName + "] not found!");
		return readDataSetFromSS(sheet, fromCol, fromRow,
				upperRowLimit);
	}

	/***
	 * To find a match for the record in a dataset
	 * 
	 * @param dataset
	 * @param key
	 * @param value
	 * @return
	 */
	public static Map<String, String> lookupDataSet(
			List<Map<String, String>> dataset, String key, String value) {
		for (Map<String, String> result : dataset) {
			String avalue = result.get(key);
			if (avalue != null)
				if (avalue.equalsIgnoreCase(value.trim()))
					return result;
		}
		return null;
	}

	/***
	 * Load the default spread sheet data from file name
	 * @author yangdo
	 * @param fileName
	 * @return
	 */
	public boolean loadData(String fileName) {
		File dataFile = new File(fileName);
		if ((dataFile != null) && dataFile.exists()) {
			boolean result = loadData(dataFile);
			currDataFileName = fileName;
			return result;
		} else
			return false;
	}

	/***
	 * Load the default spread sheet data from resource
	 * @author yangdo
	 * @param clazz
	 * @param resName
	 * @return
	 */
	public boolean loadDataFromRes(Class<?> clazz, String resName) {
		String path = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
		if (path.toLowerCase().endsWith(".jar"))
			try {
				JarFile jarFile = new JarFile(path);
				try {
					JarEntry entry = jarFile
							.getJarEntry(clazz.getPackage().getName().replace('.', '/') + "/" + resName);
					if (entry != null) {
						Workbook wb = null;
						try {
							wb = new XSSFWorkbook(jarFile.getInputStream(entry));
						} catch (Exception ex) {
							wb = new HSSFWorkbook(jarFile.getInputStream(entry));
						}
						return loadData(wb);
					} else
						return false;
				} finally {
					jarFile.close();
				}
			} catch (IOException e) {
				return false;
			}
		else
			try {
				return loadData(getResPath(clazz, resName));
			} catch (Exception e) {
				return false;
			}
	}

	/***
	 * Load the default spread sheet data from InputStream object
	 * @author yangdo
	 * @param input
	 * @return
	 */
	public boolean loadData(Workbook wb) {
		currData = wb;

		if (wb instanceof XSSFWorkbook)
			currEvaluator = new XSSFFormulaEvaluator((XSSFWorkbook) wb);
		else
			currEvaluator = new HSSFFormulaEvaluator((HSSFWorkbook) wb);

		try {
			loadDefaultDataSets();
			loadAnnotatedFields();
			loadAdditionalData(wb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Sheet sheet;
		Row row;
		sheet = wb.getSheet(defaultCaseTabName);
		defaultWriteBack = createWriteBack(defaultCaseTabName, 0, 0);
		if (sheet != null) {
			FormulaEvaluator evaluator;
			if (sheet instanceof XSSFSheet)
				evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) sheet.getWorkbook());
			else
				evaluator = new HSSFFormulaEvaluator((HSSFWorkbook) sheet.getWorkbook());

			row = sheet.getRow(0);
			Cell cell = row.getCell(0);
			String colName = getCellValueAsString(cell, evaluator);
			List<String> colData = new ArrayList<String>();
			colData.add(colName);
			caseDataSet.add(colData);
			for (int j = 1; j < sheet.getLastRowNum() + 1; j++) {
				Row row2 = sheet.getRow(j);
				if (row2 == null)
					break;
				String key = getCellValueAsString(row2.getCell(0), evaluator);
				if (key.isEmpty())
					break;
				if (!key.startsWith("#"))
					colData.add(key);
			}
			for (int i = 1; i < row.getLastCellNum(); i++) {
				cell = row.getCell(i);
				colName = getCellValueAsString(cell, evaluator);
				if (colName.trim().isEmpty())
					break;
				if (!colName.startsWith("#")) {
					colData = new ArrayList<String>();
					colData.add(colName);
					caseDataSet.add(colData);
					for (int j = 1; j < sheet.getLastRowNum() + 1; j++) {
						Row row2 = sheet.getRow(j);
						if (row2 == null)
							break;
						Cell keyCell = row2.getCell(0);
						if (keyCell != null) {
							String keyValue = getCellValueAsString(keyCell, evaluator);
							if (keyValue.isEmpty())
								break;
							if (!keyValue.startsWith("#")) {
								cell = sheet.getRow(j).getCell(i);
								if (cell != null)
									colData.add(getCellValueAsString(cell, evaluator));
								else
									colData.add("");
							}
						}
					}
				}
			}
		}
		return true;
	}

	/***
	 * Load the default spread sheet data from File object
	 * @author yangdo
	 * @param dataFile
	 * @return
	 */
	public boolean loadData(File dataFile) {
		try {
			if (!dataFile.exists())
				return false;
			Workbook wb = null;
			try {
				wb = new XSSFWorkbook(new FileInputStream(dataFile));
			} catch (Exception ex) {
				wb = new HSSFWorkbook(new FileInputStream(dataFile));
			}
			return loadData(wb);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/***
	 * Save current data to a file
	 * @param fileName
	 */
	public void saveData(String fileName) {
		try {
			FileOutputStream fileOut = new FileOutputStream(fileName);
			try {
				currData.write(fileOut);
			} finally {
				fileOut.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/***
	 * Save current data back a file
	 * @param fileName
	 */
	public void saveData() {
		saveData(currDataFileName);
	}

	public boolean loadData() {
		boolean loaded = false;
		loaded = loadData(expandFileName(".xlsx"));
		if (!loaded)
			loaded = loadData(expandFileName(".xls"));
		if (!loaded)
			loaded = loadDataFromRes(getClass(), getClass().getSimpleName() + ".xlsx");
		if (!loaded)
			loaded = loadDataFromRes(getClass(), getClass().getSimpleName() + ".xls");
		return loaded;
	}

	/**
	 * Retrieves test suite xml data from test db xml webservice
	 * @author yangdo
	 */
	@BeforeTest(groups = { "_default_", "abstract", "excel" })
	public boolean loadTestData() {
		return loadData();
	}

	protected boolean isScenarioPrepared(String methodName) {
		return lastCalledBeforeMethod.equals(methodName);
	}

	protected void afterScenarioPrepared(String methodName) {
		lastCalledBeforeMethod = methodName;
	}

	@BeforeMethod(groups = { "_default_", "abstract", "excel" })
	public void beforeScenario(Method m) {
		if (isExperimenting)
			return;
		String methodName = m.getName();
		methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
		methodName = "before" + methodName;
		if (!isScenarioPrepared(methodName))
			try {
				Method method;
				method = getClass().getMethod(methodName);
				Object[] args = new Object[0];
				if (method != null)
					try {
						System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
						System.out
								.println(">>> Executing Scenario Prerequisite >>> [" + methodName + "]");
						System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
						invoke(this, method, args);
						afterScenarioPrepared(methodName);
					} catch (Throwable e) {
						System.out
								.println(">>> Exceptions occurred during executing scenario prerequisite >>> [" + methodName + "]");
						e.printStackTrace();
					}
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
			}
	}

	/**
	 * The default main data provider
	 * @author yangdo
	 * @return [[DataMap]]
	 */
	@DataProvider(name = "DefaultCases")
	public Iterator<Object[]> getTestData(Method m) {
		List<Object[]> testData = new ArrayList<Object[]>();
		if (m.getParameterTypes().length == 0) {
			System.out.println("============================================================");
			System.out.println("=============(^@^): Please copy following params============");
			System.out.println("============================================================");
			String params = getProposedParams(defaultCaseTabName, 0, 0, m.getName().length() + 5);
			System.out.println(params);
			System.out.println("");
			System.out.println("");
			StringList.setClipBoardText(params);
		} else {
			Class<?>[] types = m.getParameterTypes();
			int pCount = types.length;
			Object[] params = null;
			if (caseDataSet.size() > 0)
				try {
					boolean useMapAsParam = (pCount == 1) && (types[0] == Map.class);
					ClassPool pool = ClassPool.getDefault();
					CtClass cc = pool.get(getClass().getName());
					CtClass[] ctTypes = new CtClass[pCount];
					for (int i = 0; i < pCount; i++)
						ctTypes[i] = pool.get(types[i].getName());
					CtMethod cm = cc.getDeclaredMethod(m.getName(), ctTypes);
					MethodInfo methodInfo = cm.getMethodInfo();
					CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
					LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
							.getAttribute(LocalVariableAttribute.tag);
					int idxOffset = 0;//Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
					for (int i = 0; i < attr.length(); i++)
						if (attr.variableName(i).equals("this")) {
							idxOffset = i + 1;
							break;
						}

					for (int row = 1; row < caseDataSet.get(0).size(); row++) {
						Map<String, String> data = new LinkedHashMap<String, String>();
						for (int col = 0; col < caseDataSet.size(); col++) {
							List<String> acolumn = caseDataSet.get(col);
							String avalue = acolumn.get(row);
							data.put(StringList.normalizeForVarName(acolumn.get(0)), avalue);
						}
						if (useMapAsParam) {
							params = new Object[1];
							params[0] = data;
							testData.add(params);
						} else
						{
							params = new Object[pCount];
							for (int i = 0; i < pCount; i++) {
								Class<?> t = types[i];
								if (t == SnippetRunner.class) {
									SnippetRunner shell = new SnippetRunner(this);
									shell.writeBack = defaultWriteBack;
									shell.dataIndex = row - 1;
									params[i] = shell;
								} else {
									String varName = attr.variableName(i + idxOffset);
									String cellValue = data.get(varName);
									if (cellValue == null)
										cellValue = data.get(StringList.upperFirstChar(varName));
									if (t == int.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Integer.parseInt(cellValue.trim());
									} else if (t == long.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Long.parseLong(cellValue.trim());
									} else if (t == byte.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Byte.parseByte(cellValue.trim());
									} else if (t == short.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Short.parseShort(cellValue.trim());
									} else if (t == boolean.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "false";
										params[i] = Boolean.parseBoolean(cellValue.trim());
									} else if (t == double.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Double.parseDouble(cellValue.trim());
									} else if (t == float.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Float.parseFloat(cellValue.trim());
									} else
										params[i] = cellValue;
								}
							}
							for (int i = 0; i < pCount; i++)
								if (types[i] == SnippetRunner.class) {
									Binding context = ((SnippetRunner) params[i]).getContext();
									for (int j = 0; j < pCount; j++) {
										String varName = attr.variableName(j + idxOffset);
										if (types[j] != SnippetRunner.class)
											context.setVariable(varName, params[j]);
									}
									initSnippetContext(context);
								}
							testData.add(params);
						}
					}
				} catch (NotFoundException e) {
					e.printStackTrace();
				}
		}
		return testData.iterator();
	}

	/***
	 * This method is for overriding in descendent class for initializing
	 * context object with need variables.
	 * @param context
	 */
	public void initSnippetContext(Binding context) {

	}

	/**
	 * The default main data provider. It requires the test method's name should be
	 * as such pattern as: testSomething_with_NameOfDataSet
	 * @author yangdo
	 * @return Data Map
	 */
	@DataProvider(name = "DataBlock")
	public Iterator<Object[]> getCommonData(Method m) {
		if (m.isAnnotationPresent(DataSource.class)) {
			DataSource ds = m.getAnnotation(DataSource.class);
			String dsName = ds.sheet() + "_" + ds.col() + "_" + ds.row();
			return provideData(dsName, m);
		} else
			return null;
	}

	/***
	 * This is for providing data for [DataProvider] annotation
	 * @author yangdo
	 * @param dataset
	 * @return
	 */
	public Iterator<Object[]> provideData(
			List<Map<String, String>> dataset, Method m) {
		List<Object[]> testData = new ArrayList<Object[]>();
		Class<?>[] types = m.getParameterTypes();
		int pCount = types.length;
		Object[] params = null;
		boolean useMapAsParam = (pCount == 1) && (types[0] == Map.class);
		WriteBack writeBack = null;
		if (dataset.getClass().isAssignableFrom(AreaDataSet.class))
			writeBack = ((AreaDataSet) dataset).writeBack;
		if (dataset != null) {
			ClassPool pool = ClassPool.getDefault();
			try {
				CtClass cc = pool.get(getClass().getName());
				CtClass[] ctTypes = new CtClass[pCount];
				for (int i = 0; i < pCount; i++)
					ctTypes[i] = pool.get(types[i].getName());
				CtMethod cm = cc.getDeclaredMethod(m.getName(), ctTypes);
				MethodInfo methodInfo = cm.getMethodInfo();
				CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
				LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
						.getAttribute(LocalVariableAttribute.tag);
				int idxOffset = 0;//Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
				for (int i = 0; i < attr.length(); i++)
					if (attr.variableName(i).equals("this")) {
						idxOffset = i + 1;
						break;
					}

				for (int index = 0; index < dataset.size(); index++) {
					Map<String, String> row = dataset.get(index);
					if (useMapAsParam) {
						params = new Object[1];
						params[0] = row;
					} else {
						if (pCount == 0)
							continue;
						String keyValue = row.get(attr.variableName(idxOffset));
						if (keyValue == null)
							keyValue = row.get(StringList.upperFirstChar(attr.variableName(idxOffset)));
						if ((keyValue == null) || keyValue.startsWith("#"))
							continue;
						params = new Object[pCount];
						for (int i = 0; i < pCount; i++) {
							Class<?> t = types[i];

							if (t == SnippetRunner.class) {
								SnippetRunner shell = new SnippetRunner(this);
								shell.writeBack = writeBack;
								shell.dataIndex = index;
								params[i] = shell;
							} else {
								String cellValue = row.get(attr.variableName(i + idxOffset));
								try {
									if (cellValue == null)
										cellValue = row.get(StringList.upperFirstChar(attr
												.variableName(i + idxOffset)));
									if (t == int.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Integer.parseInt(cellValue.trim());
									} else if (t == long.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Long.parseLong(cellValue.trim());
									} else if (t == byte.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Byte.parseByte(cellValue.trim());
									} else if (t == short.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Short.parseShort(cellValue.trim());
									} else if (t == boolean.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "false";
										params[i] = Boolean.parseBoolean(cellValue.trim());
									} else if (t == double.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Double.parseDouble(cellValue.trim());
									} else if (t == float.class) {
										if ((cellValue == null) || cellValue.trim().isEmpty())
											cellValue = "0";
										params[i] = Float.parseFloat(cellValue.trim());
									} else
										params[i] = cellValue;

								} catch (RuntimeException e) {
									System.out.println("Invalid field value for field [" +
											attr.variableName(i + idxOffset) + "]:\r\n" + cellValue);
									throw e;
								}
							}
						}
						for (int i = 0; i < pCount; i++)
							if (types[i] == SnippetRunner.class) {
								Binding context = ((SnippetRunner) params[i]).getContext();
								for (int j = 0; j < pCount; j++) {
									String varName = attr.variableName(j + idxOffset);
									if (types[j] != SnippetRunner.class)
										context.setVariable(varName, params[j]);
								}
								initSnippetContext(context);
							}
					}
					testData.add(params);
				}
			} catch (NotFoundException e) {
				e.printStackTrace();
			}
		}
		return testData.iterator();
	}

	/***
	 * Provide data from internal pool, the data is a list of string map
	 * @author yangdo
	 * @param entry the entry name of the dataset
	 * @return
	 */
	public Iterator<Object[]> provideData(String entry, Method m) {
		List<Map<String, String>> dataset = dataSetPool.get(entry);
		if (dataset != null)
			return provideData(dataset, m);
		else {
			return new ArrayList<Object[]>().iterator();
		}
	}

	/***
	 * Get pre-loaded datasets from pool.
	 * @param entry Name of the dataset, might be of such patten as:
	 * SheetName_m_n
	 * @return
	 */
	public List<Map<String, String>> getDataSetFromPool(String entry) {
		return dataSetPool.get(entry);
	}

	/***
	 * Remove all sheets in currData except the specified one.
	 * @param sheet
	 */
	public void remainSheet(String sheet) {
		remainSheet(sheet, null);
	}

	/***
	 * Remove all sheets in currData except the specified one, and set the 
	 * sheet a new name.
	 * @param sheet
	 * @param newName
	 */
	public void remainSheet(String sheet, String newName) {
		int idx = currData.getSheetIndex(sheet);
		if ((newName != null) && (idx >= 0))
			currData.setSheetName(idx, newName);
		for (int i = currData.getNumberOfSheets() - 1; i >= 0; i--) {
			if (i != idx)
				currData.removeSheetAt(i);
		}
	}

	public boolean sendReportWithData(String mailSubject, String fromAddress, String toAddress,
			String ccAddress, String fileName) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			currData.write(bos);
			return sendReport(mailSubject, getClass().getSimpleName() + ".temp.html", tempVars, fromAddress,
					toAddress, ccAddress, fileName, bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean sendReport(String mailSubject, String fromAddress, String toAddress, String ccAddress) {
		if (fromAddress == null)
			fromAddress = "";
		if (toAddress == null)
			toAddress = "";
		if (ccAddress == null)
			ccAddress = "";
		return sendReport(mailSubject, getClass().getSimpleName() + ".temp.html", tempVars, fromAddress, toAddress, ccAddress);
	}

	public boolean sendReportWithData(String mailSubject, String fromAddress, String toAddress,
			String ccAddress) {
		if (currData instanceof HSSFWorkbook)
			return sendReportWithData(mailSubject, fromAddress, toAddress, ccAddress,
					"TestData.xls");
		else if (currData instanceof XSSFWorkbook)
			return sendReportWithData(mailSubject, fromAddress, toAddress, ccAddress,
					"TestData.xlsx");
		return false;
	}

	public boolean sendReportWithData(String mailSubject, String tempFileName, Map<String, String> values,
			String fromAddress, String toAddress, String ccAddress, String fileName) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			currData.write(bos);
			return sendReport(mailSubject, expandFileName(tempFileName), values, fromAddress,
					toAddress, ccAddress, fileName, bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean sendReportWithData(String mailSubject, String tempFileName, Map<String, String> values,
			String fromAddress, String toAddress, String ccAddress) {
		if (currData instanceof HSSFWorkbook)
			return sendReportWithData(mailSubject, tempFileName, values, fromAddress,
					toAddress, ccAddress, "TestData.xls");
		else if (currData instanceof XSSFWorkbook)
			return sendReportWithData(mailSubject, tempFileName, values, fromAddress,
					toAddress, ccAddress,
					"TestData.xlsx");
		return false;
	}
}