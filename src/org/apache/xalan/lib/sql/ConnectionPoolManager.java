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
 * $Id: ConnectionPoolManager.java,v 1.8 2004/12/15 17:35:30 jycli Exp $
 */


 package org.apache.xalan.lib.sql;

import java.util.Hashtable;

import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;

/**
 */
public class ConnectionPoolManager
{
  /**
   */
  private static Hashtable m_poolTable = null;

  /**
   */
  public ConnectionPoolManager( )
  {
    init();
  }

  /**
   * Initialize the internal structures of the Pool Manager
   *
   */
  private synchronized void init( )
  {
    /**
     * Only do this process once
     * Initialize the pool table
     */   
    if (m_poolTable == null)
            m_poolTable = new Hashtable();
  }

  /**
   * Register a nuew connection pool to the global pool table.
   * If a pool by that name currently exists, then throw an
   * IllegalArgumentException stating that the pool already
   * exist.
   * @param name
   * @param pool
   *
   * @link org.apache.xalan.lib.sql.ConnectionPool}
   *
   * @throws <code>IllegalArgumentException</code>, throw this exception
   * if a pool with the same name currently exists.
   */
  public synchronized void registerPool( String name, ConnectionPool pool )
  {
    if ( m_poolTable.containsKey(name) )
    {
      throw new IllegalArgumentException(XSLMessages.createMessage(XSLTErrorResources.ER_POOL_EXISTS, null)); //"Pool already exists");
    }

    m_poolTable.put(name, pool);
  }

  /**
   * Remove a pool from the global table. If the pool still has
   * active connections, then only mark this pool as inactive and
   * leave it around until all the existing connections are closed.
   * @param name
   *
   */
  public synchronized void removePool( String name )
  {
    ConnectionPool pool = getPool(name);

    if (null != pool)
    {
      //
      // Disable future use of this pool under the Xalan
      // extension only. This flag should only exist in the
      // wrapper and not in the actual pool implementation.
      pool.setPoolEnabled(false);


      //
      // Remove the pool from the Hashtable if we don'd have
      // any active connections.
      //
      if ( ! pool.hasActiveConnections() ) m_poolTable.remove(name);
    }

  }


  /**
   * Return the connection pool referenced by the name
   * @param name
   *
   * @return <code>ConnectionPool</code> a reference to the ConnectionPool
   * object stored in the Pool Table. If the named pool does not exist, return
   * null
   */
  public synchronized ConnectionPool getPool( String name )
  {
    return (ConnectionPool) m_poolTable.get(name);
  }

}
