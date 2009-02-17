/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph.impl;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.predicates.TreePredicate;
import edu.uci.ics.jung.utils.UserData;

/**
 * An implementation of <code>Graph</code> that consists of a 
 * <code>Vertex</code> set and a <code>DirectedEdge</code> set.
 * Further, a vertex can have no more than one incoming directed
 * edge (enforced with <code>TreePredicate</code>); the tree must
 * define a root vertex at construction time.
 * This implementation does NOT ALLOW parallel edges. 
 * <code>SimpleDirectedSparseVertex</code> is the most efficient
 * vertex for this graph type.
 *
 * <p>Edge constraints imposed by this class: DIRECTED_EDGE, 
 * <code>TreePredicate</code>, NOT_PARALLEL_EDGE
 * 
 * <p>For additional system and user constraints defined for
 * this class, see the superclasses of this class.</p>
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * 
 * @see DirectedSparseVertex
 * @see DirectedSparseEdge
 */
public class SparseTree extends SparseGraph
	implements DirectedGraph {

    protected Vertex mRoot;
    public static final Object SPARSE_ROOT_KEY = "edu.uci.ics.jung.graph.impl.SparseTree.RootKey";
	public static final Object IN_TREE_KEY = "edu.uci.ics.jung.graph.impl.SparseTree.InTreeKey";

    /**
     * @param root
     */
    public SparseTree(Vertex root) {
        edge_requirements.add(TreePredicate.getInstance());
        edge_requirements.add(DIRECTED_EDGE);
        edge_requirements.add(NOT_PARALLEL_EDGE);

        this.mRoot = root;
        addVertex( root );
        mRoot.setUserDatum(SPARSE_ROOT_KEY, SPARSE_ROOT_KEY, UserData.SHARED);
        mRoot.setUserDatum(IN_TREE_KEY, IN_TREE_KEY, UserData.SHARED);
    }

    /**
     * @return the root of this tree
     */
    public Vertex getRoot() {
        return mRoot;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.Graph#addEdge(edu.uci.ics.jung.graph.Edge)
     */
    public Edge addEdge(Edge e) {
        Edge rv = super.addEdge(e);
        Vertex dest = (Vertex) rv.getEndpoints().getSecond();
        dest.setUserDatum(IN_TREE_KEY, IN_TREE_KEY, UserData.SHARED);        
        return rv;
    }
}
