/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on May 9, 2004
 */
package edu.uci.ics.jung.graph.predicates;

import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.impl.SparseTree;


/**
 * @author danyelf
 */
public class TreePredicate extends EdgePredicate {

    /**
     * Any edges added to this graph must go to a vertex where
     * 
     * @see edu.uci.ics.jung.graph.predicates.EdgePredicate#evaluateEdge(edu.uci.ics.jung.graph.ArchetypeEdge)
     */
    public boolean evaluateEdge(ArchetypeEdge e) {
        if (! Graph.DIRECTED_EDGE.evaluate(e) ) 
            return false;
        DirectedEdge de = (DirectedEdge) e;
        Vertex dest = de.getDest();
        if( dest.containsUserDatumKey(SparseTree.SPARSE_ROOT_KEY))
            return false;
        if ( dest.inDegree () > 0)
            return false;

        Vertex src = de.getSource();
        if( !src.containsUserDatumKey(SparseTree.IN_TREE_KEY))
            return false;
        return true;        
    }

    protected static TreePredicate instance = null;
    
    /**
     * @return a <code>TreePredicate</code> instance
     */
    public static TreePredicate getInstance() {
        if ( instance == null) 
            instance = new TreePredicate();
        return instance;
    }

}
