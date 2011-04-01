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
 * $Id: SQLErrorDocument.java,v 1.8 2004/02/11 17:56:36 minchau Exp $
 */

package org.apache.xalan.lib.sql;

import java.sql.SQLException;
import java.sql.SQLWarning;

import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMManager;

/**
 *
 * A base class that will convert an exception into an XML stream
 * that can be returned in place of the standard result. The XML
 * format returned is a follows.
 *
 * <ext-error>
 *  <message> The Message for a generic error </message>
 *  <sql-error>
 *    <message> SQL Message from the Exception thrown </message>
 *    <code> SQL Error Code </stack>
 *  </sql-error>
 * <ext-error>
 *
 */

/**
 * The SQL Document is the main controlling class the executesa SQL Query
 */
public class SQLErrorDocument extends DTMDocument
{
  /**
   */
  private static final String S_EXT_ERROR = "ext-error";
  /**
   */
  private static final String S_SQL_ERROR = "sql-error";
  /**
   */
  private static final String S_MESSAGE = "message";
  /**
   */
  private static final String S_CODE = "code";

  /**
   */
  private static final String S_STATE = "state";

  /**
   */
  private static final String S_SQL_WARNING = "sql-warning";

  /**
   */
  private int m_ErrorExt_TypeID = DTM.NULL;
  /**
   */
  private int m_Message_TypeID = DTM.NULL;
  /**
   */
  private int m_Code_TypeID = DTM.NULL;

  /**
   */
  private int m_State_TypeID = DTM.NULL;

  /**
   */
  private int m_SQLWarning_TypeID = DTM.NULL;

  /**
   */
  private int m_SQLError_TypeID = DTM.NULL;

  /**
   */
  private int m_rootID = DTM.NULL;
  /**
   */
  private int m_extErrorID = DTM.NULL;
  /**
   */
  private int m_MainMessageID = DTM.NULL;

  /**
   * Build up an SQLErrorDocument that includes the basic error information
   * along with the Extended SQL Error information.
   * @param mgr
   * @param ident
   * @param error
   */
  public SQLErrorDocument( DTMManager mgr, int ident, SQLException error )
  {
    super(mgr, ident);

    createExpandedNameTable();
    buildBasicStructure(error);

    int sqlError = addElement(2, m_SQLError_TypeID, m_extErrorID, m_MainMessageID);
    int element = DTM.NULL;

    element = addElementWithData(
      new Integer(error.getErrorCode()), 3,
      m_Code_TypeID, sqlError, element);

    element = addElementWithData(
      error.getLocalizedMessage(), 3,
      m_Message_TypeID, sqlError, element);

//    this.dumpDTM();
  }


  /**
   * Build up an Error Exception with just the Standard Error Information
   * @param mgr
   * @param ident
   * @param error
   */
  public SQLErrorDocument( DTMManager mgr, int ident, Exception error )
  {
    super(mgr, ident);
    createExpandedNameTable();
    buildBasicStructure(error);
  }

  /**
   * Build up an Error Exception with just the Standard Error Information
   * @param mgr
   * @param ident
   * @param error
   */
  public SQLErrorDocument(DTMManager mgr, int ident, Exception error, SQLWarning warning, boolean full)
  {
    super(mgr, ident);
    createExpandedNameTable();
    buildBasicStructure(error);

	SQLException se = null;
	int prev = m_MainMessageID;
	boolean inWarnings = false;

	if ( error != null && error instanceof SQLException )
		se = (SQLException)error;
	else if ( full && warning != null )
	{
		se = warning;
		inWarnings = true;
	}

	while ( se != null )
	{
	    int sqlError = addElement(2, inWarnings ? m_SQLWarning_TypeID : m_SQLError_TypeID, m_extErrorID, prev);
		prev = sqlError;
    	int element = DTM.NULL;

	    element = addElementWithData(
	      new Integer(se.getErrorCode()), 3,
	      m_Code_TypeID, sqlError, element);

	    element = addElementWithData(
	      se.getLocalizedMessage(), 3,
	      m_Message_TypeID, sqlError, element);

		if ( full )
		{
			String state = se.getSQLState();
			if ( state != null && state.length() > 0 )
			    element = addElementWithData(
			      state, 3,
			      m_State_TypeID, sqlError, element);

			if ( inWarnings )
				se = ((SQLWarning)se).getNextWarning();
			else
				se = se.getNextException();
		}
		else
			se = null;
	}
  }

  /**
   * Build up the basic structure that is common for each error.
   * @param e
   * @return
   */
  private void buildBasicStructure( Exception e )
  {
    m_rootID = addElement(0, m_Document_TypeID, DTM.NULL, DTM.NULL);
    m_extErrorID = addElement(1, m_ErrorExt_TypeID, m_rootID, DTM.NULL);
    m_MainMessageID = addElementWithData
      (e != null ? e.getLocalizedMessage() : "SQLWarning", 2, m_Message_TypeID, m_extErrorID, DTM.NULL);
  }

  /**
   * Populate the Expanded Name Table with the Node that we will use.
   * Keep a reference of each of the types for access speed.
   * @return
   */
  protected void createExpandedNameTable( )
  {

    super.createExpandedNameTable();

    m_ErrorExt_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_EXT_ERROR, DTM.ELEMENT_NODE);

    m_SQLError_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_SQL_ERROR, DTM.ELEMENT_NODE);

    m_Message_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_MESSAGE, DTM.ELEMENT_NODE);

    m_Code_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_CODE, DTM.ELEMENT_NODE);

    m_State_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_STATE, DTM.ELEMENT_NODE);

    m_SQLWarning_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_SQL_WARNING, DTM.ELEMENT_NODE);
  }

}
