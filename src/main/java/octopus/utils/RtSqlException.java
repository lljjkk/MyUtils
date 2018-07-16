package octopus.utils;

import java.sql.SQLException;

public class RtSqlException extends RuntimeException {
	private static final long serialVersionUID = 4330939766095111071L;
	private SQLException originException = null;

	public RtSqlException(SQLException originException) {
		super(originException.getMessage());
		this.originException = originException;
	}

	public static RtSqlException create(SQLException originException) {
		RtSqlException result = new RtSqlException(originException);
		return result;
	}

	@Override
	public void printStackTrace() {
		originException.printStackTrace();
	}
}
