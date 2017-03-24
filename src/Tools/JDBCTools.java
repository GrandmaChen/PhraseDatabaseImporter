package Tools;

import java.sql.Connection;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * JDBC 的工具类
 * 
 * 其中包含: 获取数据库连接, 关闭数据库资源等方法.
 */
public class JDBCTools {

	private static DataSource dataSource = null;

	// 数据库连接池应只被初始化一次.
	static {
		dataSource = new ComboPooledDataSource("helloc3p0");
	}

	public static Connection getConnection() throws Exception {
		return dataSource.getConnection();
	}

}
