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
package org.apache.xalan.lib.sql;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * A Connection Pool that wraps a JDBC datasource to provide connections.
 * 
 * An instance of this class is created by <code>XConnection</code> when it
 * attempts to resolves a <code>ConnectionPool</code> name as a JNDI data source.
 * 
 * Most methods in this implementation do nothing since configuration is handled
 * by the underlying JDBC datasource.  Users should always call
 * <code>XConnection.close()</code> from their stylsheet to explicitely close
 * their connection.  However, since there is no way to enforce this
 * (Yikes!), it is recommended that a relatively short datasource timeout
 * be used to prevent dangling connections.
 */
public class JNDIConnectionPool implements ConnectionPool
{
  
  /**
   * Reference to the datasource
   */
  protected Object jdbcSource = null;

  /** 
   * To maintain Java 1.3 compatibility, we need to work with the
   * DataSource class through Reflection. The getConnection method
   * is one of the methods used, and there are two different flavors.
   * 
   */  
  private Method getConnectionWithArgs = null;
  private Method getConnection = null;
  
  
  /**
   * The unique jndi path for this datasource.
   */
  protected String jndiPath = null;
  
  /**
   * User name for protected datasources.
   */
  protected String user = null;
  
  /**
   * Password for protected datasources.
   */
  protected String pwd = null;
  
  /**
   * Use of the default constructor requires the jndi path to be set via
   * setJndiPath().
   */
  public JNDIConnectionPool() {  }
  
  /**
   * Creates a connection pool with a specified JNDI path. 
   * @param jndiDatasourcePath Complete path to the JNDI datasource
   */
  public JNDIConnectionPool(String jndiDatasourcePath)
  {
    jndiPath = jndiDatasourcePath.trim();
  }
  
  /**
   * Sets the path for the jndi datasource 
   * @param jndiPath 
   */
  public void setJndiPath(String jndiPath)
  {
    this.jndiPath = jndiPath;
  }

  /**
   * Returns the path for the jndi datasource 
   * @param jndiPath 
   */
  public String getJndiPath()
  {
    return jndiPath;
  }

  /**
   * Always returns true.
   * This method was intended to indicate if the pool was enabled, however, in
   * this implementation that is not relavant.
   * @return 
   */
  public boolean isEnabled()
  {
    return true;
  }

  /**
   * Not implemented and will throw an Error if called.
   * 
   * Connection configuration is handled by the underlying JNDI DataSource.
   * @param d 
   */
  public void setDriver(String d)
  {
    throw new Error(
      "This method is not supported. " +
      "All connection information is handled by the JDBC datasource provider");
  }

  /**
   * Not implemented and will throw an Error if called.
   * 
   * Connection configuration is handled by the underlying JNDI DataSource.
   * @param d 
   */
  public void setURL(String url)
  {
    throw new Error(
      "This method is not supported. " +
      "All connection information is handled by the JDBC datasource provider");
  }

  /**
   * Intended to release unused connections from the pool.
   * Does nothing in this implementation.
   */
  public void freeUnused()
  {
    //Do nothing - not an error to call this method
  }

  /**
   * Always returns false, indicating that this wrapper has no idea of what
   * connections the underlying JNDI source is maintaining.
   * @return 
   */
  public boolean hasActiveConnections()
  {
    return false;
  }

  /**
   * Sets the password for the connection.
   * If the jndi datasource does not require a password (which is typical),
   * this can be left null.
   * @param p the password
   */
  public void setPassword(String p)
  {
    
    if (p != null) p = p.trim();
    if (p != null && p.length() == 0) p = null;
    
    pwd = p;
  }

  /**
   * Sets the user name for the connection.
   * If the jndi datasource does not require a user name (which is typical),
   * this can be left null.
   * @param u the user name
   */
  public void setUser(String u)
  {
    
    if (u != null) u = u.trim();
    if (u != null && u.length() == 0) u = null;
    
    user = u;
  }

  /**
   * Returns a connection from the JDNI DataSource found at the JNDI Datasource
   * path.
   * 
   * @return 
   * @throws SQLException 
   */
  public Connection getConnection() throws SQLException
  {
    if (jdbcSource == null)
    {
      try
      {
        findDatasource();
      }
      catch (NamingException ne)
      {
        throw new SQLException(
          "Could not create jndi context for " + 
          jndiPath + " - " + ne.getLocalizedMessage());
      }
    }
    
    try
    {
      if (user != null || pwd != null)
      {
        Object arglist[] = { user, pwd }; 
        return (Connection) getConnectionWithArgs.invoke(jdbcSource, arglist);
      }
      else
      {
        Object arglist[] = {}; 
        return (Connection) getConnection.invoke(jdbcSource, arglist);
      }
    }
    catch (Exception e)
    {
      throw new SQLException(
        "Could not create jndi connection for " + 
        jndiPath + " - " + e.getLocalizedMessage());
    }
    
  }
  
  /**
   * Internal method used to look up the datasource. 
   * @throws NamingException 
   */
  protected void findDatasource() throws NamingException
  {
    try
    {
      InitialContext context = new InitialContext();
      jdbcSource =  context.lookup(jndiPath);
      
      Class withArgs[] = { String.class, String.class };
      getConnectionWithArgs = 
        jdbcSource.getClass().getDeclaredMethod("getConnection", withArgs);
      
      Class noArgs[] = { };
      getConnection = 
        jdbcSource.getClass().getDeclaredMethod("getConnection", noArgs);
      
    }
    catch (NamingException e)
    {
      throw e;
    }
    catch (NoSuchMethodException e)
    {
      // For simpleification, we will just throw a NamingException. We will only
      // use the message part of the exception anyway.
      throw new NamingException("Unable to resolve JNDI DataSource - " + e);
    }
  }

  public void releaseConnection(Connection con) throws SQLException
  {
    con.close();
  }

  public void releaseConnectionOnError(Connection con) throws SQLException
  {
    con.close();
  }

  /**
   * Releases the reference to the jndi datasource.
   * The original intention of this method was to actually turn the pool *off*.
   * Since we are not managing the pool, we simply release our reference to
   * the datasource.  Future calls to the getConnection will simply recreate
   * the datasource.
   * @param flag If false, the reference to the datasource is released.
   */
  public void setPoolEnabled(boolean flag)
  {
    if (! flag) jdbcSource = null;
  }

  /**
   * Ignored in this implementation b/c the pooling is determined by the jndi dataosource. 
   * @param p
   */
  public void setProtocol(Properties p)
  {
    /* ignore - properties are determined by datasource */
  }
  
  /**
   * Ignored in this implementation b/c the pooling is determined by the jndi dataosource. 
   * @param n 
   */
  public void setMinConnections(int n)
  {
    /* ignore - pooling is determined by datasource */
  }

  /**
   * A simple test to see if the jndi datasource exists.
   * 
   * Note that this test does not ensure that the datasource will return valid
   * connections.
   */
  public boolean testConnection()
  {
    if (jdbcSource == null)
    {
      try
      {
        findDatasource();
      }
      catch (NamingException ne)
      {
        return false;
      }
    }
    
    return true;
  }



}