/*
 * DAOException.java
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

/**
 * @author Joerg Huber
 */
public class DAOException extends Exception
{
  private static final long serialVersionUID = 1L;

  private final Exception exception;
  private final BaseDAO   dao;
  private final String    message;

  DAOException(Exception exception, BaseDAO dao, String message)
  {
    this.exception = exception;
    this.dao = dao;
    this.message = message;
  }

  DAOException(Exception exception, BaseDAO dao)
  {
    this(exception, dao, "{No Message}");
  }

  public BaseDAO getDao()
  {
    return dao;
  }

  public Exception getException()
  {
    return exception;
  }

  public String getMessage()
  {
    return message;
  }

  public String toString()
  {
    return exception.getClass().getName() + " in " + dao.getClass().getName() + ": " + message;
  }

}
