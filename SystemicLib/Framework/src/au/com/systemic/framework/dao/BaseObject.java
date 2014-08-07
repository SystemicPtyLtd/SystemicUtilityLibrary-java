/*
 * BaseObject.java
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

import java.util.Date;

import au.com.systemic.framework.utils.DateUtils;
import au.com.systemic.framework.utils.StringUtils;


/**
 * @author Joerg Huber
 */
public class BaseObject
{
  // ---------------------
  // VARIABLES
  // ---------------------
  /** Holds the type of the object */
  private ValueObjectTypes type  = ValueObjectTypes.OBJECT;

  /** Holds the value of the object. */
  private Object value = null;

  /**
   * Constructor for the full set of supported types.
   */
  public BaseObject(int value)
  {
    set(value);
  }

  public BaseObject(long value)
  {
    set(value);
  }

  public BaseObject(double value)
  {
    set(value);
  }

  public BaseObject(float value)
  {
    set(value);
  }

  public BaseObject(boolean value)
  {
    set(value);
  }

  public BaseObject(String value)
  {
    set(value);
  }

  public BaseObject(Object value)
  {
    set(value);
  }

  /**
   * Stores a data value. The type should be either DATE, TIME or DATE_TIME.
   * 
   * @param value
   * @param type
   */
  public BaseObject(Date value, ValueObjectTypes type)
  {
    set(value, type);
  }

  /**
   * This constructor will first check what the type is and will convert the
   * value to an appropriate Object.
   * 
   * @param type
   *          The type the objetct needs to be converted. Should be one of the
   *          constants defined at the top of this class.
   * @param value
   *          The value of the object.
   */
  public BaseObject(ValueObjectTypes type, String value)
  {
    set(type, value);
  }

  /**
   * Setters. <br>
   * <br>
   * Assumptions & Constraints<br>
   * None
   * 
   * @param value
   *          The value to be set for the object. This will automatically update
   *          the type of the object.
   */
  public void set(int value)
  {
    this.type = ValueObjectTypes.INTEGER;
    this.value = new Integer(value);
  }

  public void set(long value)
  {
    this.type = ValueObjectTypes.LONG;
    this.value = new Long(value);
  }

  public void set(float value)
  {
    this.type = ValueObjectTypes.FLOAT;
    this.value = new Float(value);
  }

  public void set(double value)
  {
    this.type = ValueObjectTypes.DOUBLE;
    this.value = new Double(value);
  }

  public void set(boolean value)
  {
    this.type = ValueObjectTypes.BOOLEAN;
    this.value = new Boolean(value);
  }

  public void set(String value)
  {
    this.type = ValueObjectTypes.STRING;
    this.value = value;
  }

  public void set(Object value)
  {
    this.type = ValueObjectTypes.OBJECT;
    this.value = value;
  }

  public void set(Date value, ValueObjectTypes type)
  {
    this.type = type;
    this.value = value;
  }

  /**
   * Setter: Convenience method. If the value is a string and needs to be
   * converted to another type before it is set then this method can be used. If
   * the string represents a Date then it is expected that it is one of the DB
   * formats as defined in DateUtils (DB_DATE, DB_TIME, DB_DATE_TIME). If it is
   * of any other format the date will be stored in its string representation
   * only.<br>
   * <br>
   * 
   * Assumptions & Constraints<br>
   * None<br>
   * <br>
   * 
   * @param attrType
   *          The type the attribute needs to be converted. Should be one of the
   *          constants defined at the BaseObject class.
   * @param value
   *          The value of the attribute.
   */
  public void set(ValueObjectTypes attrType, String value)
  {
    if (attrType == ValueObjectTypes.DOUBLE)
    {
      set(Double.parseDouble(value));
    }
    else if (attrType == ValueObjectTypes.STRING)
    {
      set(value);
    }
    else if (attrType == ValueObjectTypes.INTEGER)
    {
      set(Integer.parseInt(value));
    }
    else if (attrType == ValueObjectTypes.LONG)
    {
      set(Long.parseLong(value));
    }
    else if (attrType == ValueObjectTypes.BOOLEAN)
    {
      set(StringUtils.toBoolean(value));
    }
    else if (attrType == ValueObjectTypes.FLOAT)
    {
      set(Float.parseFloat(value));
    }
    else if (attrType == ValueObjectTypes.DATE)
    {
      try
      {
        set(DateUtils.stringToDate(value, DateUtils.DB_DATE), ValueObjectTypes.DATE);
      }
      catch (Exception ex)
      {
        // We simply store the string representation
        set(value);
      }
    }
    else if (attrType == ValueObjectTypes.TIME)
    {
      try
      {
        set(DateUtils.stringToDate(value, DateUtils.DB_TIME), ValueObjectTypes.TIME);
      }
      catch (Exception ex)
      {
        // We simply store the string representation
        set(value);
      }
    }
    else if (attrType == ValueObjectTypes.DATE_TIME)
    {
      try
      {
        set(DateUtils.stringToDate(value, DateUtils.DB_DATE_TIME), ValueObjectTypes.DATE_TIME);
      }
      catch (Exception ex)
      {
        // We simply store the string representation
        set(value);
      }
    }
    else
    // we store as String
    {
      set(value);
    }
  }

  /**
   * Getter: This returns the string representation of the value. <br>
   * <br>
   * Assumptions & Constraints<br>
   * None
   * 
   * @return String Value of the object as a string.
   */
  public String getValue()
  {
    if (value == null)
    {
      return (null);
    }
    else
    {
      return value.toString();
    }
  }

  /**
   * Getter: Returns the value as the type indicated. If the object is of a
   * different type then a NumberFormatException is raised. <br>
   * <br>
   * Assumptions & Constraints<br>
   * None
   */
  public int getInt() throws NumberFormatException
  {
    if (type == ValueObjectTypes.INTEGER)
    {
      return ((Integer) value).intValue();
    }

    throw new NumberFormatException("Object is of type " + type + "and not of type INTEGER");
  }

  public long getLong() throws NumberFormatException
  {
    if (type == ValueObjectTypes.LONG)
    {
      return ((Long) value).longValue();
    }

    throw new NumberFormatException("Object is of type " + type + "and not of type LONG");
  }

  public double getDouble() throws NumberFormatException
  {
    if (type == ValueObjectTypes.DOUBLE)
    {
      return ((Double) value).doubleValue();
    }

    throw new NumberFormatException("Object is of type " + type  + "and not of type DOUBLE");
  }

  public float getFloat() throws NumberFormatException
  {
    if (type == ValueObjectTypes.FLOAT)
    {
      return ((Float) value).floatValue();
    }

    throw new NumberFormatException("Object is of type " + type  + "and not of type FLOAT");
  }

  public boolean getBoolean() throws NumberFormatException
  {
    if (type == ValueObjectTypes.BOOLEAN)
    {
      return ((Boolean) value).booleanValue();
    }

    throw new NumberFormatException("Object is of type " + type  + "and not of type BOOLEAN");
  }

  public Date getDate() throws NumberFormatException
  {
    if ((type == ValueObjectTypes.DATE)
        || (type == ValueObjectTypes.DATE_TIME)
        || (type == ValueObjectTypes.TIME))
    {
      return ((Date) value);
    }

    throw new NumberFormatException("Object is of type " + type  + "and not of type DATE, TIME or DATE_TIME");
  }

  public Object getObject() throws NumberFormatException
  {
    if (type == ValueObjectTypes.OBJECT)
    {
      return value;
    }

    throw new NumberFormatException("Object is of type " + type + "and not of type OBJECT");
  }

  public String getString()
  {
    if (value != null)
    {
      return value.toString();
    }

    return null;
  }

  /**
   * Getter : Type <br>
   * <br>
   * Assumptions & Constraints<br>
   * None
   */
  public ValueObjectTypes getType()
  {
    return type;
  }

  /**
   * Returns a String representation of the object. <br>
   * <br>
   * Assumptions & Constraints<br>
   * None <br>
   * 
   * @return String representation of the object.
   */
  public String toString()
  {
    return ("Type=" + type.name() + "  Value=" + getValue());
  }

}
