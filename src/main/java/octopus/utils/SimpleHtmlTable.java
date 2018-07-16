package octopus.utils;

public class SimpleHtmlTable {
	private StringBuilder src = new StringBuilder();
	private int fieldCount = 0;
	private String tableAttributes;
	public int rowCount = 0;

	public void init(String attr, String... header) {
		tableAttributes = attr;
		src.setLength(0);
		src.append("<table ").append(tableAttributes).append(" >\n");
		rowCount = 0;
		fieldCount = header.length;

		src.append("<thead>\n");
		src.append("<tr>\n");
		for (int i = 0; i < fieldCount; i++)
			src.append("<th>").append(StringList.escapeHtml(header[i].trim())).append("</th>\n");
		src.append("</tr>\n");
		src.append("</thead>\n");
		src.append("<tbody>\n");

	}

	public void add(String... cell) {
		rowCount++;
		src.append("<tr>\n");
		for (int i = 0; i < cell.length; i++) {
			String value = cell[i];
			if (value == null)
				value = "";
			else
				value = value.trim();
			src.append("<td>").append(StringList.escapeHtml(value)).append("</td>\n");
		}
		src.append("</tr>\n");
	}

	public void add(String firstCell, String[] cellsRight) {
		String[] cells = new String[cellsRight.length + 1];
		cells[0] = firstCell;
		for (int i = 0; i < cellsRight.length; i++)
			cells[i + 1] = cellsRight[i];
		add(cells);
	}

	public String getSrc() {
		src.append("</tbody>\n</table>");
		String result = src.toString();
		src.setLength(0);
		return result;
	}
}
