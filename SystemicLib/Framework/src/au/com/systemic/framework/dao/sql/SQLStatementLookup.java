/*
 * SQLStatementLookup.java
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;

import au.com.systemic.framework.utils.XMLUtils;


/**
 * This class provides a lookup table for SQL statements. It is implemented as a singleton with a 
 * factory method. The main singleton holds the sql statements for each DAO class where as the 
 * factory method determines based in the DAO class name which SQLStatementLookup object to return.<br><br>
 *                                                       
 * Example of use<br>
 * SQLStatementLookup factory = SQLStatementLookup.getInstance(DemoDAO.class);<br>
 * SQLStatement stmt = factory.getStatement("getAllLibraries");<br>
 * Connection conn = factory.getConnection("getAllLibraries");<br>
 * ...<br><br>
 *                                                       
 * Assumption & Constraints<br>
 * It is assumed that a DAO class has a *.sql file in the same directory with the same name as the class
 * name. For example if a class is called DemoDAO.class it must have a file called DemoDAO.sql in the 
 * same directory as the class file. The *.sql file must adhere to the following structure:<br><br>
 * <statements>
 *
 *    <statement name="id">
 *       <![CDATA[
 *          sqlStatement
 *      ]]>
 *    </statement>
 *
 *    <statement name="id">
 *       <![CDATA[
 *          sqlStatement
 *      ]]>
 *    </statement>
 *
 *    etc.
 * </statements>
 *
 * @author Joerg Huber
 */
public class SQLStatementLookup
{
  protected final Logger logger = Logger.getLogger(getClass());
  
  private static final HashMap<Class, SQLStatementLookup> allClasses = new HashMap<Class, SQLStatementLookup>();
  private HashMap<String, SQLStatementInfo> statements    = new HashMap<String, SQLStatementInfo>();

  /**
   * Initialiser
   *
   * @param clientClass the client of this factory
   */
  private SQLStatementLookup(Class clientClass)
  {
    String sqlFileName = getClassName(clientClass) + ".sql";

    readStatements(loadSqlDocument(sqlFileName, clientClass), sqlFileName);
  }

  /**
   * Static initialiser for SQLStatementLookup.
   *
   * @param clientClass the client of this factory
   * @return a SQLStatementLookup for the client
   */
  public static synchronized SQLStatementLookup getInstance(Class clientClass)
  {
    SQLStatementLookup thisClass = (SQLStatementLookup)allClasses.get(clientClass);

    if (thisClass == null) // does not exists yet => create the lot
    {
      thisClass = new SQLStatementLookup(clientClass);
      allClasses.put(clientClass, thisClass);
    }
    return thisClass;
  }

  /**
   * This method returns the SQLStatemnt object for the given stmtID. If
   * no such object exists then null os returned.
   *
   * @param stmtID The ID for which a SQLStatement object shall be returned.
   *
   * @return SQLStatement or null.
   */
  public SQLStatement getStatement(String stmtID)
  {
    SQLStatementInfo sqlStatementInfo = (SQLStatementInfo)statements.get(stmtID);
    return new SQLStatement(sqlStatementInfo);
  }

  //-------------------
  // Private Methods --
  //-------------------

  /**
   * This method determins the class name without the full dot notation and
   * retruns it. For example this class is 'com.ebooks.ebl.sql.SQLStatementFactory'.
   * This method would only return 'SQLStatementFactory'.
   *
   * @param clazz The Class for which the name shall be returned.
   */
  private String getClassName(Class clazz)
  {
    String className = clazz.getName(); // gets full name incl
    int dotPos = className.lastIndexOf(".");

    if (dotPos != -1)
    {
      className = className.substring(dotPos + 1);
    }

    return className;
  }

  /**
   * Loads the given file which contains sql the statements. If the file
   * cannot be loaded or has an incorrect format then null is returned.
   *
   * @param sqlFileName The file name of the sql file top be loaded.
   * @param clazz The class through which the sql is loaded.
   *
   * @return A xml Document that can be parsed afterwards. Null is returned
   *         if the file doesn't exist or has incorrect data (no XML).
   */
  public synchronized Document loadSqlDocument(String sqlFileName, Class clazz)
  {
    return XMLUtils.load(sqlFileName, clazz);
  }

  /**
   * This method parses the XML and stores each 'statement' with its
   * datasource in the appropriate varaible.
   *
   * @param doc The JDOM documet to be parsed.
   * @param sqlFileName Name of the SQL file.
   */
  private void readStatements(Document doc, String sqlFileName)
  {
    Element root = doc.getRootElement();
    String dfltDataSource = root.getAttributeValue("datasource");
    if (dfltDataSource == null)
    {
      logger.info("No default datasource given for "+sqlFileName+".");
    }
    
    // Get all statements of the sql file.
    Collection elements = root.getChildren("statement");

    for (Iterator i = elements.iterator(); i.hasNext();)
    {
      Element item = (Element) i.next();
      String name = item.getAttributeValue("name");
      String dataSource = item.getAttributeValue("datasource");

      if (name != null)
      {
        if (dataSource == null)
        {
          dataSource = dfltDataSource;
        }
        SQLStatementInfo sqlStatementInfo = new SQLStatementInfo(name, item.getTextNormalize(), dataSource);
        statements.put(name, sqlStatementInfo);
      }
      else
      {
        logger.error("The file " + sqlFileName + " contains statements without a name.");
      }
    }
  }
}
