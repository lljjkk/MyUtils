package octopus.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class WriteBack {
	public FormulaEvaluator evaluator;
	private Workbook currWorkbook = null;
	private Sheet currSheet = null;
	private Row currRow = null;
	private Map<String, Integer> fieldMap = new HashMap<String, Integer>();
	private List<Integer> records = new ArrayList<Integer>();
	private Map<String, Integer> keyMap = new HashMap<String, Integer>();
	private int rowBound;
	private int rowStart = 1;
	private int colStart = 0;
	private int keyIndex = 0;

	public WriteBack(Sheet sheet, String keyField, int fromCol, int fromRow) {
		rowStart = fromRow;
		colStart = fromCol;
		if (sheet != null) {
			if (sheet instanceof XSSFSheet)
				evaluator = new XSSFFormulaEvaluator((XSSFWorkbook) sheet.getWorkbook());
			else
				evaluator = new HSSFFormulaEvaluator((HSSFWorkbook) sheet.getWorkbook());

			Row row;
			Cell cell;
			currSheet = sheet;
			currWorkbook = sheet.getWorkbook();
			row = sheet.getRow(fromRow);
			rowBound = fromRow;
			if (row != null) {
				if (keyField != null)
					keyField = keyField.trim().toLowerCase();
				keyIndex = fromCol;
				for (int i = fromCol; i <= OctopusExcelDataTool.MaxColCout; i++) {
					cell = row.getCell(i);
					if (cell == null)
						break;
					String cellData = OctopusExcelDataTool.getCellValueAsString(cell, evaluator);
					if (cellData.isEmpty())
						break;
					cellData = StringList.normalizeForVarName(cellData);
					fieldMap.put(cellData, i);
					if ((keyField != null) && (cellData.trim().toLowerCase().equals(keyField)))
						keyIndex = i;
				}
				int upperRowLimit = OctopusExcelDataTool.MaxRowCount;
				for (int j = fromRow + 1; j < upperRowLimit; j++)
					try {
						row = sheet.getRow(j);
						rowBound = j;
						if (row == null)
							break;
						cell = row.getCell(fromCol);
						if (cell == null)
							break;
						String rowKey = OctopusExcelDataTool.getCellValueAsString(cell, evaluator);
						if (rowKey.trim().isEmpty())
							break;
						else {
							if (keyIndex >= 0) {
								cell = row.getCell(keyIndex);
								String cellData = "";
								if (cell != null)
									cellData = OctopusExcelDataTool.getCellValueAsString(cell, evaluator)
											.trim()
											.toLowerCase();
								keyMap.put(cellData, j);
							}
							records.add(j);
						}
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
			}
		}
	}

	public boolean gotoIndex(int index) {
		if ((index >= 0) && (index < records.size())) {
			currRow = currSheet.getRow(records.get(index));
			return true;
		}
		currRow = null;
		return false;
	}

	public boolean gotoRow(int row) {
		try {
			currRow = currSheet.getRow(rowStart + row);
			if (currRow == null)
				currRow = currSheet.createRow(rowStart + row);
		} catch (Exception e) {
			currRow = null;
		}
		return currRow != null;
	}

	public boolean gotoKey(String key) {
		Integer index = keyMap.get(key.toLowerCase());
		if (index != null) {
			currRow = currSheet.getRow(index);
			return true;
		}
		currRow = null;
		return false;
	}

	public void gotoKeyOrAdd(String key) {
		if (!gotoKey(key))
			addRow(key);
	}

	public void addRow(String key) {
		currRow = currSheet.getRow(rowBound);
		if (currRow == null)
			currRow = currSheet.createRow(rowBound);
		if (key != null) {
			keyMap.put(key, rowBound);
			setFieldValue(keyIndex, key);
		}
		rowBound++;
	}

	public void addRow() {
		addRow(null);
	}

	public boolean setFieldByKey(String key, String field, String value) {
		if (gotoKey(key))
			return setFieldValue(field, value);
		return false;
	}

	public boolean setFieldByIndex(int index, String field, String value) {
		if (gotoIndex(index))
			return setFieldValue(field, value);
		return false;
	}

	public boolean setFieldByRow(int row, String field, String value) {
		if (gotoRow(row))
			return setFieldValue(field, value);
		return false;
	}

	public boolean setFieldByKey(String key, int fieldIdx, String value) {
		if (gotoKey(key))
			return setFieldValue(fieldIdx, value);
		return false;
	}

	public boolean setFieldByIndex(int index, int fieldIdx, String value) {
		if (gotoIndex(index))
			return setFieldValue(fieldIdx, value);
		return false;
	}

	public boolean setFieldByRow(int row, int fieldIdx, String value) {
		if (gotoRow(row))
			return setFieldValue(fieldIdx, value);
		return false;
	}

	public boolean setFieldByKey(String key, String field, String value, CellStyle cellStyle) {
		if (gotoKey(key))
			return setFieldValue(field, value, cellStyle);
		return false;
	}

	public boolean setFieldByIndex(int index, String field, String value, CellStyle cellStyle) {
		if (gotoIndex(index))
			return setFieldValue(field, value, cellStyle);
		return false;
	}

	public boolean setFieldByRow(int row, String field, String value, CellStyle cellStyle) {
		if (gotoRow(row))
			return setFieldValue(field, value, cellStyle);
		return false;
	}

	public boolean setFieldByKey(String key, int fieldIdx, String value, CellStyle cellStyle) {
		if (gotoKey(key))
			return setFieldValue(fieldIdx, value, cellStyle);
		return false;
	}

	public boolean setFieldByIndex(int index, int fieldIdx, String value, CellStyle cellStyle) {
		if (gotoIndex(index))
			return setFieldValue(fieldIdx, value, cellStyle);
		return false;
	}

	public boolean setFieldByRow(int row, int fieldIdx, String value, CellStyle cellStyle) {
		if (gotoRow(row))
			return setFieldValue(fieldIdx, value, cellStyle);
		return false;
	}

	public boolean setFieldValue(String field, String value, CellStyle cellStyle) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(String field, String value) {
		return setFieldValue(field, value, null);
	}

	public boolean setFieldValue(int fieldIdx, String value, CellStyle cellStyle) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(int fieldIdx, String value) {
		return setFieldValue(fieldIdx, value, null);
	}

	public boolean setFieldValue(String field, double value, CellStyle cellStyle) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(String field, double value) {
		return setFieldValue(field, value, null);
	}

	public boolean setFieldValue(int fieldIdx, double value, CellStyle cellStyle) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(int fieldIdx, double value) {
		return setFieldValue(fieldIdx, value, null);
	}

	public boolean setFieldValue(String field, boolean value, CellStyle cellStyle) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(String field, boolean value) {
		return setFieldValue(field, value, null);
	}

	public boolean setFieldValue(int fieldIdx, boolean value, CellStyle cellStyle) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(int fieldIdx, boolean value) {
		return setFieldValue(fieldIdx, value, null);
	}

	public boolean setFieldValue(String field, Date value, CellStyle cellStyle) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(String field, Date value) {
		return setFieldValue(field, value, null);
	}

	public boolean setFieldValue(int fieldIdx, Date value, CellStyle cellStyle) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(int fieldIdx, Date value) {
		return setFieldValue(fieldIdx, value, null);
	}

	public boolean setFieldValue(String field, Calendar value, CellStyle cellStyle) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(String field, Calendar value) {
		return setFieldValue(field, value, null);
	}

	public boolean setFieldValue(int fieldIdx, Calendar value, CellStyle cellStyle) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(int fieldIdx, Calendar value) {
		return setFieldValue(fieldIdx, value, null);
	}

	public boolean setFieldValue(String field, RichTextString value, CellStyle cellStyle) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(String field, RichTextString value) {
		return setFieldValue(field, value, null);
	}

	public boolean setFieldValue(int fieldIdx, RichTextString value, CellStyle cellStyle) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			cell.setCellValue(value);
			if (cellStyle != null)
				cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setFieldValue(int fieldIdx, RichTextString value) {
		return setFieldValue(fieldIdx, value, null);
	}

	public CellStyle createCellStyle() {
		CellStyle result = currWorkbook.createCellStyle();
		return result;
	}

	public CellStyle createCellStyleWithFont() {
		CellStyle result = currWorkbook.createCellStyle();
		createFont(result);
		return result;
	}

	public CellStyle createCellStyle(Cell cell) {
		CellStyle result = createCellStyle(cell.getCellStyle());
		cell.setCellStyle(result);
		return result;
	}

	public CellStyle createCellStyleWithFont(Cell cell) {
		CellStyle result = createCellStyle(cell.getCellStyle());
		cell.setCellStyle(result);
		createFont(result);
		return result;
	}

	public CellStyle createCellStyle(CellStyle cloneFrom) {
		CellStyle result = currWorkbook.createCellStyle();
		result.cloneStyleFrom(cloneFrom);
		return result;
	}

	public Font createFont() {
		return currWorkbook.createFont();
	}

	public Font createFont(Font cloneFrom) {
		Font result = currWorkbook.createFont();
		cloneFont(result, cloneFrom);
		return result;
	}

	public Font createFontFromStyle(CellStyle cloneFrom) {
		Font result = currWorkbook.createFont();
		cloneFont(result, currWorkbook.getFontAt(cloneFrom.getFontIndex()));
		return result;
	}

	public Font createFont(CellStyle targetStyle) {
		Font result = createFontFromStyle(targetStyle);
		targetStyle.setFont(result);
		return result;
	}

	public boolean setCellColor(String field, Short colorIdx) {
		if (colorIdx == null)
			return false;
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);

			CellStyle cellStyle = createCellStyle();
			cellStyle.cloneStyleFrom(cell.getCellStyle());
			cell.setCellStyle(cellStyle);
			cellStyle.setFillForegroundColor(colorIdx);
			cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		}
		return result;
	}

	public static void setColor(CellStyle cellStyle, Short colorIdx) {
		cellStyle.setFillForegroundColor(colorIdx);
		cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	}

	public boolean setCellColor(int fieldIdx, Short colorIdx) {
		if (colorIdx == null)
			return false;
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			CellStyle cellStyle = createCellStyle();
			cellStyle.cloneStyleFrom(cell.getCellStyle());
			cell.setCellStyle(cellStyle);
			setColor(cellStyle, colorIdx);
		}
		return result;
	}

	public static void setBorder(CellStyle cellStyle, Boolean withBorder, Short colorIdx) {
		if (withBorder != null) {
			if (withBorder) {
				cellStyle.setBorderLeft(BorderStyle.valueOf((short) 1));
				cellStyle.setBorderTop(BorderStyle.valueOf((short) 1));
				cellStyle.setBorderRight(BorderStyle.valueOf((short) 1));
				cellStyle.setBorderBottom(BorderStyle.valueOf((short) 1));
			} else {
				cellStyle.setBorderLeft(BorderStyle.valueOf((short) 1));
				cellStyle.setBorderTop(BorderStyle.valueOf((short) 1));
				cellStyle.setBorderRight(BorderStyle.valueOf((short) 1));
				cellStyle.setBorderBottom(BorderStyle.valueOf((short) 1));
			}
		}
		if (colorIdx != null) {
			cellStyle.setLeftBorderColor(colorIdx);
			cellStyle.setTopBorderColor(colorIdx);
			cellStyle.setRightBorderColor(colorIdx);
			cellStyle.setBottomBorderColor(colorIdx);
		}
	}

	public static void setCellBorder(Cell cell, Boolean withBorder, Short colorIdx) {
		CellStyle cellStyle = cell.getCellStyle();
		if (cellStyle == null) {
			cellStyle = cell.getSheet().getWorkbook().createCellStyle();
			cell.setCellStyle(cellStyle);
		}
		setBorder(cellStyle, withBorder, colorIdx);
	}

	public void setBorder(Cell cell, Boolean withBorder, Short colorIdx) {
		CellStyle cellStyle = createCellStyle();
		cellStyle.cloneStyleFrom(cell.getCellStyle());
		cell.setCellStyle(cellStyle);
		setBorder(cellStyle, withBorder, colorIdx);
	}

	public boolean setBorder(String field, Boolean withBorder, Short colorIdx) {
		if (colorIdx == null)
			return false;
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			setBorder(cell, withBorder, colorIdx);
		}
		return result;
	}

	public boolean setBorder(int fieldIdx, Boolean withBorder, Short colorIdx) {
		if (colorIdx == null)
			return false;
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			setBorder(cell, withBorder, colorIdx);
		}
		return result;
	}

	public static void cloneFont(Font targetFont, Font srcFont) {
		if ((srcFont == null) || (targetFont == null))
			return;
		targetFont.setFontName(srcFont.getFontName());
		targetFont.setFontHeight(srcFont.getFontHeight());
		targetFont.setFontHeightInPoints(srcFont.getFontHeightInPoints());
		targetFont.setItalic(srcFont.getItalic());
		targetFont.setStrikeout(srcFont.getStrikeout());
		targetFont.setColor(srcFont.getColor());
		targetFont.setTypeOffset(srcFont.getTypeOffset());
		targetFont.setUnderline(srcFont.getUnderline());
		targetFont.setCharSet(srcFont.getCharSet());
		targetFont.setCharSet(srcFont.getCharSet());
		targetFont.setBold(srcFont.getBold());
	}

	public void setTextStyle(CellStyle cellStyle, Short colorIdx, Boolean bold, Boolean underline,
			Boolean strikeOut) {
		Font font = currWorkbook.getFontAt(cellStyle.getFontIndex());
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
	}

	private void setTextStyle(Cell cell, Short colorIdx, Boolean bold, Boolean underline,
			Boolean strikeOut) {
		CellStyle cellStyle = createCellStyle();
		cellStyle.cloneStyleFrom(cell.getCellStyle());
		cell.setCellStyle(cellStyle);
		Font oldFont = currWorkbook.getFontAt(cellStyle.getFontIndex());
		Font font = currWorkbook.createFont();
		cloneFont(font, oldFont);
		cellStyle.setFont(font);
		setTextStyle(cellStyle, colorIdx, bold, underline, strikeOut);
	}

	public boolean setTextStyle(String field, Short colorIdx, Boolean bold, Boolean underline,
			Boolean strikeOut) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			setTextStyle(cell, colorIdx, bold, underline, strikeOut);
		}
		return result;
	}

	public boolean setTextStyle(int fieldIdx, Short colorIdx, Boolean bold, Boolean underline,
			Boolean strikeOut) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			setTextStyle(cell, colorIdx, bold, underline, strikeOut);
		}
		return result;
	}

	public boolean setCellStyle(String field, CellStyle cellStyle) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public boolean setCellStyle(int fieldIdx, CellStyle cellStyle) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			cell.setCellStyle(cellStyle);
		}
		return result;
	}

	public void setFont(CellStyle cellStyle, String fontName, int size) {
		Font font = currWorkbook.getFontAt(cellStyle.getFontIndex());
		if ((fontName != null) && (!fontName.trim().isEmpty()))
			font.setFontName(fontName);
		font.setFontHeightInPoints((short) size);
	}

	public void setCellFont(Cell cell, String fontName, int size) {
		CellStyle cellStyle = createCellStyle();
		cellStyle.cloneStyleFrom(cell.getCellStyle());
		cell.setCellStyle(cellStyle);

		Font oldFont = currWorkbook.getFontAt(cellStyle.getFontIndex());
		Font font = currWorkbook.createFont();
		cloneFont(font, oldFont);
		cellStyle.setFont(font);
		setFont(cellStyle, fontName, size);
	}

	public boolean setCellFont(String field, String fontName, int size) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			setCellFont(cell, fontName, size);
		}
		return result;
	}

	public boolean setCellFont(int fieldIdx, String fontName, int size) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			setCellFont(cell, fontName, size);
		}
		return result;
	}

	private void setCellAlignment(Cell cell, HorizontalAlignment alignment) {
		CellStyle cellStyle = createCellStyle();
		cellStyle.cloneStyleFrom(cell.getCellStyle());
		cell.setCellStyle(cellStyle);
		cellStyle.setAlignment(alignment);
		cell.setCellStyle(cellStyle);
	}

	public boolean setCellAlignment(String field, HorizontalAlignment alignment) {
		Integer fieldPos = fieldMap.get(field);
		boolean result = (currRow != null) && (fieldPos != null);
		if (result) {
			Cell cell = currRow.getCell(fieldPos);
			if (cell == null)
				cell = currRow.createCell(fieldPos);
			setCellAlignment(cell, alignment);
		}
		return result;
	}

	public boolean setCellAlignment(int fieldIdx, HorizontalAlignment alignment) {
		boolean result = currRow != null;
		if (result) {
			Cell cell = currRow.getCell(colStart + fieldIdx);
			if (cell == null)
				cell = currRow.createCell(colStart + fieldIdx);
			setCellAlignment(cell, alignment);
		}
		return result;
	}

	public String getFieldValue(String field) {
		if (currRow != null) {
			Integer fieldPos = fieldMap.get(field);
			if (fieldPos != null) {
				Cell cell = currRow.getCell(fieldPos);
				if (cell == null)
					return "";
				else
					return OctopusExcelDataTool.getCellValueAsString(cell, evaluator);
			} else
				return null;
		}
		return null;
	}

	public Map<String, String> getValues() {
		if (currRow != null) {
			Map<String, String> result = new LinkedHashMap<String, String>();
			for (String field : fieldMap.keySet()) {
				Integer fieldPos = fieldMap.get(field);
				Cell cell = currRow.getCell(fieldPos);
				if (cell != null)
					result.put(field, OctopusExcelDataTool.getCellValueAsString(cell, evaluator));
				else
					result.put(field, "");
			}
			return result;
		} else
			return null;
	}
}