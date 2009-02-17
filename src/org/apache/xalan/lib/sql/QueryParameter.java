/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: QueryParameter.java,v 1.11 2004/02/11 17:56:36 minchau Exp $
 */

/* This class holds a parameter definition for a JDBC PreparedStatement or CallableStatement. */

package org.apache.xalan.lib.sql;

import java.util.Hashtable;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.Statement;

public class QueryParameter
{
  private int     m_type;
  private String  m_name;
  private String  m_value;
  private boolean m_output;
  private String  m_typeName;
  private static  Hashtable m_Typetable = null;

  public QueryParameter()
  {
    m_type = -1;
    m_name = null;
    m_value = null;
    m_output = false;
    m_typeName = null;
  }

  /**
   * @param v The parameter value.
   * @param t The type of the parameter.
   */
  public QueryParameter( String v, String t )
  {
    m_name = null;
    m_value = v;
    m_output = false;
    setTypeName(t);
  }

  public QueryParameter( String name, String value, String type, boolean out_flag )
  {
    m_name = name;
    m_value = value;
    m_output = out_flag;
    setTypeName(type);
  }

  /**
   *
   */
  public String getValue( ) {
    return m_value;
  }

  /**
   * @param newValue
   *
   */
  public void setValue( String newValue ) {
    m_value = newValue;
  }

  /** Used to set the parameter type when the type information is provided in the query.
   * @param newType The parameter type.
   *
   */
  public void setTypeName( String newType )
  {
    m_type = map_type(newType);
    m_typeName = newType;
  }

  /**
   *
   */
  public String getTypeName( )
  {
    return m_typeName;
  }

  /**
   *
   */
  public int getType( )
  {
    return m_type;
  }

  /**
   *
   */
  public String getName()
  {
    return m_name;
  }

  /**
   * Set Name, this should really be covered in the constructor but the
   * QueryParser has a State issue where the name is discoverd after the
   * Parameter object needs to be created
   */
  public void setName(String n)
  {
    m_name = n;
  }

  /**
  *
  */
  public boolean isOutput()
  {
    return m_output;
  }

  /**
   * Set Name, this should really be covered in the constructor but the
   * QueryParser has a State issue where the name is discoverd after the
   * Parameter object needs to be created
   */
  public void setIsOutput(boolean flag)
  {
    m_output = flag;
  }

  private static int map_type(String typename)
  {
    if ( m_Typetable == null )
    {
      // Load up the type mapping table.
      m_Typetable = new Hashtable();
      m_Typetable.put("BIGINT", new Integer(java.sql.Types.BIGINT));
      m_Typetable.put("BINARY", new Integer(java.sql.Types.BINARY));
      m_Typetable.put("BIT", new Integer(java.sql.Types.BIT));
      m_Typetable.put("CHAR", new Integer(java.sql.Types.CHAR));
      m_Typetable.put("DATE", new Integer(java.sql.Types.DATE));
      m_Typetable.put("DECIMAL", new Integer(java.sql.Types.DECIMAL));
      m_Typetable.put("DOUBLE", new Integer(java.sql.Types.DOUBLE));
      m_Typetable.put("FLOAT", new Integer(java.sql.Types.FLOAT));
      m_Typetable.put("INTEGER", new Integer(java.sql.Types.INTEGER));
      m_Typetable.put("LONGVARBINARY", new Integer(java.sql.Types.LONGVARBINARY));
      m_Typetable.put("LONGVARCHAR", new Integer(java.sql.Types.LONGVARCHAR));
      m_Typetable.put("NULL", new Integer(java.sql.Types.NULL));
      m_Typetable.put("NUMERIC", new Integer(java.sql.Types.NUMERIC));
      m_Typetable.put("OTHER", new Integer(java.sql.Types.OTHER));
      m_Typetable.put("REAL", new Integer(java.sql.Types.REAL));
      m_Typetable.put("SMALLINT", new Integer(java.sql.Types.SMALLINT));
      m_Typetable.put("TIME", new Integer(java.sql.Types.TIME));
      m_Typetable.put("TIMESTAMP", new Integer(java.sql.Types.TIMESTAMP));
      m_Typetable.put("TINYINT", new Integer(java.sql.Types.TINYINT));
      m_Typetable.put("VARBINARY", new Integer(java.sql.Types.VARBINARY));
      m_Typetable.put("VARCHAR", new Integer(java.sql.Types.VARCHAR));

      // Aliases from Xalan SQL extension.
      m_Typetable.put("STRING", new Integer(java.sql.Types.VARCHAR));
      m_Typetable.put("BIGDECIMAL", new Integer(java.sql.Types.NUMERIC));
      m_Typetable.put("BOOLEAN", new Integer(java.sql.Types.BIT));
      m_Typetable.put("BYTES", new Integer(java.sql.Types.LONGVARBINARY));
      m_Typetable.put("LONG", new Integer(java.sql.Types.BIGINT));
      m_Typetable.put("SHORT", new Integer(java.sql.Types.SMALLINT));
    }

    Integer type = (Integer) m_Typetable.get(typename.toUpperCase());
    int rtype;
    if ( type == null )
      rtype = java.sql.Types.OTHER;
    else
      rtype = type.intValue();

    return(rtype);
  }

  /**
   * This code was in the XConnection, it is included for reference but it
   * should not be used.
   *
   * @TODO Remove this code as soon as it is determined that its Use Case is
   * resolved elsewhere.
   */
  /**
   * Set the parameter for a Prepared Statement
   * @param pos
   * @param stmt
   * @param p
   *
   * @throws SQLException
   */
  /*
  private void setParameter( int pos, PreparedStatement stmt, QueryParameter p )throws SQLException
  {
    String type = p.getType();
    if (type.equalsIgnoreCase("string"))
    {
      stmt.setString(pos, p.getValue());
    }

    if (type.equalsIgnoreCase("bigdecimal"))
    {
      stmt.setBigDecimal(pos, new BigDecimal(p.getValue()));
    }

    if (type.equalsIgnoreCase("boolean"))
    {
      Integer i = new Integer( p.getValue() );
      boolean b = ((i.intValue() != 0) ? false : true);
      stmt.setBoolean(pos, b);
    }

    if (type.equalsIgnoreCase("bytes"))
    {
      stmt.setBytes(pos, p.getValue().getBytes());
    }

    if (type.equalsIgnoreCase("date"))
    {
      stmt.setDate(pos, Date.valueOf(p.getValue()));
    }

    if (type.equalsIgnoreCase("double"))
    {
      Double d = new Double(p.getValue());
      stmt.setDouble(pos, d.doubleValue() );
    }

    if (type.equalsIgnoreCase("float"))
    {
      Float f = new Float(p.getValue());
      stmt.setFloat(pos, f.floatValue());
    }

    if (type.equalsIgnoreCase("long"))
    {
      Long l = new Long(p.getValue());
      stmt.setLong(pos, l.longValue());
    }

    if (type.equalsIgnoreCase("short"))
    {
      Short s = new Short(p.getValue());
      stmt.setShort(pos, s.shortValue());
    }

    if (type.equalsIgnoreCase("time"))
    {
      stmt.setTime(pos, Time.valueOf(p.getValue()) );
    }

    if (type.equalsIgnoreCase("timestamp"))
    {

      stmt.setTimestamp(pos, Timestamp.valueOf(p.getValue()) );
    }

  }
  */

}


