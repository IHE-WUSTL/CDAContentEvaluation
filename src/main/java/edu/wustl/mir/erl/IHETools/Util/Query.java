package edu.wustl.mir.erl.IHETools.Util;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrSubstitutor;

public class Query implements Serializable {
    private static final long serialVersionUID = 1L;

    private ArrayList<StringBuilder> sql = new ArrayList<StringBuilder>();
    private String lastCommand = null;
    private ArrayList<String> lastSql = new ArrayList<String>();
    private Map<String, String> vars = new HashMap<String, String>();
    private Map<String, String> nulls = new HashMap<String, String>();

    private static String lstCommand = null;
    private static ArrayList<String> lstSql = new ArrayList<String>();

    /**
     * Default constructor. Creates Query with single empty SQL Query String
     */
    public Query() {
      sql.add(new StringBuilder(""));
   }

    /**
     * Creates query with single SQL query string using passed query string as
     * starting point.
     * @param obj - {@link java.lang.Object Object} Starting SQL Query string
     * is the .toString() value of this Object.
     */
    public Query(Object obj) {
      sql.add(new StringBuilder(obj.toString()));
   }

    /**
     * Creates query with query strings made from each of the passed objects in
     * the array.
     * @param objs
     */
   public Query(Object[] objs) {
      for (Object o : objs) {
         sql.add(new StringBuilder(o.toString()));
      }
   }

    /**
     * Appends the passed string to the SQL Query string (the first/0 string if
     * this query has multiple query strings.
     * @param obj {@link java.lang.Object Object} the .toString() value of this
     * Object is appended to the Query.
     * @return a reference to this Query object.
     */
    public Query append(Object obj) {
        sql.get(0).append(obj.toString());
        return this;
    }

    /**
     * Appends the passed string to the SQL Query string indicated by index,
     * numbered 0 - size-1.
     * @param index number of sql line to append to.
     * @param obj {@link java.lang.Object Object} the .toString() value of this
     * Object is appended to the Query.
     * @return a reference to this Query object.
     * @throws Exception on index out of bounds.
     */
    public Query append(int index, Object obj) throws Exception {
	if (index < 0 || index >= sql.size())
	    throw new Exception("append to non-existent Query line " + index);
        sql.get(index).append(obj.toString());
        return this;
    }

    /**
     * Adds the value of the passed object to the query as the next query line.
     * @param obj {@link java.lang.Object Object} the .toString() value of this
     * Object is added as the next query line
     * @return a reference to this Query object.
     */
    public Query add(Object obj) {
	sql.add(new StringBuilder(obj.toString()));
	return this;
    }
    /**
     * Adds the values of each object in the passed array to the query as the
     * next query lines.
     * @param objs {@link java.lang.Object Object} array. the .toString() values
     * of these Objects are added as the next query lines
     * @return a reference to this Query object.
     */
    public Query add(Object[] objs) {
	for (Object o : objs) sql.add(new StringBuilder(o.toString()));
	return this;
    }

    /**
     * Sets a parameter value in the SQL Query string. Parameters must be of
     * the form ${parameter name}, and are handled by a default
     * {@link org.apache.commons.lang.text.StrSubstitutor StrSubstitutor}.<br/>
     * <b>Note:</b> Parameter substitutions are not actually processed until
     * the Query is submitted. If the same parameter is set multiple times, the
     * last value will be the effective one.<br/>
     * <b>Note:</b> Handles null values on inserts.  If the parameter is of the
     * form '${parameter name}' in the SQL (indicating a string value), set will
     * replace the entire expression, including the quotes, with NULL.
     * @param key {@link java.lang.Object Object} the .toString() value of this
     * Object is the parameter name.
     * @param value {@link java.lang.Object Object} the .toString() value of this
     * Object is the value which replaces the parameter entry. This value will
     * be escaped for SQL before use.
     * @return a reference to this Query object.
     */
   public Query set(Object key, Object value) {
      if (value == null) {
         nulls.put(key.toString(), "NULL");
         vars.put(key.toString(), "NULL");
      } else {
         vars.put(key.toString(), StringEscapeUtils.escapeSql(value.toString()));
      }
      return this;
   }

    /**
     * Executes the query on the passed dbName. This call is for queries which
     * return a ResultSet. Uses
     * {@link gov.nist.registry.syslog.util.Util#dbQuery(String, String) Util.dbQuery}
     * @param c DataBaseConnection
     * @return {@link java.sql.ResultSet ResultSet}
     * @throws Exception on error or if query has multiple lines.
     */
   public ResultSet dbQuery(DataBaseConnection c) throws Exception {
      lstCommand = lastCommand = "dbQuery(" + c.getDbName() + ")";
      if (sql.size() > 1)
         throw new Exception("Multi-line query; Use dbUpdates");
      return Util.dbQuery(c, prepQuery(0));
   }

   /**
    * Executes the query on the passed dbName. This call is for queries which
    * return record counts or nothing. Uses
    * {@link gov.nist.registry.syslog.util.Util#dbUpdate(String, String)
    * Util.dbUpdate}
    *
    * @param c DataBaseConnection
    * @return integer result of query.
    * @throws Exception
    *            on error.
    */
   public int dbUpdate(DataBaseConnection c) throws Exception {
      lstCommand = lastCommand = "dbUpdate(" + c.getDbName() + ")";
      if (sql.size() > 1)
         throw new Exception("Multi-line query; Use dbUpdates");
      return Util.dbUpdate(c, prepQuery(0));
   }

   /**
    * Executes the query on the passed dbName. This call is for multiple line
    * queries which return record counts or nothing. Uses
    * {@link gov.nist.registry.syslog.util.Util#dbUpdate(String, String)
    * Util.dbUpdate}
    *
    * @param c DataBaseConnection
    * @return integer arrays with results of queries.
    * @throws Exception
    *            on error.
    */
   public int[] dbUpdates(DataBaseConnection c) throws Exception {
      lstCommand = lastCommand = "dbUpdates(" + c.getDbName() + ")";
      int[] ret = new int[sql.size()];
      for (int i = 0; i < sql.size(); i++)
         ret[i] = Util.dbUpdate(c, prepQuery(i));
      return ret;
   }

   private String prepQuery(int index) {
      String query = sql.get(index).toString();
      if (!nulls.isEmpty()) query = StrSubstitutor.replace(query, nulls, "'${", "}'");
      if (!vars.isEmpty())  query = StrSubstitutor.replace(query, vars);
      Util.getLog().info("    query = " + query);
      lastSql.add(index, query);
      lstSql.add(index, query);
      return query;
   }

   /**
    * Returns last call to Util db routines executed by this instance of
    * Query. Returns "N/A" if no routine has been executed.
    * @return string representation of command, with variables replaced.
    */
   public String lastCommand() {
      if (lastCommand == null) return "N/A";
      StringBuilder str = new StringBuilder(lastCommand).append("\n");
      for (String s : lastSql) str.append(s).append("\n");
      return str.toString();
   }
   /**
    * Returns last call to Util db routines executed by any instance of
    * Query. Returns "N/A" if no routine has been executed.
    * @return string representation of command, with variables replaced.
    */
   public static String LastCommand() {
      if (lstCommand == null) return "N/A";
      StringBuilder str = new StringBuilder(lstCommand).append("\n");
      for (String s : lstSql) str.append(s).append("\n");
      return str.toString();

   }
}