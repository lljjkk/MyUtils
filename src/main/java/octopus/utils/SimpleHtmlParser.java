package octopus.utils;

import static octopus.utils.StringList.getBetween;

import octopus.utils.StringList.OnBetween;

public class SimpleHtmlParser {
	public interface IProcessRow {
		public void processRow(int row, StringList values);
	}

	private int CurrPos = 0;
	private int currTagBegin = 0;
	private int currTagEnd = 0;
	private String currAttributes = "";

	private String getTag(String line) {
		int len = line.length();
		int idx1 = line.indexOf('<', CurrPos);
		if (idx1 >= 0) {
			currTagBegin = idx1;
			idx1++;
			while ((idx1 < len) && line.charAt(idx1) == ' ' && line.charAt(idx1) != '>')
				idx1++;
			int idx2 = line.indexOf(" ", idx1 + 1);
			int idx3 = line.indexOf(">", idx1 + 1);
			currTagEnd = idx3;
			if ((idx2 < 0) || ((idx3 > 0) && (idx3 < idx2)))
				idx2 = idx3;
			CurrPos = idx2;
			String result = line.substring(idx1, idx2);
			currAttributes = line.substring(idx2, currTagEnd);
			if (result.endsWith("/"))
				result = result.substring(0, result.length() - 1);
			return result;
		} else
			return "";
	}

	private String getAttribute(String attrName) {
		return getBetween(currAttributes, attrName + "=\"", "\"");
	}

	private String extract(String tagName, String id, String htmlSrc) {
		String result = "";
		int stage = 0;//0:finding tag; 1:finding enclosing tag
		int level = 0;
		int tagBegin = -1;
		int tagEnd = -1;
		CurrPos = 0;
		String tag = getTag(htmlSrc);
		while (!tag.isEmpty()) {
			if (stage == 0) {
				if (tag.equalsIgnoreCase(tagName) && getAttribute("id").equals(id)) {
					tagBegin = currTagBegin;
					stage = 1;
				}
			} else if (stage == 1) {
				if (tag.equalsIgnoreCase(tagName)) {
					level++;
				} else if (tag.equalsIgnoreCase("/" + tagName)) {
					if (level == 0) {
						tagEnd = currTagEnd;
						break;
					} else
						level--;
				}
			}
			tag = getTag(htmlSrc).toLowerCase();
		}
		if ((tagEnd > 0) && (tagBegin > 0))
			result = htmlSrc.substring(tagBegin, tagEnd + 1);
		return result;
	}

	public static String extractTagById(String tagName, String id, String htmlSrc) {
		SimpleHtmlParser p = new SimpleHtmlParser();
		return p.extract(tagName, id, htmlSrc);
	}

	public static void processTable(String src, String id, IProcessRow proc) {
		final StringList loader = new StringList();
		final IProcessRow procRow = proc;
		class Looper extends OnBetween {

			@Override
			public void onBetween(String content) {
				content = content.substring(content.indexOf('>') + 1);
				loader.add(content);
			}

		}
		final Looper looper = new Looper();
		StringList.processBetweens(extractTagById("table", id, src), "<tr", "</tr>", new OnBetween() {

			@Override
			public void onBetween(String content) {
				loader.clear();
				StringList.processBetweens(content, "<td", "</td>", looper);
				if (loader.size() == 0)
					StringList.processBetweens(content, "<th", "</th>", looper);
				procRow.processRow(index, loader);
			}
		});
	}

	public static void main(String[] args) {
	}
}
