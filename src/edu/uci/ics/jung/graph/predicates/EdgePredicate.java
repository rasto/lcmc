/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
* 
* Created on Mar 5, 2004
*/
package edu.uci.ics.jung.graph.predicates;

import edu.uci.ics.jung.graph.ArchetypeEdge;

/**
 * 
 * @author Joshua O'Madadhain
 */
public abstract class EdgePredicate extends GPredicate
//implements Predicate
{
    /**
     * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
     */
    public final boolean evaluate(Object arg0)
    {
        return evaluateEdge((ArchetypeEdge)arg0);
    }

    public abstract boolean evaluateEdge(ArchetypeEdge e);
}
