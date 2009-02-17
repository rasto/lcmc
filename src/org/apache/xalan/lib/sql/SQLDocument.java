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
 * $Id: SQLDocument.java,v 1.28 2005/01/23 00:16:04 mcnamara Exp $
 */

package org.apache.xalan.lib.sql;

import java.util.Vector;

import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xpath.XPathContext;

import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTM;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.*;
import org.apache.xml.dtm.ref.*;

/**
 * The SQL Document is the main controlling class the executesa SQL Query
 */
public class SQLDocument extends DTMDocument
{

  /**
   */
  private boolean DEBUG = false;

  /**
   */
  private static final String S_NAMESPACE = "http://xml.apache.org/xalan/SQLExtension";


  /**
   */
  private static final String S_SQL = "sql";

  /**
   */
  private static final String S_ROW_SET = "row-set";

  /**
   */
  private static final String S_METADATA = "metadata";

  /**
   */
  private static final String S_COLUMN_HEADER = "column-header";

  /**
   */
  private static final String S_ROW = "row";

  /**
   */
  private static final String S_COL = "col";

  /**
   */
  private static final String S_OUT_PARAMETERS = "out-parameters";

  /**
   */
  private static final String S_CATALOGUE_NAME = "catalogue-name";
  /**
   */
  private static final String S_DISPLAY_SIZE = "column-display-size";
  /**
   */
  private static final String S_COLUMN_LABEL = "column-label";
  /**
   */
  private static final String S_COLUMN_NAME = "column-name";
  /**
   */
  private static final String S_COLUMN_TYPE = "column-type";
  /**
   */
  private static final String S_COLUMN_TYPENAME = "column-typename";
  /**
   */
  private static final String S_PRECISION = "precision";
  /**
   */
  private static final String S_SCALE = "scale";
  /**
   */
  private static final String S_SCHEMA_NAME = "schema-name";
  /**
   */
  private static final String S_TABLE_NAME = "table-name";
  /**
   */
  private static final String S_CASESENSITIVE = "case-sensitive";
  /**
   */
  private static final String S_DEFINITELYWRITABLE = "definitely-writable";
  /**
   */
  private static final String S_ISNULLABLE = "nullable";
  /**
   */
  private static final String S_ISSIGNED = "signed";
  /**
   */
  private static final String S_ISWRITEABLE = "writable";
  /**
   */
  private static final String S_ISSEARCHABLE = "searchable";

  /**
   */
  private int m_SQL_TypeID = 0;
  /**
   */
  private int m_MetaData_TypeID = 0;
  /**
   */
  private int m_ColumnHeader_TypeID = 0;
  /**
   */
  private int m_RowSet_TypeID = 0;
  /**
   */
  private int m_Row_TypeID = 0;
  /**
   */
  private int m_Col_TypeID = 0;
  /**
   */
  private int m_OutParameter_TypeID = 0;

  /**
   */
  private int m_ColAttrib_CATALOGUE_NAME_TypeID = 0;
  /**
   */
  private int m_ColAttrib_DISPLAY_SIZE_TypeID = 0;
  /**
   */
  private int m_ColAttrib_COLUMN_LABEL_TypeID = 0;
  /**
   */
  private int m_ColAttrib_COLUMN_NAME_TypeID = 0;
  /**
   */
  private int m_ColAttrib_COLUMN_TYPE_TypeID = 0;
  /**
   */
  private int m_ColAttrib_COLUMN_TYPENAME_TypeID = 0;
  /**
   */
  private int m_ColAttrib_PRECISION_TypeID = 0;
  /**
   */
  private int m_ColAttrib_SCALE_TypeID = 0;
  /**
   */
  private int m_ColAttrib_SCHEMA_NAME_TypeID = 0;
  /**
   */
  private int m_ColAttrib_TABLE_NAME_TypeID = 0;
  /**
   */
  private int m_ColAttrib_CASESENSITIVE_TypeID = 0;
  /**
   */
  private int m_ColAttrib_DEFINITELYWRITEABLE_TypeID = 0;
  /**
   */
  private int m_ColAttrib_ISNULLABLE_TypeID = 0;
  /**
   */
  private int m_ColAttrib_ISSIGNED_TypeID = 0;
  /**
   */
  private int m_ColAttrib_ISWRITEABLE_TypeID = 0;
  /**
   */
  private int m_ColAttrib_ISSEARCHABLE_TypeID = 0;

  /**
   * The Statement used to extract the data from the database connection.
   */
  private Statement m_Statement = null;

  /**
   * Expression COntext used to creat this document
   * may be used to grab variables from the XSL processor
   */
  private ExpressionContext m_ExpressionContext = null;

  /**
   * The Connection Pool where we has derived all of our connections
   * for this document
   */
  private ConnectionPool m_ConnectionPool = null;

  /**
   * The current ResultSet.
   */
  private ResultSet m_ResultSet = null;

  /**
   * The parameter definitions if this is a callable
   * statement with output parameters.
   */
  private SQLQueryParser m_QueryParser = null;

  /**
   * As the column header array is built, keep the node index
   * for each Column.
   * The primary use of this is to locate the first attribute for
   * each column in each row as we add records.
   */
  private int[] m_ColHeadersIdx;

  /**
   * An indicator on how many columns are in this query
   */
  private int m_ColCount;

  /**
   * The Index of the MetaData Node. Currently the MetaData Node contains the
   *
   */
  private int m_MetaDataIdx = DTM.NULL;

  /**
   * The index of the Row Set node. This is the sibling directly after
   * the last Column Header.
   */
  private int m_RowSetIdx = DTM.NULL;

  /**
   */
  private int m_SQLIdx = DTM.NULL;

  /**
   * Demark the first row element where we started adding rows into the
   * Document.
   */
  private int m_FirstRowIdx = DTM.NULL;

  /**
   * Keep track of the Last row inserted into the DTM from the ResultSet.
   * This will be used as the index of the parent Row Element when adding
   * a row.
   */
  private int m_LastRowIdx = DTM.NULL;

  /**
   * Streaming Mode Control, In Streaming mode we reduce the memory
   * footprint since we only use a single row instance.
   */
  private boolean m_StreamingMode = true;

  /**
   * Multiple Result sets mode (metadata inside rowset).
   */
  private boolean m_MultipleResults = false;

  /**
   * Flag to detect if an error occured during an operation
   * Defines how errors are handled and how the SQL Connection
   * is closed.
   */
  private boolean m_HasErrors = false;

  /**
   * Is statement caching enabled.
   */
  private boolean m_IsStatementCachingEnabled = false;

  /**
   * XConnection this document came from.
   */
  private XConnection m_XConnection = null;

  /**
   * @param mgr
   * @param ident
   * @throws SQLException
   */
  // public cSQLDocument(DTMManager mgr, int ident, Statement stmt,
  //  ResultSet singleResult, Vector paramdefs, boolean streamingMode,
  // boolean multipleResults, boolean statementCachingEnabled) throws SQLException

  public SQLDocument(DTMManager mgr, int ident)
  {
    super(mgr, ident);
  }

  /**
   * This static method simplifies the creation of an SQL Document and allows
   * us to embedd the complexity of creating / handling the dtmIdent inside
   * the document. This type of method may better placed inside the DTMDocument
   * code
   */
  public static SQLDocument getNewDocument(ExpressionContext exprContext)
  {
    DTMManager mgr =
      ((XPathContext.XPathExpressionContext)exprContext).getDTMManager();
    DTMManagerDefault  mgrDefault = (DTMManagerDefault) mgr;


    int dtmIdent = mgrDefault.getFirstFreeDTMID();

    SQLDocument doc =
      new SQLDocument(mgr, dtmIdent << DTMManager.IDENT_DTM_NODE_BITS);

    // Register the document
    mgrDefault.addDTM(doc, dtmIdent);
    doc.setExpressionContext(exprContext);

    return doc;
  }

  /**
   * When building the SQL Document, we need to store the Expression
   * Context that was used to create the document. This will be se to
   * reference items int he XSLT process such as any variables that were
   * present.
   */
  protected void setExpressionContext(ExpressionContext expr)
  {
    m_ExpressionContext = expr;
  }

  /**
   * Return the context used to build this document
   */
  public ExpressionContext getExpressionContext()
  {
    return m_ExpressionContext;
  }


  public void execute(XConnection xconn, SQLQueryParser query)
    throws SQLException
  {
    try
    {
      m_StreamingMode = "true".equals(xconn.getFeature("streaming"));
      m_MultipleResults = "true".equals(xconn.getFeature("multiple-results"));
      m_IsStatementCachingEnabled = "true".equals(xconn.getFeature("cache-statements"));
      m_XConnection = xconn;
      m_QueryParser = query;

      executeSQLStatement();

      createExpandedNameTable();

      // Start the document here
      m_DocumentIdx = addElement(0, m_Document_TypeID, DTM.NULL, DTM.NULL);
      m_SQLIdx = addElement(1, m_SQL_TypeID,  m_DocumentIdx, DTM.NULL);


      if ( ! m_MultipleResults )
        extractSQLMetaData(m_ResultSet.getMetaData());

      // Only grab the first row, subsequent rows will be
      // fetched on demand.
      // We need to do this here so at least on row is set up
      // to measure when we are actually reading rows.

      // We won't grab the first record in case the skip function
      // is applied prior to looking at the first record.
      // JCG Changed 9/15/04
      // addRowToDTMFromResultSet();
    }
    catch(SQLException e)
    {
      m_HasErrors = true;
      throw e;
    }
  }

  private void executeSQLStatement() throws SQLException
  {
    m_ConnectionPool = m_XConnection.getConnectionPool();

    Connection conn = m_ConnectionPool.getConnection();

    if (! m_QueryParser.hasParameters() )
    {
      m_Statement = conn.createStatement();
      m_ResultSet = m_Statement.executeQuery(m_QueryParser.getSQLQuery());


    }

    else if (m_QueryParser.isCallable())
    {
      CallableStatement cstmt =
        conn.prepareCall(m_QueryParser.getSQLQuery());
      m_QueryParser.registerOutputParameters(cstmt);
      m_QueryParser.populateStatement(cstmt, m_ExpressionContext);
      m_Statement = cstmt;
      if (! cstmt.execute()) throw new SQLException("Error in Callable Statement");

      m_ResultSet = m_Statement.getResultSet();
    }
    else
    {
      PreparedStatement stmt =
        conn.prepareStatement(m_QueryParser.getSQLQuery());
      m_QueryParser.populateStatement(stmt, m_ExpressionContext);
      m_Statement = stmt;
      m_ResultSet = stmt.executeQuery();
    }

  }

  /**
   * Push the record set forward value rows. Used to help in 
   * SQL pagination.
   * 
   * @param value
   */
  public void skip( int value )
  {
    try
    {
      if (m_ResultSet != null) m_ResultSet.relative(value);
    }
    catch(Exception origEx)
    {
      // For now let's assume that the relative method is not supported.
      // So let's do it manually.
      try
      {
        for (int x=0; x<value; x++)
        {
          if (! m_ResultSet.next()) break;
        }
      }
      catch(Exception e)
      {
        // If we still fail, add in both exceptions
        m_XConnection.setError(origEx, this, checkWarnings());
        m_XConnection.setError(e, this, checkWarnings());
      }
    }
  }


  /**
   * Extract the Meta Data and build the Column Attribute List.
   * @param meta
   * @return
   */
  private void extractSQLMetaData( ResultSetMetaData meta )
  {
    // Build the Node Tree, just add the Column Header
    // branch now, the Row & col elements will be added
    // on request.

    // Add in the row-set Element

    // Add in the MetaData Element
    m_MetaDataIdx = addElement(1, m_MetaData_TypeID,  m_MultipleResults ? m_RowSetIdx : m_SQLIdx, DTM.NULL);

    try
    {
      m_ColCount = meta.getColumnCount();
      m_ColHeadersIdx = new int[m_ColCount];
    }
    catch(Exception e)
    {
      m_XConnection.setError(e, this, checkWarnings());
      //error("ERROR Extracting Metadata");
    }

    // The ColHeaderIdx will be used to keep track of the
    // Element entries for the individual Column Header.
    int lastColHeaderIdx = DTM.NULL;

    // JDBC Columms Start at 1
    int i = 1;
    for (i=1; i<= m_ColCount; i++)
    {
      m_ColHeadersIdx[i-1] =
        addElement(2,m_ColumnHeader_TypeID, m_MetaDataIdx, lastColHeaderIdx);

      lastColHeaderIdx = m_ColHeadersIdx[i-1];
      // A bit brute force, but not sure how to clean it up

      try
      {
        addAttributeToNode(
          meta.getColumnName(i),
          m_ColAttrib_COLUMN_NAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_COLUMN_NAME_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.getColumnLabel(i),
          m_ColAttrib_COLUMN_LABEL_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_COLUMN_LABEL_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.getCatalogName(i),
          m_ColAttrib_CATALOGUE_NAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_CATALOGUE_NAME_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          new Integer(meta.getColumnDisplaySize(i)),
          m_ColAttrib_DISPLAY_SIZE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_DISPLAY_SIZE_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          new Integer(meta.getColumnType(i)),
          m_ColAttrib_COLUMN_TYPE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_COLUMN_TYPE_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.getColumnTypeName(i),
          m_ColAttrib_COLUMN_TYPENAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_COLUMN_TYPENAME_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          new Integer(meta.getPrecision(i)),
          m_ColAttrib_PRECISION_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_PRECISION_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          new Integer(meta.getScale(i)),
          m_ColAttrib_SCALE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_SCALE_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.getSchemaName(i),
          m_ColAttrib_SCHEMA_NAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_SCHEMA_NAME_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.getTableName(i),
          m_ColAttrib_TABLE_NAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_TABLE_NAME_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.isCaseSensitive(i) ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_CASESENSITIVE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_CASESENSITIVE_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.isDefinitelyWritable(i) ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_DEFINITELYWRITEABLE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_DEFINITELYWRITEABLE_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.isNullable(i) != 0 ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_ISNULLABLE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_ISNULLABLE_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.isSigned(i) ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_ISSIGNED_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_ISSIGNED_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.isWritable(i) == true ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_ISWRITEABLE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_ISWRITEABLE_TypeID, lastColHeaderIdx);
      }

      try
      {
        addAttributeToNode(
          meta.isSearchable(i) == true ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_ISSEARCHABLE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_ISSEARCHABLE_TypeID, lastColHeaderIdx);
      }
    }
  }

  /**
   * Populate the Expanded Name Table with the Node that we will use.
   * Keep a reference of each of the types for access speed.
   * @return
   */
  protected void createExpandedNameTable( )
  {
    super.createExpandedNameTable();

    m_SQL_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_SQL, DTM.ELEMENT_NODE);

    m_MetaData_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_METADATA, DTM.ELEMENT_NODE);

    m_ColumnHeader_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_HEADER, DTM.ELEMENT_NODE);
    m_RowSet_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ROW_SET, DTM.ELEMENT_NODE);
    m_Row_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ROW, DTM.ELEMENT_NODE);
    m_Col_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COL, DTM.ELEMENT_NODE);
    m_OutParameter_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_OUT_PARAMETERS, DTM.ELEMENT_NODE);

    m_ColAttrib_CATALOGUE_NAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_CATALOGUE_NAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_DISPLAY_SIZE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_DISPLAY_SIZE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_COLUMN_LABEL_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_LABEL, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_COLUMN_NAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_NAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_COLUMN_TYPE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_TYPE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_COLUMN_TYPENAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_TYPENAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_PRECISION_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_PRECISION, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_SCALE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_SCALE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_SCHEMA_NAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_SCHEMA_NAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_TABLE_NAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_TABLE_NAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_CASESENSITIVE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_CASESENSITIVE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_DEFINITELYWRITEABLE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_DEFINITELYWRITABLE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_ISNULLABLE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ISNULLABLE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_ISSIGNED_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ISSIGNED, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_ISWRITEABLE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ISWRITEABLE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_ISSEARCHABLE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ISSEARCHABLE, DTM.ATTRIBUTE_NODE);
  }


  /**
   * Pull a record from the result set and map it to a DTM based ROW
   * If we are in Streaming mode, then only create a single row and
   * keep copying the data into the same row. This will keep the memory
   * footprint constint independant of the RecordSet Size. If we are not
   * in Streaming mode then create ROWS for the whole tree.
   * @return
   */
  private boolean addRowToDTMFromResultSet( )
  {
    try
    {
      // If we have not started the RowSet yet, then add it to the
      // tree.
      if (m_FirstRowIdx == DTM.NULL)
      {
        m_RowSetIdx =
          addElement(1, m_RowSet_TypeID,  m_SQLIdx, m_MultipleResults ? m_RowSetIdx : m_MetaDataIdx);
        if ( m_MultipleResults ) extractSQLMetaData(m_ResultSet.getMetaData());
      }


      // Check to see if all the data has been read from the Query.
      // If we are at the end the signal that event
      if ( ! m_ResultSet.next())
      {
        // In Streaming mode, the current ROW will always point back
        // to itself until all the data was read. Once the Query is
        // empty then point the next row to DTM.NULL so that the stream
        // ends. Only do this if we have statted the loop to begin with.

        if (m_StreamingMode && (m_LastRowIdx != DTM.NULL))
        {
          // We are at the end, so let's untie the mark
          m_nextsib.setElementAt(DTM.NULL, m_LastRowIdx);
        }

        m_ResultSet.close();
        if ( m_MultipleResults )
        {
          while ( !m_Statement.getMoreResults() && m_Statement.getUpdateCount() >= 0 ) ;
          m_ResultSet = m_Statement.getResultSet();
        }
        else
          m_ResultSet = null;

        if ( m_ResultSet != null )
        {
          m_FirstRowIdx = DTM.NULL;
          addRowToDTMFromResultSet();
        }
        else
        {
          Vector parameters = m_QueryParser.getParameters();
          // Get output parameters.
          if ( parameters != null )
          {
            int outParamIdx = addElement(1, m_OutParameter_TypeID,  m_SQLIdx, m_RowSetIdx);
            int lastColID = DTM.NULL;
            for ( int indx = 0 ; indx < parameters.size() ; indx++ )
            {
              QueryParameter parm = (QueryParameter)parameters.elementAt(indx);
              if ( parm.isOutput() )
              {
                Object rawobj = ((CallableStatement)m_Statement).getObject(indx + 1);
                lastColID = addElementWithData(rawobj, 2, m_Col_TypeID, outParamIdx, lastColID);
                addAttributeToNode(parm.getName(), m_ColAttrib_COLUMN_NAME_TypeID, lastColID);
                addAttributeToNode(parm.getName(), m_ColAttrib_COLUMN_LABEL_TypeID, lastColID);
                addAttributeToNode(new Integer(parm.getType()), m_ColAttrib_COLUMN_TYPE_TypeID, lastColID);
                addAttributeToNode(parm.getTypeName(), m_ColAttrib_COLUMN_TYPENAME_TypeID, lastColID);
              }
            }
          }

          SQLWarning warn = checkWarnings();
          if ( warn != null )	m_XConnection.setError(null, null, warn);
        }

        return false;
      }

      // If this is the first time here, start the new level
      if (m_FirstRowIdx == DTM.NULL)
      {
        m_FirstRowIdx =
          addElement(2, m_Row_TypeID, m_RowSetIdx, m_MultipleResults ? m_MetaDataIdx : DTM.NULL);

        m_LastRowIdx = m_FirstRowIdx;

        if (m_StreamingMode)
        {
          // Let's tie the rows together until the end.
          m_nextsib.setElementAt(m_LastRowIdx, m_LastRowIdx);
        }

      }
      else
      {
        //
        // If we are in Streaming mode, then only use a single row instance
        if (! m_StreamingMode)
        {
          m_LastRowIdx = addElement(2, m_Row_TypeID, m_RowSetIdx, m_LastRowIdx);
        }
      }

      // If we are not in streaming mode, this will always be DTM.NULL
      // If we are in streaming mode, it will only be DTM.NULL the first time
      int colID = _firstch(m_LastRowIdx);

      // Keep Track of who our parent was when adding new col objects.
      int pcolID = DTM.NULL;

      // Columns in JDBC Start at 1 and go to the Extent
      for (int i=1; i<= m_ColCount; i++)
      {
        // Just grab the Column Object Type, we will convert it to a string
        // later.
        Object o = m_ResultSet.getObject(i);

        // Create a new column object if one does not exist.
        // In Streaming mode, this mechinism will reuse the column
        // data the second and subsequent row accesses.
        if (colID == DTM.NULL)
        {
          pcolID = addElementWithData(o,3,m_Col_TypeID, m_LastRowIdx, pcolID);
          cloneAttributeFromNode(pcolID, m_ColHeadersIdx[i-1]);
        }
        else
        {
          // We must be in streaming mode, so let's just replace the data
          // If the firstch was not set then we have a major error
          int dataIdent = _firstch(colID);
          if (dataIdent == DTM.NULL)
          {
            error("Streaming Mode, Data Error");
          }
          else
          {
            m_ObjectArray.setAt(dataIdent, o);
          }
        } // If

        // In streaming mode, this will be !DTM.NULL
        // So if the elements were already established then we
        // should be able to walk them in order.
        if (colID != DTM.NULL)
        {
          colID = _nextsib(colID);
        }

      } // For Col Loop
    }
    catch(Exception e)
    {
      if (DEBUG)
      {
        System.out.println(
          "SQL Error Fetching next row [" + e.getLocalizedMessage() + "]");
      }

      m_XConnection.setError(e, this, checkWarnings());
      m_HasErrors = true;
    }

    // Only do a single row...
    return true;
  }


  /**
   * Used by the XConnection to determine if the Document should
   * handle the document differently.
   */
  public boolean hasErrors()
  {
    return m_HasErrors;
  }

  /**
   * Close down any resources used by this document. If an SQL Error occure
   * while the document was being accessed, the SQL Connection used to create
   * this document will be released to the Connection Pool on error. This allows
   * the COnnection Pool to give special attention to any connection that may
   * be in a errored state.
   *
   */
  public void close(boolean flushConnPool )
  {
    try
    {
      SQLWarning warn = checkWarnings();
      if ( warn != null ) m_XConnection.setError(null, null, warn);
    }
    catch(Exception e) {}

    try
    {
      if (null != m_ResultSet)
      {
        m_ResultSet.close();
        m_ResultSet = null;
      }
    }
    catch(Exception e) {}


    Connection conn = null;

    try
    {
      if (null != m_Statement)
      {
        conn = m_Statement.getConnection();
        m_Statement.close();
        m_Statement = null;
      }
    }
    catch(Exception e) {}

    try
    {
      if (conn != null)
      {
        if (m_HasErrors)  m_ConnectionPool.releaseConnectionOnError(conn);
        else m_ConnectionPool.releaseConnection(conn);
//        if (flushConnPool)  m_ConnectionPool.freeUnused();
      }
    }
    catch(Exception e) {}

    getManager().release(this, true);
  }

  /**
   * @return
   */
  protected boolean nextNode( )
  {
    if (DEBUG) System.out.println("nextNode()");
    try
    {
      return false;
//      return m_ResultSet.isAfterLast();
    }
    catch(Exception e)
    {
      return false;
    }
  }

  /**
   * @param identity
   * @return
   */
  protected int _nextsib( int identity )
  {
    // If we are asking for the next row and we have not
    // been there yet then let's see if we can get another
    // row from the ResultSet.
    //

  if ( m_ResultSet != null )
  {
      int id = _exptype(identity);
      
      // We need to prime the pump since we don't do it in execute any more.
      if (m_FirstRowIdx == DTM.NULL)
      {
        addRowToDTMFromResultSet();
      }
      
      if (
        ( id == m_Row_TypeID) &&
        (identity >= m_LastRowIdx) )
      {
        if (DEBUG) System.out.println("reading from the ResultSet");
        addRowToDTMFromResultSet();
      }
      else if ( m_MultipleResults && identity == m_RowSetIdx )
      {
        if (DEBUG) System.out.println("reading for next ResultSet");
      int startIdx = m_RowSetIdx;
      while ( startIdx == m_RowSetIdx && m_ResultSet != null )
            addRowToDTMFromResultSet();
      }
  }

    return super._nextsib(identity);
  }

  public void documentRegistration()
  {
    if (DEBUG) System.out.println("Document Registration");
  }

  public void documentRelease()
  {
    if (DEBUG) System.out.println("Document Release");
  }

  public SQLWarning checkWarnings()
  {
    SQLWarning warn = null;
    if ( m_Statement != null )
    {
      try
      {
        warn = m_Statement.getWarnings();
        m_Statement.clearWarnings();
      }
      catch (SQLException se) {}
    }
    return(warn);
  }
}
