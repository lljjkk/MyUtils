package octopus.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/***
 * @author Yang Dong
 * Usage
 * Creating a logger instance within a class:
 * 	public static RemoteLogger logger = new RemoteLogger("http://YourHost/DebugYang/log", "ReceiveLog.pas|@OnHandleStrData", "DebuggerYang");
 * Using the logger:
 *  logger.log("Your Log Information");
 */
public class RemoteLogger {
	private String url;
	private URL urlObj;
	private HttpURLConnection httpConn;
	private static boolean globalLoggerSwitch = true;
	private static int globalLoggerLevel = 0;

	public static void setEnabled(boolean enabled) {
		globalLoggerSwitch = enabled;
	}

	public static void setLogLevel(int level) {
		globalLoggerLevel = level;
	}

	public RemoteLogger(String url, String script, String editor) {
		super();
		try {
			this.url = url + "?script=" + URLEncoder.encode(script, "UTF-8")
					+ "&editor=" + URLEncoder.encode(editor, "UTF-8");
			urlObj = new URL(this.url);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public int log(int level, String line) {
		if (!globalLoggerSwitch)
			return 0;
		if (level >= globalLoggerLevel)
			return log(line);
		else
			return 0;
	}

	public synchronized int log(String line) {
		if (!globalLoggerSwitch)
			return 0;
		int responseCode = -1;
		for (int i = 0; i < 6; i++)
			try {
				httpConn = (HttpURLConnection) urlObj.openConnection();
				httpConn.setDoOutput(true);
				httpConn.setDoInput(true);
				httpConn.setUseCaches(false);
				httpConn.setRequestMethod("POST");
				httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				httpConn.setRequestProperty("Connection", "Keep-Alive");
				httpConn.setRequestProperty("Charset", "UTF-8");
				DataOutputStream dos = new DataOutputStream(httpConn.getOutputStream());
				dos.writeBytes("content=");
				dos.writeBytes(URLEncoder.encode(line, "UTF-8"));
				dos.flush();
				dos.close();
				responseCode = httpConn.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK)
					return responseCode;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				httpConn.disconnect();
			}
		return responseCode;
	}
}
