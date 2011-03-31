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
 * $Id: ConnectionPool.java,v 1.13 2004/02/11 17:56:36 minchau Exp $
 */

package org.apache.xalan.lib.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * An interface used to build wrapper classes around existing
 * Connection Pool libraries.
 * Title:     ConnectionPool<p>
 * @author John Gentilin
 * @version 1.0
 */
public interface ConnectionPool
{

  /**
   * Determine if a Connection Pool has been disabled. If a Connection pool
   * is disabled, then it will only manage connections that are in use.
   *
   */
  public boolean isEnabled( );

  /**
   * The Driver and URL are the only required parmeters.
   * @param d
   *
   */
  public void setDriver( String d );

  /**
   * @param url
   *
   */
  public void setURL( String url );

  /**
   * Start downsizeing the pool, this usally happens right after the
   * pool has been marked as Inactive and we are removing connections
   * that are not currently inuse.
   *
   */
  public void freeUnused( );


  /**
   * Provide an indicator to the PoolManager when the Pool can be removed
   * from the Pool Table.
   *
   */
  public boolean hasActiveConnections( );

  /**
   * The rest of the protocol parameters can eiter be passed in as
   * just Username and Password or as a property collection. If the
   * property collection is used, then the sperate username and password
   * may be ignored, it is up to the wrapper implementation to handle
   * the situation. If the connection information changes while after the
   * pool has been established, the wrapper implementation should ignore
   * the change and throw an error.
   * @param p
   *
   */
  public void setPassword( String p );

  /**
   * @param u
   *
   */
  public void setUser( String u );


  /**
   * Set tne minimum number of connections that are to be maintained in the
   * pool.
   * @param n
   *
   */
  public void setMinConnections( int n );

  /**
   * Test to see if the connection info is valid to make a real connection
   * to the database. This method may cause the pool to be crated and filled
   * with min connections.
   *
   */
  public boolean testConnection( );

  /**
   * Retrive a database connection from the pool
   *
   * @throws SQLException
   */
  public Connection getConnection( )throws SQLException;

   /**
   * Return a connection to the pool, the connection may be closed if the
   * pool is inactive or has exceeded the max number of free connections
   * @param con
   *
   * @throws SQLException
   */
  public void releaseConnection( Connection con )throws SQLException;

   /**
   * Provide a mechinism to return a connection to the pool on Error.
   * A good default behaviour is to close this connection and build
   * a new one to replace it. Some JDBC impl's won't allow you to
   * reuse a connection after an error occurs.
   * @param con
   *
   * @throws SQLException
   */
  public void releaseConnectionOnError( Connection con )throws SQLException;


  /**
   * The Pool can be Enabled and Disabled. Disabling the pool
   * closes all the outstanding Unused connections and any new
   * connections will be closed upon release.
   * @param flag Control the Connection Pool. If it is enabled
   * then Connections will actuall be held around. If disabled
   * then all unused connections will be instantly closed and as
   * connections are released they are closed and removed from the pool.
   *
   */
  public void setPoolEnabled( final boolean flag );

  /**
   * Used to pass in extra configuration options during the
   * database connect phase.
   */
  public void setProtocol(Properties p);


}
