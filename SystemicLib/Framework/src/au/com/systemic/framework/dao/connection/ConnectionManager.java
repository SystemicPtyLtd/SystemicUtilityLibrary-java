/*
 * ConnectionManager.java
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
package au.com.systemic.framework.dao.connection;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import au.com.systemic.framework.utils.PropertyFileReader;
import au.com.systemic.framework.utils.StringUtils;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * This class manages connections to databases. It requires a property file called 
 * jdbc_connections.properties. This file must be in a directory that is part of the classpath for it
 * to be readable.<br/><br/>
 * 
 * Connection pools will be setup. The pool properties are read from the c3p0.properties file.<br/><br/>
 * 
 * In the jdbc_connections.properties file the following properties must be set for each pool:<br/>
 *   jdbc.driver.<datasoruce>=...<br/>
 *   jdbc.url.<datasoruce>=...<br/>
 *   jdbc.username.<datasoruce>=...<br/>
 *   jdbc.password.<datasoruce>=...<br/><br/>
 *   
 * The <datasource> is the name of the pool and will be required as part of the getConnection() method.<br/><br/>
 * 
 * The property jdbc.pools=<comma_separated_list_of_datasource_name> will be used to determine which
 * pools to set up.
 * 
 * @author Joerg Huber
 */
public class ConnectionManager
{
  private static final String JDBC_CONNECTION_FILE_NAME = "jdbc_connections";
  
  protected static Logger logger = Logger.getLogger(ConnectionManager.class);

  private static HashMap<String, ComboPooledDataSource> connectionPools = null;
  
  public static Connection getConnection(String datasource)
  {
    try
    {
      if (connectionPools == null)
      {
        initDBConnectionPools();
        if (connectionPools == null) // no pools are available
        {
          return null;
        }
      }
      
      ComboPooledDataSource pool = connectionPools.get(datasource);
      if (pool == null) // no connection pool for this data source.
      {
        logger.error("There is no connection pool for datasource '" + datasource + "'."); 
        return null;
      }
      
      Connection connection = pool.getConnection();
      if (logger.isDebugEnabled())
      {
        logger.debug("# connections/idle connections/busy connections : " + pool.getNumConnections()+" / "+pool.getNumIdleConnections()+" / "+pool.getNumBusyConnections()); 
      }
      return connection;
    }
    catch (Exception ex)
    {
      logger.error("Failed to retrieve a DB connection: "+ex.getMessage(), ex);
    }
    return null;
  }
  
  /**
   * Convenience method to close/return a connection to the pool without worring about an exception if
   * connection isn't open or during the close process.
   * 
   * @param connection To close/return to pool.
   */
  public static void closeConnection(Connection connection)
  {
    if (connection != null)
    {
      try 
      {
        connection.close();
      } 
      catch (Exception ex) {} // nothing we can do... 
    }
  }
  
  
  public static void closeConnectionPools()
  {
    if (connectionPools != null)
    {
      for (String poolName : connectionPools.keySet())
      {
        logger.info("Shuting down connection pool '"+poolName+"'...");
        connectionPools.get(poolName).close();
        logger.info("Connection pool shut down.");
      }
      connectionPools.clear();
    }
    connectionPools = null;
  }
  
  
  /*---------------------*/
  /*-- Private Methods --*/
  /*---------------------*/
  private static void initDBConnectionPools() throws Exception
  {
	  PropertyFileReader pfr = new PropertyFileReader(JDBC_CONNECTION_FILE_NAME);
		Properties properties = pfr.getProperties();
		if (properties != null)
		{
			String pools = properties.getProperty("jdbc.pools");
			if (pools != null)
			{
				for (String datasource : StringUtils.split(pools, ","))
				{
					datasource = datasource.trim();
					ComboPooledDataSource connectionPool = new ComboPooledDataSource();
					connectionPool.setDriverClass(properties.getProperty("jdbc.driver."+ datasource));
					connectionPool.setJdbcUrl(properties.getProperty("jdbc.url." + datasource));
					connectionPool.setUser(properties.getProperty("jdbc.username." + datasource));
					connectionPool.setPassword(properties.getProperty("jdbc.password." + datasource));

					logger.info("Connection pool '" + datasource + "' initialised.");

					if (connectionPools == null)
					{
						connectionPools = new HashMap<String, ComboPooledDataSource>();
					}
					connectionPools.put(datasource, connectionPool);
				}
			}
			else
			{
				logger.error("The property jdbc.pools is not set in " + JDBC_CONNECTION_FILE_NAME + ".properties.");
			}
		}
	}

}
