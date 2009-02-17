/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms;

import edu.uci.ics.jung.algorithms.MatrixElementOperations;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.UserData;

/**
 * Implements the basic matrix operations on double-precision values.  Assumes
 * that the edges have a MutableDouble value.
 * 
 * @author Joshua O'Madadhain
 */
public class RealMatrixElementOperations implements MatrixElementOperations 
{
    private String EDGE_KEY;

    public RealMatrixElementOperations(String edge_key)
    {
        EDGE_KEY = edge_key;
    }

	/**
	 * @see MatrixElementOperations#mergePaths(Edge, Object)
	 */
	public void mergePaths(Edge e, Object pathData) 
    {
        MutableDouble pd = (MutableDouble)pathData;
        MutableDouble ed = (MutableDouble)e.getUserDatum(EDGE_KEY);
        if (ed == null)
            e.addUserDatum(EDGE_KEY, pd, UserData.SHARED);
        else
            ed.add(pd.doubleValue());
	}

	/**
	 * @see MatrixElementOperations#computePathData(Edge, Edge)
	 */
	public Object computePathData(Edge e1, Edge e2) 
    {
        double d1 = ((MutableDouble)e1.getUserDatum(EDGE_KEY)).doubleValue();
        double d2 = ((MutableDouble)e2.getUserDatum(EDGE_KEY)).doubleValue();
        return new MutableDouble(d1*d2);
	}
}
