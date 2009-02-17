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
 * $Id: XConnection.java,v 1.34 2005/07/28 00:26:16 johng Exp $
 */
package org.apache.xalan.lib.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.ref.DTMManagerDefault;
import org.apache.xml.dtm.ref.DTMNodeIterator;
import org.apache.xml.dtm.ref.DTMNodeProxy;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBooleanStatic;
import org.apache.xpath.objects.XNodeSet;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An XSLT extension that allows a stylesheet to
 * access JDBC data.
 *
 * It is accessed by specifying a namespace URI as follows:
 * <pre>
 *    xmlns:sql="http://xml.apache.org/xalan/sql"
 * </pre>
 *
 * From the stylesheet perspective,
 * XConnection provides 3 extension functions: new(),
 * query(), and close().
 * Use new() to call one of XConnection constructors, which
 * establishes a JDBC driver connection to a data source and
 * returns an XConnection object.
 * Then use the XConnection object query() method to return a
 * result set in the form of a row-set element.
 * When you have finished working with the row-set, call the
 * XConnection object close() method to terminate the connection.
 */
public class XConnection
{

  /**
   * Flag for DEBUG mode
   */
  private static final boolean DEBUG = false;

  /**
   * The Current Connection Pool in Use. An XConnection can only
   * represent one query at a time, prior to doing some type of query.
   */
  private ConnectionPool m_ConnectionPool = null;

  /**
   * The DBMS Connection used to produce this SQL Document.
   * Will be used to clear free up the database resources on
   * close.
   */
  private Connection m_Connection = null;

  /**
   * If a default Connection Pool is used. i.e. A connection Pool
   * that is created internally, then do we actually allow pools
   * to be created. Due to the archititure of the Xalan Extensions,
   * there is no notification of when the Extension is being unloaded and
   * as such, there is a good chance that JDBC COnnections are not closed.
   * A finalized is provided to try and catch this situation but since
   * support of finalizers is inconsistant across JVM's this may cause
   * a problem. The robustness of the JDBC Driver is also at issue here.
   * if a controlled shutdown is provided by the driver then default
   * conntectiom pools are OK.
   */
  private boolean m_DefaultPoolingEnabled = false;


  /**
   * As we do queries, we will produce SQL Documents. Any ony may produce
   * one or more SQL Documents so that the current connection information
   * may be easilly reused. This collection will hold a collection of all
   * the documents created. As Documents are closed, they will be removed
   * from the collection and told to free all the used resources.
   */
  private Vector m_OpenSQLDocuments = new Vector();


  /**
   * Let's keep a copy of the ConnectionPoolMgr in
   * alive here so we are keeping the static pool alive
   * We will also use this Pool Manager to register our default pools.
   */
  private ConnectionPoolManager m_PoolMgr = new ConnectionPoolManager();

  /**
   * For PreparedStatements, we need a place to
   * to store the parameters in a vector.
   */
  private Vector m_ParameterList = new Vector();

  /**
   * Allow the SQL Extensions to return null on error. The Error information will
   * be stored in a seperate Error Document that can easily be retrived using the
   * getError() method.
   * %REVIEW% This functionality will probably be buried inside the SQLDocument.
   */
  private Exception m_Error = null;

  /**
   * When the Stylesheet wants to review the errors from a paticular
   * document, it asks the XConnection. We need to track what document
   * in the list of managed documents caused the last error. As SetError
   * is called, it will record the document that had the error.
   */
  private SQLDocument     m_LastSQLDocumentWithError = null;

  /**
   * If true then full information should be returned about errors and warnings
   * in getError(). This includes chained errors and warnings.
   * If false (the default) then getError() returns just the first SQLException.
   */
  private boolean m_FullErrors = false;



  /**
   * One a per XConnection basis there is a master QueryParser that is responsible
   * for generating Query Parsers. This will allow us to cache previous instances
   * so the inline parser execution time is minimized.
   */
  private SQLQueryParser m_QueryParser = new SQLQueryParser();

  /**
   */
  private boolean m_IsDefaultPool = false;

  /**
   * This flag will be used to indicate to the SQLDocument to use
   * Streaming mode. Streeaming Mode will reduce the memory footprint
   * to a fixed amount but will not let you traverse the tree more than
   * once since the Row data will be reused for every Row in the Query.
   */
  private boolean m_IsStreamingEnabled = true;

  /**
   *
   */
   private boolean m_InlineVariables = false;

  /**
   * This flag will be used to indicate if multiple result sets are
   * supported from the database. If they are, then the metadata element is
   * moved to insude the row-set element and multiple row-set elements may
   * be included under the sql root element.
   */
  private boolean m_IsMultipleResultsEnabled = false;

  /**
   * This flag will be used to indicate if database preparedstatements
   * should be cached. This also controls if the Java statement object
   * should be cached.
   */
  private boolean m_IsStatementCachingEnabled = false;

  /**
   */
  public XConnection( )
  {
  }

  /**
   * Constructs a new XConnection and attempts to connect to a datasource as
   * defined in the
   * <code>connect(ExpressionContext exprContext, String connPoolName)</code>
   * method.
   * <code>org.apache.xalan.lib.sql.ConnectionPool</code> or a JNDI datasource.
   *
   * @param exprContext Context automatically passed from the XSLT sheet.
   * @param name The name of the ConnectionPool or the JNDI DataSource path.
   *
   */
  public XConnection( ExpressionContext exprContext, String connPoolName )
  {
    connect(exprContext, connPoolName);
  }

  /**
   * @param exprContext
   * @param driver
   * @param dbURL
   */
  public XConnection( ExpressionContext exprContext, String driver, String dbURL )
  {
    connect(exprContext, driver, dbURL);
  }

  /**
   * @param exprContext
   * @param list
   */
  public XConnection( ExpressionContext exprContext, NodeList list )
  {
    connect(exprContext, list);
  }

  /**
   * @param exprContext
   * @param driver
   * @param dbURL
   * @param user
   * @param password
   */
  public XConnection( ExpressionContext exprContext, String driver, String dbURL, String user, String password )
  {
    connect(exprContext, driver, dbURL, user, password);
  }

  /**
   * @param exprContext
   * @param driver
   * @param dbURL
   * @param protocolElem
   */
  public XConnection( ExpressionContext exprContext, String driver, String dbURL, Element protocolElem )
  {
    connect(exprContext, driver, dbURL, protocolElem);
  }

  /**
    * Returns an XConnection from either a user created
    * <code>org.apache.xalan.lib.sql.ConnectionPool</code> or a JNDI datasource.

    * 
    * This method first tries to resolve the passed name against
    * <code>ConnectionPool</code>s registered with
    * <code>ConnectionPoolManager</code>.
    * If that fails, it attempts to find the name as a JNDI DataSource path.
    *
    * @param exprContext Context automatically passed from the XSLT sheet.
    * @param name The name of the ConnectionPool or the JNDI DataSource path.
    *
    */
   public XBooleanStatic connect( ExpressionContext exprContext, String name )
   {
     try
     {
       m_ConnectionPool = m_PoolMgr.getPool(name);

       if (m_ConnectionPool == null)
       {
         //Try to create a jndi source with the passed name
         ConnectionPool pool = new JNDIConnectionPool(name);
        
         if (pool.testConnection())
         {
          
           //JNDIConnectionPool seems good, so register it with the pool manager.
           //Once registered, it will show up like other named ConnectionPool's,
           //so the m_PoolMgr.getPool(name) method (above) will find it.
           m_PoolMgr.registerPool(name, pool);
           m_ConnectionPool = pool;
          
           m_IsDefaultPool = false;
           return new XBooleanStatic(true);
         }
         else
         {
           throw new IllegalArgumentException(
               "Invalid ConnectionPool name or JNDI Datasource path: " + name);
         }
       }
       else
       {
         m_IsDefaultPool = false;
         return new XBooleanStatic(true);
       }
     }
     catch (Exception e)
     {
       setError(e, exprContext);
       return new XBooleanStatic(false);
     }

   }


  /**
   * Create an XConnection object with just a driver and database URL.
   * @param exprContext
   * @param driver JDBC driver of the form foo.bar.Driver.
   * @param dbURL database URL of the form jdbc:subprotocol:subname.
   *
   */
  public XBooleanStatic connect( ExpressionContext exprContext, String driver, String dbURL )
  {
    try
    {
      init(driver, dbURL, new Properties());
      return new XBooleanStatic(true);
    }
    catch(SQLException e)
    {
      setError(e,exprContext);
      return new XBooleanStatic(false);
    }
    catch (Exception e)
    {
      setError(e,exprContext);
      return new XBooleanStatic(false);
    }
  }

  /**
   * @param exprContext
   * @param protocolElem
   *
   */
  public XBooleanStatic connect( ExpressionContext exprContext, Element protocolElem )
  {
    try
    {
      initFromElement(protocolElem);
      return new XBooleanStatic(true);
    }
    catch(SQLException e)
    {
      setError(e,exprContext);
      return new XBooleanStatic(false);
    }
    catch (Exception e)
    {
      setError(e,exprContext);
      return new XBooleanStatic(false);
    }
  }

  /**
   * @param exprContext
   * @param list
   *
   */
  public XBooleanStatic connect( ExpressionContext exprContext, NodeList list )
  {
    try
    {
      initFromElement( (Element) list.item(0) );
      return new XBooleanStatic(true);
    }
    catch(SQLException e)
    {
      setError(e, exprContext);
      return new XBooleanStatic(false);
    }
    catch (Exception e)
    {
      setError(e,exprContext);
      return new XBooleanStatic(false);
    }
  }

  /**
   * Create an XConnection object with user ID and password.
   * @param exprContext
   * @param driver JDBC driver of the form foo.bar.Driver.
   * @param dbURL database URL of the form jdbc:subprotocol:subname.
   * @param user user ID.
   * @param password connection password.
   *
   */
  public XBooleanStatic connect( ExpressionContext exprContext, String driver, String dbURL, String user, String password )
  {
    try
    {
      Properties prop = new Properties();
      prop.put("user", user);
      prop.put("password", password);

      init(driver, dbURL, prop);

      return new XBooleanStatic(true);
    }
    catch(SQLException e)
    {
      setError(e,exprContext);
      return new XBooleanStatic(false);
    }
    catch (Exception e)
    {
      setError(e,exprContext);
      return new XBooleanStatic(false);
    }
  }


  /**
   * Create an XConnection object with a connection protocol
   * @param exprContext
   * @param driver JDBC driver of the form foo.bar.Driver.
   * @param dbURL database URL of the form jdbc:subprotocol:subname.
   * @param protocolElem list of string tag/value connection arguments,
   * normally including at least "user" and "password".
   *
   */
  public XBooleanStatic connect( ExpressionContext exprContext, String driver, String dbURL, Element protocolElem )
  {
    try
    {
      Properties prop = new Properties();

      NamedNodeMap atts = protocolElem.getAttributes();

      for (int i = 0; i < atts.getLength(); i++)
      {
        prop.put(atts.item(i).getNodeName(), atts.item(i).getNodeValue());
      }

      init(driver, dbURL, prop);

      return new XBooleanStatic(true);
    }
    catch(SQLException e)
    {
      setError(e,exprContext);
      return new XBooleanStatic(false);
    }
    catch (Exception e)
    {
      setError(e, exprContext);
      return new XBooleanStatic(false);
    }
  }


  /**
   * Allow the database connection information to be sepcified in
   * the XML tree. The connection information could also be
   * externally originated and passed in as an XSL Parameter.
   * The required XML Format is as follows.
   * A document fragment is needed to specify the connection information
   * the top tag name is not specific for this code, we are only interested
   * in the tags inside.
   * <DBINFO-TAG>
   * Specify the driver name for this connection pool
   * <dbdriver>drivername</dbdriver>
   * Specify the URL for the driver in this connection pool
   * <dburl>url</dburl>
   * Specify the password for this connection pool
   * <password>password</password>
   * Specify the username for this connection pool
   * <user>username</user>
   * You can add extra protocol items including the User Name & Password
   * with the protocol tag. For each extra protocol item, add a new element
   * where the name of the item is specified as the name attribute and
   * and its value as the elements value.
   * <protocol name="name of value">value</protocol>
   * </DBINFO-TAG>
   * @param e
   *
   * @throws SQLException
   */
  private void initFromElement( Element e )throws SQLException
  {

    Properties prop = new Properties();
    String driver = "";
    String dbURL = "";
    Node n = e.getFirstChild();

    if (null == n) return; // really need to throw an error

    do
    {
      String nName = n.getNodeName();

      if (nName.equalsIgnoreCase("dbdriver"))
      {
        driver = "";
        Node n1 = n.getFirstChild();
        if (null != n1)
        {
          driver = n1.getNodeValue();
        }
      }

      if (nName.equalsIgnoreCase("dburl"))
      {
        dbURL = "";
        Node n1 = n.getFirstChild();
        if (null != n1)
        {
          dbURL = n1.getNodeValue();
        }
      }

      if (nName.equalsIgnoreCase("password"))
      {
        String s = "";
        Node n1 = n.getFirstChild();
        if (null != n1)
        {
          s = n1.getNodeValue();
        }
        prop.put("password", s);
      }

      if (nName.equalsIgnoreCase("user"))
      {
        String s = "";
        Node n1 = n.getFirstChild();
        if (null != n1)
        {
          s = n1.getNodeValue();
        }
        prop.put("user", s);
      }

      if (nName.equalsIgnoreCase("protocol"))
      {
        String Name = "";

        NamedNodeMap attrs = n.getAttributes();
        Node n1 = attrs.getNamedItem("name");
        if (null != n1)
        {
          String s = "";
          Name = n1.getNodeValue();

          Node n2 = n.getFirstChild();
          if (null != n2) s = n2.getNodeValue();

          prop.put(Name, s);
        }
      }

    } while ( (n = n.getNextSibling()) != null);

    init(driver, dbURL, prop);
  }



  /**
   * Initilize is being called because we did not have an
   * existing Connection Pool, so let's see if we created one
   * already or lets create one ourselves.
   * @param driver
   * @param dbURL
   * @param prop
   *
   * @throws SQLException
   */
  private void init( String driver, String dbURL, Properties prop )throws SQLException
  {
    Connection con = null;

    if (DEBUG)
      System.out.println("XConnection, Connection Init");

    String user = prop.getProperty("user");
    if (user == null) user = "";

    String passwd = prop.getProperty("password");
    if (passwd == null) passwd = "";


    String poolName = driver + dbURL + user + passwd;
    ConnectionPool cpool = m_PoolMgr.getPool(poolName);

    if (cpool == null)
    {

      if (DEBUG)
      {
        System.out.println("XConnection, Creating Connection");
        System.out.println(" Driver  :" + driver);
        System.out.println(" URL     :" + dbURL);
        System.out.println(" user    :" + user);
        System.out.println(" passwd  :" + passwd);
      }


      DefaultConnectionPool defpool = new DefaultConnectionPool();

      if ((DEBUG) && (defpool == null))
        System.out.println("Failed to Create a Default Connection Pool");

      defpool.setDriver(driver);
      defpool.setURL(dbURL);
      defpool.setProtocol(prop);

      // Only enable pooling in the default pool if we are explicatly
      // told too.
      if (m_DefaultPoolingEnabled) defpool.setPoolEnabled(true);

      m_PoolMgr.registerPool(poolName, defpool);
      m_ConnectionPool = defpool;
    }
    else
    {
      m_ConnectionPool = cpool;
    }

    m_IsDefaultPool = true;

    //
    // Let's test to see if we really can connect
    // Just remember to give it back after the test.
    //
    try
    {
      con = m_ConnectionPool.getConnection();
    }
    catch(SQLException e)
    {
      if (con != null)
      {
        m_ConnectionPool.releaseConnectionOnError(con);
        con = null;
      }
      throw e;
    }
    finally
    {
      if ( con != null ) m_ConnectionPool.releaseConnection(con);
    }
  }

  /**
   * Allow the SQL Document to retrive a connection to be used
   * to build the SQL Statement.
   */
  public ConnectionPool getConnectionPool()
  {
    return m_ConnectionPool;
  }


  /**
   * Execute a query statement by instantiating an
   * @param exprContext
   * @param queryString the SQL query.
   * @return XStatement implements NodeIterator.
   * @throws SQLException
   * @link org.apache.xalan.lib.sql.XStatement XStatement}
   * object. The XStatement executes the query, and uses the result set
   * to create a
   * @link org.apache.xalan.lib.sql.RowSet RowSet},
   * a row-set element.
   */
  public DTM query( ExpressionContext exprContext, String queryString )
  {
    SQLDocument doc = null;

    try
    {
      if (DEBUG) System.out.println("pquery()");

      // Build an Error Document, NOT Connected.
      if ( null == m_ConnectionPool ) return null;

      SQLQueryParser query =
          m_QueryParser.parse
            (this, queryString, SQLQueryParser.NO_INLINE_PARSER);

      doc = SQLDocument.getNewDocument(exprContext);
      doc.execute(this, query);

      // also keep a local reference
      m_OpenSQLDocuments.addElement(doc);
    }
    catch (Exception e)
    {
      // OK We had an error building the document, let try and grab the
      // error information and clean up our connections.

      if (DEBUG) System.out.println("exception in query()");

      if (doc != null)
      {
        if (doc.hasErrors())
        {
          setError(e, doc, doc.checkWarnings());
        }

        doc.close(m_IsDefaultPool);
        doc = null;
      }
    }
    finally
    {
      if (DEBUG) System.out.println("leaving query()");
    }

    // Doc will be null if there was an error
    return doc;
  }

  /**
   * Execute a parameterized query statement by instantiating an
   * @param exprContext
   * @param queryString the SQL query.
   * @return XStatement implements NodeIterator.
   * @throws SQLException
   * @link org.apache.xalan.lib.sql.XStatement XStatement}
   * object. The XStatement executes the query, and uses the result set
   * to create a
   * @link org.apache.xalan.lib.sql.RowSet RowSet},
   * a row-set element.
   */
  public DTM pquery( ExpressionContext exprContext, String queryString )
  {
    return(pquery(exprContext, queryString, null));
  }

  /**
   * Execute a parameterized query statement by instantiating an
   * @param exprContext
   * @param queryString the SQL query.
   * @param typeInfo
   * @return XStatement implements NodeIterator.
   * @throws SQLException
   * @link org.apache.xalan.lib.sql.XStatement XStatement}
   * object. The XStatement executes the query, and uses the result set
   * to create a
   * @link org.apache.xalan.lib.sql.RowSet RowSet},
   * a row-set element.
   * This method allows for the user to pass in a comma seperated
   * String that represents a list of parameter types. If supplied
   * the parameter types will be used to overload the current types
   * in the current parameter list.
   */
  public DTM pquery( ExpressionContext exprContext, String queryString, String typeInfo)
  {
    SQLDocument doc = null;

    try
    {
      if (DEBUG) System.out.println("pquery()");

      // Build an Error Document, NOT Connected.
      if ( null == m_ConnectionPool ) return null;

      SQLQueryParser query =
          m_QueryParser.parse
            (this, queryString, SQLQueryParser.NO_OVERRIDE);

      // If we are not using the inline parser, then let add the data
      // to the parser so it can populate the SQL Statement.
      if ( !m_InlineVariables )
      {
        addTypeToData(typeInfo);
        query.setParameters(m_ParameterList);
      }

      doc = SQLDocument.getNewDocument(exprContext);
      doc.execute(this, query);

      // also keep a local reference
      m_OpenSQLDocuments.addElement(doc);
    }
    catch (Exception e)
    {
      // OK We had an error building the document, let try and grab the
      // error information and clean up our connections.

      if (DEBUG) System.out.println("exception in query()");

      if (doc != null)
      {
        if (doc.hasErrors())
        {
          setError(e, doc, doc.checkWarnings());
        }

        // If we are using the Default Connection Pool, then
        // force the connection pool to flush unused connections.
        doc.close(m_IsDefaultPool);
        doc = null;
      }
    }
    finally
    {
      if (DEBUG) System.out.println("leaving query()");
    }

    // Doc will be null if there was an error
    return doc;
  }

  /**
   * The purpose of this routine is to force the DB cursor to skip forward
   * N records. You should call this function after [p]query to help with
   * pagination. i.e. Perfrom your select, then skip forward past the records
   * you read previously.
   * 
   * @param exprContext
   * @param o
   * @param value
   */
  public void skipRec( ExpressionContext exprContext, Object o, int value )
  {
    SQLDocument sqldoc = null;
    DTMNodeIterator nodei = null;
      
    sqldoc = locateSQLDocument( exprContext, o);
    if (sqldoc != null) sqldoc.skip(value);
  }

  

  private void addTypeToData(String typeInfo)
  {
      int indx;

      if ( typeInfo != null && m_ParameterList != null )
      {
          // Parse up the parameter types that were defined
          // with the query
          StringTokenizer plist = new StringTokenizer(typeInfo);

          // Override the existing type that is stored in the
          // parameter list. If there are more types than parameters
          // ignore for now, a more meaningfull error should occur
          // when the actual query is executed.
          indx = 0;
          while (plist.hasMoreTokens())
          {
            String value = plist.nextToken();
            QueryParameter qp = (QueryParameter) m_ParameterList.elementAt(indx);
            if ( null != qp )
            {
              qp.setTypeName(value);
            }

            indx++;
          }
      }
  }

  /**
   * Add an untyped value to the parameter list.
   * @param value
   *
   */
  public void addParameter( String value )
  {
    addParameterWithType(value, null);
  }

  /**
   * Add a typed parameter to the parameter list.
   * @param value
   * @param Type
   *
   */
  public void addParameterWithType( String value, String Type )
  {
    m_ParameterList.addElement( new QueryParameter(value, Type) );
  }


  /**
   * Add a single parameter to the parameter list
   * formatted as an Element
   * @param e
   *
   */
  public void addParameterFromElement( Element e )
  {
    NamedNodeMap attrs = e.getAttributes();
    Node Type = attrs.getNamedItem("type");
    Node n1  = e.getFirstChild();
    if (null != n1)
    {
      String value = n1.getNodeValue();
      if (value == null) value = "";
      m_ParameterList.addElement( new QueryParameter(value, Type.getNodeValue()) );
    }
  }


  /**
   * Add a section of parameters to the Parameter List
   * Do each element from the list
   * @param nl
   *
   */
  public void addParameterFromElement( NodeList nl )
  {
    //
    // Each child of the NodeList represents a node
    // match from the select= statment. Process each
    // of them as a seperate list.
    // The XML Format is as follows
    //
    // <START-TAG>
    //   <TAG1 type="int">value</TAG1>
    //   <TAGA type="int">value</TAGA>
    //   <TAG2 type="string">value</TAG2>
    // </START-TAG>
    //
    // The XSL to process this is formatted as follows
    // <xsl:param name="plist" select="//START-TAG" />
    // <sql:addParameter( $plist );
    //
    int count = nl.getLength();
    for (int x=0; x<count; x++)
    {
      addParameters( (Element) nl.item(x));
    }
  }

  /**
   * @param elem
   *
   */
  private void addParameters( Element elem )
  {
    //
    // Process all of the Child Elements
    // The format is as follows
    //
    //<TAG type ="typeid">value</TAG>
    //<TAG1 type ="typeid">value</TAG1>
    //<TAGA type ="typeid">value</TAGA>
    //
    // The name of the Node is not important just is value
    // and if it contains a type attribute

    Node n = elem.getFirstChild();

    if (null == n) return;

    do
    {
      if (n.getNodeType() == Node.ELEMENT_NODE)
      {
        NamedNodeMap attrs = n.getAttributes();
        Node Type = attrs.getNamedItem("type");
        String TypeStr;

        if (Type == null) TypeStr = "string";
        else TypeStr = Type.getNodeValue();

        Node n1  = n.getFirstChild();
        if (null != n1)
        {
          String value = n1.getNodeValue();
          if (value == null) value = "";


          m_ParameterList.addElement(
            new QueryParameter(value, TypeStr) );
        }
      }
    } while ( (n = n.getNextSibling()) != null);
  }

  /**
   *
   */
  public void clearParameters( )
  {
    m_ParameterList.removeAllElements();
  }

  /**
   * There is a problem with some JDBC drivers when a Connection
   * is open and the JVM shutsdown. If there is a problem, there
   * is no way to control the currently open connections in the
   * pool. So for the default connection pool, the actuall pooling
   * mechinsm is disabled by default. The Stylesheet designer can
   * re-enabled pooling to take advantage of connection pools.
   * The connection pool can even be disabled which will close all
   * outstanding connections.
   *
   * @deprecated Use setFeature("default-pool-enabled", "true");
   */
  public void enableDefaultConnectionPool( )
  {

    if (DEBUG)
      System.out.println("Enabling Default Connection Pool");

    m_DefaultPoolingEnabled = true;

    if (m_ConnectionPool == null) return;
    if (m_IsDefaultPool) return;

    m_ConnectionPool.setPoolEnabled(true);

  }

  /**
   * See enableDefaultConnectionPool
   *
   * @deprecated Use setFeature("default-pool-enabled", "false");
   */
  public void disableDefaultConnectionPool( )
  {
    if (DEBUG)
      System.out.println("Disabling Default Connection Pool");

    m_DefaultPoolingEnabled = false;

    if (m_ConnectionPool == null) return;
    if (!m_IsDefaultPool) return;

    m_ConnectionPool.setPoolEnabled(false);
  }


  /**
   * Control how the SQL Document uses memory. In Streaming Mode,
   * memory consumption is greatly reduces so you can have queries
   * of unlimited size but it will not let you traverse the data
   * more than once.
   *
   * @deprecated Use setFeature("streaming", "true");
   */
  public void enableStreamingMode( )
  {

    if (DEBUG)
      System.out.println("Enabling Streaming Mode");

    m_IsStreamingEnabled = true;
  }

  /**
   * Control how the SQL Document uses memory. In Streaming Mode,
   * memory consumption is greatly reduces so you can have queries
   * of unlimited size but it will not let you traverse the data
   * more than once.
   *
   * @deprecated Use setFeature("streaming", "false");
   */
  public void disableStreamingMode( )
  {

    if (DEBUG)
      System.out.println("Disable Streaming Mode");

    m_IsStreamingEnabled = false;
  }

  /**
   * Provide access to the last error that occued. This error
   * may be over written when the next operation occurs.
   *
   */
  public DTM getError( )
  {
    if ( m_FullErrors )
    {
      for ( int idx = 0 ; idx < m_OpenSQLDocuments.size() ; idx++ )
      {
        SQLDocument doc = (SQLDocument)m_OpenSQLDocuments.elementAt(idx);
        SQLWarning warn = doc.checkWarnings();
        if ( warn != null ) setError(null, doc, warn);
      }
    }

    return(buildErrorDocument());
  }

  /**
   * Close the connection to the data source.
   *
   * @throws SQLException
   */
  public void close( )throws SQLException
  {
    if (DEBUG)
      System.out.println("Entering XConnection.close()");

    //
    // This function is included just for Legacy support
    // If it is really called then we must me using a single
    // document interface, so close all open documents.

    while(m_OpenSQLDocuments.size() != 0)
    {
      SQLDocument d = (SQLDocument) m_OpenSQLDocuments.elementAt(0);
      try
      {
        // If we are using the Default Connection Pool, then
        // force the connection pool to flush unused connections.
        d.close(m_IsDefaultPool);
      }
      catch (Exception se ) {}

      m_OpenSQLDocuments.removeElementAt(0);
    }

    if ( null != m_Connection )
    {
      m_ConnectionPool.releaseConnection(m_Connection);
      m_Connection = null;
    }

    if (DEBUG)
      System.out.println("Exiting XConnection.close");
  }

  /**
   * Close the connection to the data source. Only close the connections
   * for a single document.
   *
   * @throws SQLException
   */

  public void close(ExpressionContext exprContext, Object doc) throws SQLException 
  {
    if (DEBUG)
        System.out.println("Entering XConnection.close(" + doc + ")");

    SQLDocument sqlDoc = locateSQLDocument(exprContext, doc);
    if (sqlDoc != null)
    {
      // If we are using the Default Connection Pool, then
      // force the connection pool to flush unused connections.
      sqlDoc.close(m_IsDefaultPool);
      m_OpenSQLDocuments.remove(sqlDoc);
    } 
  }


  /**
   * When an SQL Document is returned as a DTM object, the XSL variable is actually 
   * assigned as a DTMIterator. This is a helper function that will allow you to get
   * a reference to the original SQLDocument from the iterator.
   * 
   * Original code submitted by 
   *  Moraine Didier mailto://didier.moraine@winterthur.be
   * @param doc
   * @return
   */
  private SQLDocument locateSQLDocument(ExpressionContext exprContext, Object doc)
  {
    try
    {
      if (doc instanceof DTMNodeIterator)
      {
        DTMNodeIterator dtmIter = (DTMNodeIterator)doc;
        try
        {
          DTMNodeProxy root = (DTMNodeProxy)dtmIter.getRoot();
          return (SQLDocument) root.getDTM();
        }
        catch (Exception e)
        {
          XNodeSet xNS = (XNodeSet)dtmIter.getDTMIterator();
          DTMIterator iter = (DTMIterator)xNS.getContainedIter();
          DTM dtm = iter.getDTM(xNS.nextNode());
          return (SQLDocument)dtm;
        }
      }

/*
      XNodeSet xNS = (XNodeSet)dtmIter.getDTMIterator();
      OneStepIterator iter = (OneStepIterator)xNS.getContainedIter();
      DTMManager aDTMManager = (DTMManager)iter.getDTMManager();
      return (SQLDocument)aDTMManager.getDTM(xNS.nextNode());
*/
      setError(new Exception("SQL Extension:close - Can Not Identify SQLDocument"), exprContext);    
      return null;  
    }
    catch(Exception e)
    {
      setError(e, exprContext);
      return null;
    }
  }
  
  /**
   * @param exprContext
   * @param excp
   *
   */
  private SQLErrorDocument buildErrorDocument()
  {
    SQLErrorDocument eDoc = null;

    if ( m_LastSQLDocumentWithError != null)
    {
      // @todo
      // Do we need to do something with this ??
      //    m_Error != null || (m_FullErrors && m_Warning != null) )

      ExpressionContext ctx = m_LastSQLDocumentWithError.getExpressionContext();
      SQLWarning        warn = m_LastSQLDocumentWithError.checkWarnings();


      try
      {
        DTMManager mgr =
          ((XPathContext.XPathExpressionContext)ctx).getDTMManager();
        DTMManagerDefault mgrDefault = (DTMManagerDefault) mgr;
        int dtmIdent = mgrDefault.getFirstFreeDTMID();

        eDoc = new SQLErrorDocument(
            mgr, dtmIdent<<DTMManager.IDENT_DTM_NODE_BITS,
            m_Error, warn, m_FullErrors);

        // Register our document
        mgrDefault.addDTM(eDoc, dtmIdent);

        // Clear the error and warning.
        m_Error = null;
        m_LastSQLDocumentWithError = null;
      }
      catch(Exception e)
      {
        eDoc = null;
      }
    }

    return(eDoc);
  }


  /**
   * This is an internal version of Set Error that is called withen
   * XConnection where there is no SQLDocument created yet. As in the
   * Connect statement or creation of the ConnectionPool.
   */
  public void setError(Exception excp,ExpressionContext expr)
  {
    try
    {
      ErrorListener listen = expr.getErrorListener();
      if ( listen != null && excp != null )
      {
        
        listen.warning(
          new TransformerException(excp.toString(),
          expr.getXPathContext().getSAXLocator(), excp));
      }
    }
    catch(Exception e) {}
  }

  /**
   * Set an error and/or warning on this connection.
   *
   */
  public void setError(Exception excp, SQLDocument doc, SQLWarning warn)
  {

    ExpressionContext cont = doc.getExpressionContext();
    m_LastSQLDocumentWithError = doc;

    try
    {
      ErrorListener listen = cont.getErrorListener();
      if ( listen != null && excp != null )
      listen.warning(
        new TransformerException(excp.toString(),
        cont.getXPathContext().getSAXLocator(), excp));

      if ( listen != null && warn != null )
      {
        listen.warning(new TransformerException(
          warn.toString(), cont.getXPathContext().getSAXLocator(), warn));
      }

      // Assume there will be just one error, but perhaps multiple warnings.
      if ( excp != null )  m_Error = excp;

      if ( warn != null )
      {
        // Because the log may not have processed the previous warning yet
        // we need to make a new one.
        SQLWarning tw =
          new SQLWarning(warn.getMessage(), warn.getSQLState(),
            warn.getErrorCode());
        SQLWarning nw = warn.getNextWarning();
        while ( nw != null )
        {
          tw.setNextWarning(new SQLWarning(nw.getMessage(),
            nw.getSQLState(), nw.getErrorCode()));

          nw = nw.getNextWarning();
        }

        tw.setNextWarning(
          new SQLWarning(warn.getMessage(), warn.getSQLState(),
            warn.getErrorCode()));

//        m_Warning = tw;

      }
    }
    catch(Exception e)
    {
      //m_Error = null;
    }
  }

  /**
   * Set feature options for this XConnection.
   * @param feature The name of the feature being set, currently supports (streaming, inline-variables, multiple-results, cache-statements, default-pool-enabled).
   * @param setting The new setting for the specified feature, currently "true" is true and anything else is false.
   *
   */
  public void setFeature(String feature, String setting)
  {
    boolean value = false;

    if ( "true".equalsIgnoreCase(setting) ) value = true;

    if ( "streaming".equalsIgnoreCase(feature) )
    {
      m_IsStreamingEnabled = value;
    }
    else if ( "inline-variables".equalsIgnoreCase(feature) )
    {
      m_InlineVariables = value;
    }
    else if ( "multiple-results".equalsIgnoreCase(feature) )
    {
      m_IsMultipleResultsEnabled = value;
    }
    else if ( "cache-statements".equalsIgnoreCase(feature) )
    {
      m_IsStatementCachingEnabled = value;
    }
    else if ( "default-pool-enabled".equalsIgnoreCase(feature) )
    {
      m_DefaultPoolingEnabled = value;

      if (m_ConnectionPool == null) return;
      if (m_IsDefaultPool) return;

      m_ConnectionPool.setPoolEnabled(value);
    }
    else if ( "full-errors".equalsIgnoreCase(feature) )
    {
      m_FullErrors = value;
    }
  }

  /**
   * Get feature options for this XConnection.
   * @param feature The name of the feature to get the setting for.
   * @return The setting of the specified feature. Will be "true" or "false" (null if the feature is not known)
   */
  public String getFeature(String feature)
  {
    String value = null;

    if ( "streaming".equalsIgnoreCase(feature) )
      value = m_IsStreamingEnabled ? "true" : "false";
    else if ( "inline-variables".equalsIgnoreCase(feature) )
      value = m_InlineVariables ? "true" : "false";
    else if ( "multiple-results".equalsIgnoreCase(feature) )
      value = m_IsMultipleResultsEnabled ? "true" : "false";
    else if ( "cache-statements".equalsIgnoreCase(feature) )
      value = m_IsStatementCachingEnabled ? "true" : "false";
    else if ( "default-pool-enabled".equalsIgnoreCase(feature) )
      value = m_DefaultPoolingEnabled ? "true" : "false";
    else if ( "full-errors".equalsIgnoreCase(feature) )
      value = m_FullErrors ? "true" : "false";

    return(value);
  }



  /**
   *
   */
  protected void finalize( )
  {
    if (DEBUG) System.out.println("In XConnection, finalize");
    try
    {
      close();
    }
    catch(Exception e)
    {
      // Empty We are final Anyway
    }
  }

}
