/*
 * SQLStatement.java
 *
 * Copyright 2003-2014 Systemic Pty Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License 
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package au.com.systemic.framework.dao.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import au.com.systemic.framework.dao.BaseObject;
import au.com.systemic.framework.dao.ValueObjectTypes;
import au.com.systemic.framework.utils.DateUtils;
import au.com.systemic.framework.utils.StringUtils;

/**
 * This class provides some functionality similar to a prepared statement. The advantage is that 
 * parameters of an SQL statement can be used with logical names of the form ':varName' instead of 
 * a simple '?'. This allows also to bind a value to more than one variable if the variable has the 
 * same name.<br /><br />
 * 
 * Example of use<br />
 * SQL: 'select * from ABC where a=:var1 and n=:var2 and :var1>:var2'<br />
 * The method bind('var1', 134); will bind both occurances of :var1 in above statement.<br /><br />
 * 
 * Assumption & Constraints<br />
 * None 
 *
 * @author Joerg Huber
 */
public class SQLStatement
{
  protected final Logger logger = Logger.getLogger(getClass());

  public static final int   DATE_ONLY             = 0;
  public static final int   TIME_ONLY             = 1;
  public static final int   DATE_AND_TIME         = 2;
  private static final int  UNSPECIFIED           = Integer.MIN_VALUE;

  // common dynamic tokens
  public static final String     SELECT_CLAUSE_TOKEN   = "$selectclause";
  public static final String     FROM_CLAUSE_TOKEN     = "$fromclause";
  public static final String     JOIN_CLAUSE_TOKEN     = "$joinclause";
  public static final String     WHERE_CLAUSE_TOKEN    = "$whereclause";
  public static final String     ORDER_BY_CLAUSE_TOKEN = "$orderclause";

  private final SQLStatementInfo sqlStatementInfo;

  private Map bindValues        = new HashMap();
  private Map dynamicBindValues = new HashMap();

  // cached objects
  private PreparedStatement      cachedPreparedStatement;
  private List                   cachedBindVariableOrdering;
  private Connection             cachedConnection;
  private int                    cachedResultSetType;
  private int                    cachedResultSetConcurrency;

  /**
   * Constructs a SQLStatement object that can be used to execute SQL statements. It requires a 
   * SQL statement that conforms the notation as described in the class banner. Further each SQL 
   * statement should have an ID that identifies the statement.
   * 
   * The statement must contain a ':varName' tags for varaiables that shall be bound later. 
   * Example: select * from ABC where ABC.xyz = :someValue 
   * In Above example the ':someValue' can be bound before execution with the 'bind' methods of this 
   * class.
   * 
   * @param sqlStatementInfo The SQL statement information
   */
  public SQLStatement(SQLStatementInfo sqlStatementInfo)
  {
    this.sqlStatementInfo = sqlStatementInfo;
    clearCache();
  }

  /**
   * Getter method for sqlID
   * 
   * @return String
   */
  public String getSqlID()
  {
    return sqlStatementInfo.getSqlID();
  }

  public SQLStatementInfo getSqlStatementInfo()
  {
    return sqlStatementInfo;
  }

  /**
   * Binds a variable to the SQL statement. The 'varName' must be of the form 'varName' without 
   * the ':' as in the SQL statement. If there are no variables with the given name then no action 
   * is performed.
   * 
   * @param varName The Name of the varibale to bind.
   * @param value The value to bind.
   */
  public void bindDynamic(String varName, String value)
  {
    // statement must be re-prepared
    clearCache();
    dynamicBindValues.put(varName, new BaseObject(value));
  }

  /**
   * Binds a variable to the SQL statement. The 'varName' must be of the form
   * 'varName' without the ':' as in the SQL statement. If there are no
   * variables with the given name then no action is performed.
   * 
   * @param varName
   *          The Name of the varibale to bind.
   * @param dynamicList
   *          The ArrayList to bind.
   */
  public void bindDynamic(String varName, ArrayList dynamicList)
  {
    bindDynamic(varName, StringUtils.join(dynamicList, ", "));
  }

  /**
   * Binds a variable to the SQL statement. The 'varName' must be of the form
   * 'varName' without the ':' as in the SQL statement. If there are no
   * variables with the given name then no action is performed.
   * 
   * @param varName
   *          The Name of the varibale to bind.
   * @param value
   *          The value to bind.
   */
  public void bind(String varName, String value)
  {
    bindValues.put(varName, new BaseObject(value));
  }

  /**
   * Binds a variable with a sequence of values.
   * 
   * @param varName Varibale name of the parameter to bind
   * @param values Collection of values to bind with above variable name.
   */
  public void bind(String varName, Collection values)
  {
    clearCache();
    bindValues.put(varName, values);
  }

  /**
   * Overloaded method.
   */
  public void bind(String varName, int value)
  {
    bindValues.put(varName, new BaseObject(value));
  }

  /**
   * Overloaded method.
   */
  public void bind(String varName, double value)
  {
    bindValues.put(varName, new BaseObject(value));
  }

  /**
   * Overloaded method.
   */
  public void bind(String varName, boolean value)
  {
    bindValues.put(varName, new BaseObject(value));
  }

  /**
   * Overloaded method.
   */
  public void bind(String varName, long value)
  {
    bindValues.put(varName, new BaseObject(value));
  }

  /**
   * This will bind a date to the appropriate variable. The dateType is required
   * to indicate if only the date, time or both components are required. This
   * function assumes that the following formats for date and time are used by
   * the underlying database.
   * 
   * Date: String of form: YYYYMMDD Time: String of form: hhmmss (hh = 24 hours)
   * Date & Time: String of form: YYYYMMDDhhmmss (hh = 24 hours)
   * 
   * @param varName
   *          Name of the variable to bind.
   * @param value
   *          Value to bind.
   * @param dateType
   *          Part of the date to use.
   * 
   * @throws SQLException
   *           If the variable cannot be bound.
   */
  public void bind(String varName, Date value, int dateType)
      throws SQLException
  {
    if (dateType == DATE_AND_TIME)
    {
      bindValues.put(varName, new BaseObject(DateUtils.dateToString(value, DateUtils.DB_DATE_TIME)));
    }
    else if (dateType == DATE_ONLY)
    {
      bindValues.put(varName, new BaseObject(DateUtils.dateToString(value, DateUtils.DB_DATE)));
    }
    else if (dateType == TIME_ONLY)
    {
      bindValues.put(varName, new BaseObject(DateUtils.dateToString(value, DateUtils.DB_TIME)));
    }
    else
    {
      throw new SQLException("Invalid dateType");
    }
  }

  /**
   * bind a BaseObject directly
   * 
   * @param varName String
   * @param value BaseObject
   */
  public void bind(String varName, BaseObject value)
  {
    bindValues.put(varName, value);
  }

  /**
   * This method will clear all bind variable values. After this call it is
   * required to call the 'bind' methods again before the 'execute' method can
   * be used.
   */
  public void resetBindVariables()
  {
    bindValues.clear();
    dynamicBindValues.clear();
  }

  /**
   * This will execute the SQL statement and return the result as a ResultSet.
   * If an error occured SQLException is thrown and an error is logged. If your
   * statement does not return a result set then use
   * <code>execute(Connection)</code>.
   * 
   * @param conn
   *          The connection where to execute the statement.
   * 
   * @throws SQLException
   *           Statement failed to execute.
   */
  public ResultSet executeQuery(Connection conn) throws SQLException
  {
    long start = 0;
    long end = 0;
    try
    {
      if (logger.isDebugEnabled())
      {
        start = System.currentTimeMillis();
        logger.debug(this.toString());
      }
      useConnection(conn);
      return prepareStatement().executeQuery();
    }
    finally
    {
      if (logger.isDebugEnabled())
      {
        end = System.currentTimeMillis();
        logger.debug("Total Time to execute SQL: " + (end - start) + " ms");
      }

    }
  }

  /**
   * This will execute the SQL statement. If an error occured SQLException is
   * thrown and an error is logged. If your statement does returns a result set
   * then use <code>executeQuery(Connection)</code>.
   * 
   * @param conn
   *          The connection where to execute the statement.
   * 
   * @throws SQLException
   *           Statement failed to execute.
   */
  public void execute(Connection conn) throws SQLException
  {
    long start = 0;
    long end = 0;
    try
    {
      if (logger.isDebugEnabled())
      {
        start = System.currentTimeMillis();
        logger.debug(this.toString());
      }
      useConnection(conn);
      prepareStatement().execute();
    }
    finally
    {
      if (logger.isDebugEnabled())
      {
        end = System.currentTimeMillis();
        logger.debug("Total Time to execute SQL: " + (end - start) + " ms");
      }
    }
  }

  /**
   * This will execute the SQL statement and return the result as a ResultSet.
   * If an error occured SQLException is thrown and an error is logged. If your
   * statement does not return a result set then use
   * <code>execute(Connection)</code>.
   * 
   * @param conn
   *          The connection where to execute the statement.
   * 
   * @throws SQLException
   *           Statement failed to execute.
   */
  public ResultSet executeQuery(Connection conn, int resultSetType,
      int resultSetConcurrency) throws SQLException
  {
    long start = 0;
    long end = 0;
    try
    {
      if (logger.isDebugEnabled())
      {
        start = System.currentTimeMillis();
        logger.debug(this.toString());
      }
      useConnection(conn, resultSetType, resultSetConcurrency);
      return prepareStatement().executeQuery();
    }
    finally
    {
      if (logger.isDebugEnabled())
      {
        end = System.currentTimeMillis();
        logger.debug("Total Time to execute SQL: " + (end - start) + " ms");
      }
    }
  }

  /**
   * This will execute the SQL statement. If an error occured SQLException is
   * thrown and an error is logged. If your statement does returns a result set
   * then use <code>executeQuery(Connection)</code>.
   * 
   * @param conn
   *          The connection where to execute the statement.
   * 
   * @throws SQLException
   *           Statement failed to execute.
   */
  public void execute(Connection conn, int resultSetType,
      int resultSetConcurrency) throws SQLException
  {
    long start = 0;
    long end = 0;
    try
    {
      if (logger.isDebugEnabled())
      {
        start = System.currentTimeMillis();
        logger.debug(this.toString());
      }
      useConnection(conn, resultSetType, resultSetConcurrency);
      prepareStatement().execute();
    }
    finally
    {
      if (logger.isDebugEnabled())
      {
        end = System.currentTimeMillis();
        logger.debug("Total Time to execute SQL: " + (end - start) + " ms");
      }
    }
  }

  /**
   * Use the specified Connection
   * 
   * @param conn
   *          Connection
   */
  private void useConnection(Connection conn)
  {
    useConnection(conn, UNSPECIFIED, UNSPECIFIED);
  }

  /**
   * Use the specified Connection, ResultSetType, ResultSetType
   * 
   * @param conn
   *          Connection
   * @param resultSetType
   *          int
   * @param resultSetConcurrency
   *          int
   */
  private void useConnection(Connection conn, int resultSetType,
      int resultSetConcurrency)
  {
    if (conn == this.cachedConnection
        && resultSetType == this.cachedResultSetType
        && resultSetConcurrency == this.cachedResultSetConcurrency)
    {
      // specified connection settings already in use
      return;
    }

    clearCache();
    this.cachedConnection = conn;
    this.cachedResultSetType = resultSetType;
    this.cachedResultSetConcurrency = resultSetConcurrency;
  }

  /**
   * Clear the cached prepared statement
   */
  private void clearCache()
  {
    cachedPreparedStatement = null;
    cachedBindVariableOrdering = new ArrayList();
    cachedConnection = null;
    cachedResultSetType = UNSPECIFIED;
    cachedResultSetConcurrency = UNSPECIFIED;
  }

  private boolean useCache()
  {
    return cachedPreparedStatement != null;
  }

  /**
   * Convenience Method.
   */
  public String toString()
  {
    return "SQL ID: "
        + sqlStatementInfo.getSqlID()
        + "\nSQL Statement: "
        + substituteDirect(sqlStatementInfo.getSqlStatement(),
            dynamicBindValues) + "\nParameters: " + this.bindValues;
  }

  /*
   * Prepares this statement for execution.
   */
  private PreparedStatement prepareStatement() throws SQLException
  {
    if (!useCache())
    {
      // prepare our statement
      cachedBindVariableOrdering.clear();

      // substitute dynamic bind vars first (in case they contain bind vars)
      String preparedSQL = substituteDirect(sqlStatementInfo.getSqlStatement(),
          dynamicBindValues);

      // now bind variables
      preparedSQL = substituteBindVars(preparedSQL, bindValues,
          cachedBindVariableOrdering);

      if (cachedResultSetType == UNSPECIFIED
          && cachedResultSetConcurrency == UNSPECIFIED)
      {
        cachedPreparedStatement = cachedConnection
            .prepareStatement(preparedSQL);
      }
      else
      {
        cachedPreparedStatement = cachedConnection.prepareStatement(
            preparedSQL, cachedResultSetType, cachedResultSetConcurrency);
      }
    }

    // bind and return
    bind(cachedPreparedStatement, cachedBindVariableOrdering, bindValues);
    return cachedPreparedStatement;
  }

  private static void bind(PreparedStatement stmt, List bindVariableOrder,
      Map boundValues) throws SQLException
  {
    // Bind all the variables
    int posn = 0;
    for (Iterator i = bindVariableOrder.iterator(); i.hasNext();)
    {
      Object value = boundValues.get(i.next());

      if (value instanceof Collection)
      {
        // bind each value in the collection
        Collection coll = (Collection) value;
        for (Iterator iter = coll.iterator(); iter.hasNext();)
        {
          bind(stmt, ++posn, (BaseObject) iter.next());
        }
      }
      else
      {
        // bind single value
        bind(stmt, ++posn, (BaseObject) value);
      }
    }
  }

  private static void bind(PreparedStatement stmt, int posn, BaseObject value)
      throws SQLException
  {
    if (value == null)
    {
      // null values not permitted
      throw new SQLException("Attempt to bind null value");
    }
    else
    {
      // bind
      ValueObjectTypes type = value.getType();

      if (type == ValueObjectTypes.STRING)
      {
        stmt.setString(posn, value.getString());
      }
      else if (type == ValueObjectTypes.INTEGER)
      {
        stmt.setInt(posn, value.getInt());
      }
      else if (type == ValueObjectTypes.LONG)
      {
        stmt.setLong(posn, value.getLong());
      }
      else if (type == ValueObjectTypes.DOUBLE)
      {
        stmt.setDouble(posn, value.getDouble());
      }
      else if (type == ValueObjectTypes.BOOLEAN)
      {
        stmt.setBoolean(posn, value.getBoolean());
      }
      else
      {
        throw new SQLException("Type " + type  + " from ValueObjectConstants class is not supported as a variable to bind.");
      }
    }
  }

  /**
   * This method parses the SQL Statement and replaces all ':varName' with '?'
   * as required by the JDBC prepared statement notation. The ':varName' in turn
   * are stored in a ArrayList so that their value can be replaced by the 'bind'
   * methods.
   * 
   * PRE-CONDITION: all variables must be bound before this method is invoked
   * 
   * @param sqlStatement
   *          The raw SQL statement.
   */
  private static String substituteBindVars(String sqlStatement, Map valueMap,
      final List bindVariableOrdering) throws SQLException
  {
    final int sqlLength = sqlStatement.length();
    StringBuffer newSQLStatement = new StringBuffer(sqlLength);
    StringBuffer sqlVar = null;

    for (int i = 0; i < sqlLength; i++)
    {
      char c = sqlStatement.charAt(i);

      if (sqlVar != null) // we are currently processing a variable
      {
        if (Character.isLetterOrDigit(c) || (c == '_')) // still within variable
                                                        // name
        {
          sqlVar.append(c);
        }
        else
        // end of variable name
        {
          // Add variable to array
          String bindVarName = sqlVar.toString();
          sqlVar = null;

          bindVariableOrdering.add(bindVarName);

          Object binding = valueMap.get(bindVarName);
          if (binding == null)
          {
            // bind value not specified
            throw new SQLException("Bind Variable '" + bindVarName
                + "' not bound");
          }

          if (binding instanceof Collection)
          {
            // add a ? for each item in the collection
            for (int j = 1; j < ((Collection) binding).size(); j++)
            {
              newSQLStatement.append(',');
              newSQLStatement.append('?');
            }
          }

          newSQLStatement.append(c);
        }
      }
      else if ((c == ':') && (i < sqlLength)
          && (Character.isLetter(sqlStatement.charAt(i + 1))))
      {
        // This is the start of a new variable
        sqlVar = new StringBuffer();
        newSQLStatement.append('?');
      }
      else
      // standard character not part of a variable
      {
        newSQLStatement.append(c);
      }
    }

    // We may still have a open variable that needs to be added to the
    // bindVariables
    if (sqlVar != null) // we are currently processing a variable
    {
      bindVariableOrdering.add(sqlVar.toString());
    }

    return newSQLStatement.toString();
  }

  /**
   * returns the SQL Statement with Dynamic Bind vars substituted
   * 
   * @return String
   */
  private static String substituteDirect(String sqlStatement, Map valueMap)
  {
    for (Iterator i = valueMap.entrySet().iterator(); i.hasNext();)
    {
      Map.Entry bind = (Map.Entry) i.next();
      String key = (String) bind.getKey();
      BaseObject value = (BaseObject) bind.getValue();
      sqlStatement = StringUtils.replaceAll(sqlStatement, key, value.getString());
    }

    return sqlStatement;
  }

  public static void main(String arg[])
  {
    try
    {
      String sqlStatement = "select test.id, test.name, test.long_name "
          + " from TEST_TABLE test "
          + " where test.id = :id "
          + " order by $orderclause";

      SQLStatementInfo sqlStatementInfo = new SQLStatementInfo("", sqlStatement, "testSource");
      SQLStatement stmt = new SQLStatement(sqlStatementInfo);
      stmt.bindDynamic(ORDER_BY_CLAUSE_TOKEN, "name, long_name");

      System.out.println(substituteDirect(stmt.sqlStatementInfo.getSqlStatement(), stmt.dynamicBindValues));
    }
    catch (Exception e)
    {
      System.out.println("Error: " + e.getMessage());
    }
  }


}
