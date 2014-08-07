/*
 * BaseDAO.java
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
package au.com.systemic.framework.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import au.com.systemic.framework.dao.sql.SQLStatement;

/**
 * Base class for all DAOs. Every DAO should extend this class which provides some common functionality 
 * and definitions.<br><br>
 *                                                       
 * Example of use<br>
 * NA<br><br>
 *                                                       
 * Assumption & Constraints<br>
 * None
 *                                                       
 * @author Joerg Huber
 */

public class BaseDAO
{
  protected final Logger logger = Logger.getLogger(getClass());


  /**
   * This method will close the connection.
   *
   * @param connection The connection to be closed.
   */
  public void closeConnection(Connection connection)
  {
    try
    {
      if ((connection != null) && !connection.isClosed())
      {
        connection.close();
      }
      connection = null;
    }
    catch (SQLException ex)
    {} // nothing we can do!
  }

  /**
   * This method will commit all transactions for this connection and will
   * close it afterwards.
   *
   * @param connection The connection where the commit occurs.
   */
  public void commitAndClose(Connection connection)
  {
    try
    {
      if ((connection != null) && !connection.isClosed())
      {
        connection.commit();
        connection.close();
      }
      connection = null;
    }
    catch (SQLException ex)
    {} // nothing we can do!
  }

  /**
   * This method will roll all transactions for this connection back and will
   * close it afterwards.
   *
   * @param connection The connection where the commit occurs.
   */
  public void rollbackAndClose(Connection connection)
  {
    try
    {
      if ((connection != null) && !connection.isClosed())
      {
        connection.rollback();
        connection.close();
      }
      connection = null;
    }
    catch (SQLException ex)
    {} // nothing we can do!
  }

  /**
   * Convenience Method. Deals with exceptions.
   * This method will close a given result set.
   *
   * @param rs The result set to be closed.
   */
  public void closeResultSet(ResultSet rs)
  {
    try
    {
      if (rs != null)
      {
        rs.close();
      }
    }
    catch (SQLException ex)
    {} // nothing we can do!
  }

  
  /**
   * Pure convenience method for all of the above...
   * @param rs
   */
  public void closeAndCommit(Connection connection, ResultSet rs)
  {
    closeResultSet(rs);
    commitAndClose(connection);
  }

  
  /**
   * Pure convenience method for all of the above...
   * @param rs
   */
  public void closeAndRollback(Connection connection, ResultSet rs)
  {
    closeResultSet(rs);
    rollbackAndClose(connection);
  }
  
  /**
   * Log an exception during a query
   * @param query the name of the query being executed
   * @param ex the exception thrown
   */
  protected void logAndThrowException(Exception ex, SQLStatement stmt) throws DAOException
  {
    logAndThrowException(ex, "Error performing query '" + stmt.toString());
  }

  protected void logAndThrowException(Exception ex, String message) throws DAOException
  {
    logger.error(message, ex);
    throw new DAOException(ex, this, message);
  }

}
