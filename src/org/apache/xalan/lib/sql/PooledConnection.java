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
 * $Id: PooledConnection.java,v 1.11 2004/02/11 17:56:36 minchau Exp $
 */
package org.apache.xalan.lib.sql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 */
public class PooledConnection
{

  // Real JDBC Connection
  /**
   */
  private Connection connection = null;
  // boolean flag used to determine if connection is in use
  /**
   */
  private boolean inuse = false;

  // Constructor that takes the passed in JDBC Connection
  // and stores it in the connection attribute.
  /**
   * @param value
   */
  public PooledConnection( Connection value )
  {
    if ( value != null ) { connection = value; }
  }

  /**
   * Returns a reference to the JDBC Connection
   * @return Connection
   */
  public Connection getConnection( )
  {
    // get the JDBC Connection
    return connection;
  }

  /**
   * Set the status of the PooledConnection.
   *
   * @param value
   *
   */
  public void setInUse( boolean value )
  {
    inuse = value;
  }

  /**
   * Returns the current status of the PooledConnection.
   *
   */
  public boolean inUse( ) { return inuse; }

  /**
   *  Close the real JDBC Connection
   *
   */
  public void close( )
  {
    try
    {
      connection.close();
    }
    catch (SQLException sqle)
    {
      System.err.println(sqle.getMessage());
    }
  }
}
