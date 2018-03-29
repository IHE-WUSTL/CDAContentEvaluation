package edu.wustl.mir.erl.IHETools.Util;
import java.io.Closeable;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.log4j.Logger;

/**
 * Encapsulates information pertaining to a particular DB connection:<ul>
 *<li/>{@link java.sql.Connection conn} if a connection has been made, otherwise
 *null.
 *<li/>{@link java.sql.ResultSet lastResultSet} the most recent ResultSet from 
 *this connection, or null if no Queries generating ResultSets have been run.
 *<li/>{@link java.sql.ResultSetMetaData lastSQLMetaData} ResultSetMetaData from
 *this connection, or null.
 *<li/>dbName the database name. Corresponds to a section [dbname_DB] in the 
 *application .ini file which contains values for the JDBC connection properties
 *for this DataBaseConnection.
 *<li/>physicalDbName the physical name of the database in the DB server. 
 *Defaults to the dbName. This is used for multiple profiles. If not the same
 *as the dbName, it is the value of PhysicalDbName in the [dbName_DB] section
 *of the application .ini file.
 *<li/>user the user id used to access the database. Taken from the [dbName_DB]
 *UserId key value in the application .ini file.
 *<li/>password user password used to access the database. Taken from the 
 *[dbName_DB] Password key value in the application .ini file.
 *<li/>driver JDBC driver class used to access the database. Taken from the 
 *[dbName_DB] DriverName key value in the application .ini file.
 *<li/>url JDBC Connection String used to access the database. Taken from the 
 *[dbName_DB] ConnectionString key value in the application .ini file.</ul>
 *<b>Proper Usage:</b><br/>
 *Create a new instance of this class for each Connection you need, using the
 *static method:<br/>
 *DataBaseConnection dbc = 
 *{@link DataBaseConnection#getDataBaseConnection(dbName)};<br/>
 *Make JDBC SLQ queries using {@link Query}, passing the created instance to:<ul>
 *<li/>{@link Query#dbQuery(DataBaseConnection)},
 *<li/>{@link Query#dbUpdate(DataBaseConnection)}, or
 *<li/>{@link Query#dbUpdates(DataBaseConnection)}, as appropriate</ul>
 *<b>Note:</b> DataBaseConnection implements {@link java.io.Closeable Closeable}
 *, so you can create instances using the Java 7 try-with-resources syntax. 
 *
 * @author rmoult01
 */
public class DataBaseConnection implements Serializable, Closeable {
	static final long serialVersionUID = 1L;

	public Connection conn = null;
	public ResultSet lastResultSet = null;
	public ResultSetMetaData lastSQLMetaData = null;
	private String dbName = null;
	private String physicalDbName = null;
	private String user = null;
	private String password = null;
	private String driver = null;
	private String url = null;

	/** Set of JDBC driver classes which have been loaded. */
	private static HashSet<String> drvrs = new HashSet<String>();
	/** Map of DataBaseConnection objects for each dbName which has been loaded */
	private static HashMap<String, DataBaseConnection> dbs =
			new HashMap<String, DataBaseConnection>();

	private DataBaseConnection() {}

	/**
	 * private constructor uses passed dbName to look up DB parameters. The 
	 * first time a DataBaseConnection instance is created for a particular
	 * dbName, property values are retrieved from the application .ini file and
	 * stored in the {@link #dbs} Map. On subsequent instantiations for the 
	 * same dbName, property values are retrieved from the Map.
	 */
	private DataBaseConnection(String dbName) throws Exception {
		this.dbName = dbName;
		String section = dbName + "_DB";
		Logger log = Util.getLog();
		physicalDbName = Util.getParameterString(section, "PhysicalDbName", dbName);
		user = Util.getParameterString(section, "UserId", "syslog");
		password = Util.getParameterString(section, "Password", "syslog");
		driver = Util.getParameterString(section, "DriverName", DBUtil.DRIVER_NAME);
		url = Util.getParameterString(section, "ConnectionString", 
			DBUtil.URL + physicalDbName);

		if (!drvrs.contains(driver)) {
			try {
				Class.forName(driver).newInstance();
			} catch (Exception e) {
				log.error("Couldn't load " + driver + " " + e.getMessage());
				throw new Exception(e);
			}
			drvrs.add(driver);
		}

	} // EO private constructor

	//--- Implements Closeable interface, allowing use in try with resources
	public void close() {
		closeResultSet();
		closeConnection();
	}

	/**
	 * Returns a <b>new</b> {@link gov.nist.syslog.util.DataBaseConnection
      DataBaseConnection} object for passed data base name. Loads parameters
	 * from .ini file if needed. There will be no active 
	 * {@link java.sql.Connection Connection}.
	 * @param dbName String name of database. Corresponds to a section 
	 * [dbname_DB] in the application .ini file which contains values for the J
	 * DBC connection properties for this DataBaseConnection.
	 * @return DatabaseConnection object
	 * @throws Exception on error.
	 */
	public static DataBaseConnection getDataBaseConnection(String dbName)
			throws Exception {
		if (!dbs.containsKey(dbName))
			dbs.put(dbName, new DataBaseConnection(dbName));
		DataBaseConnection o = dbs.get(dbName);
		DataBaseConnection c = new DataBaseConnection();
		c.dbName = o.dbName;
		c.physicalDbName = o.physicalDbName;
		c.user = o.user;
		c.password = o.password;
		c.driver = o.driver;
		c.url = o.url;
		return c;
	}

	/**
	 * Returns current value of connection property.  May be a valid
	 * {@link java.sql.Connection Connection} or null.
	 * @return
	 */
	public Connection getConn() {
		return conn;
	}

	/**
	 * Gets a valid {@link java.sql.Connection Connection} for this
	 * {@link gov.nist.registry.syslog.util.DataBaseConnection DataBaseConnection}
	 * object. Opens a new Connection if needed.
	 * @return Connection
	 * @throws Exception on error
	 */
	public Connection getConnection() throws Exception {
		if (conn == null)
			conn = DriverManager.getConnection(url, user, password);
		return conn;
	}

	/**
	 * Closes the {@link java.sql.Connection Connection} for this
	 * {@link gov.nist.registry.syslog.util.DataBaseConnection DataBaseConnection}
	 * if open. Any exceptions are swallowed.
	 */
	public void closeConnection() {
		if (conn == null) return;
		try {
			conn.close();
			conn = null;
		} catch (Exception e) {
		}
	}
	/**
	 * Closes the {@link java.sql.ResultSet ResultSet for this
	 * {@link gov.nist.registry.syslog.util.DataBaseConnection DataBaseConnection}
	 * if open. Any exceptions are swallowed.
	 */
	public void closeResultSet() {
		if (lastResultSet == null) return;
		try {
			lastResultSet.close();
			lastResultSet = null;
			lastSQLMetaData = null;
		} catch (Exception e) { }
	}

	public ResultSet getLastResultSet() {
		return lastResultSet;
	}

	public void setLastResultSet(ResultSet lastResultSet) {
		this.lastResultSet = lastResultSet;
	}

	public ResultSetMetaData getLastSQLMetaData() {
		return lastSQLMetaData;
	}

	public void setLastSQLMetaData(ResultSetMetaData lastSQLMetaData) {
		this.lastSQLMetaData = lastSQLMetaData;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}
	public String getPhysicalDbName() {
		return physicalDbName;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String toString() {
		return new StringBuilder("[")
		.append(this.getClass().getName()).append(": ")
		.append("dbName=").append(dbName)
		.append(", driver=").append(driver)
		.append(", url=").append(url)
		.append(", user=").append(user)
		.append(", pw=").append(password)
		.append((conn != null) ? " connected" : " not connected")
		.append((lastResultSet != null) ? " has results" : "no results")
		.append("]")
		.toString();
	}
}


