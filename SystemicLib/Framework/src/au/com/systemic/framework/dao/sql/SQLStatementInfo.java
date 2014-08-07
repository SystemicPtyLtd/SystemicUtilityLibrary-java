/*
 * SQLStatementInfo.java
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

/**
 * @author Joerg Huber
 */
public class SQLStatementInfo
{
  private final String sqlID;
  private final String sqlStatement;
  private final String datasourceName;

  public SQLStatementInfo(String sqlID, String sqlStatement, String datasourceName)
  {
    this.sqlID = sqlID;
    this.sqlStatement = sqlStatement;
    this.datasourceName = datasourceName;
  }

  public String getSqlID()
  {
    return sqlID;
  }

  public String getSqlStatement()
  {
    return sqlStatement;
  }

  public String getDatasourceName()
  {
    return datasourceName;
  }
}
