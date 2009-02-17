/*
 * Created on Mar 29, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.impl;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.OnePredicate;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.KPartiteGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.predicates.KPartiteEdgePredicate;
import edu.uci.ics.jung.utils.SubsetManager;


/**
 * An implementation of KPartiteGraph based on SparseGraph.
 * This implementation optionally creates a subset for each partition
 * specified in the constructor.
 * 
 * <p>Vertex constraints imposed by this class: predicates in 
 * <code>partitions</code> constructor argument</p>
 * <p>Edge constraints imposed by this class:
 * <code>KPartiteEdgePredicate(partitions)</code></p>
 * 
 * @author Joshua O'Madadhain
 */
public class KPartiteSparseGraph extends SparseGraph implements KPartiteGraph
{
    protected Collection partitions;

    /**
     * Creates a KPartiteSparseGraph whose partitions are specified by
     * the predicates in the <code>partitions</code> array.  If the 
     * <code>subsets</code> argument is true, creates a subset for
     * each partition.
     */
    public KPartiteSparseGraph(Collection partitions, boolean subsets)
    {
        super();
        if (partitions.size() < 2)
            throw new IllegalArgumentException("Constructor must " +
                  "specify >= 2 vertex partition predicates");
        this.partitions = partitions;
        
        // each vertex added must satisfy exactly 1 partition-specific predicate
        getVertexConstraints().add(OnePredicate.getInstance(partitions));
        
        // each edge added must connect vertices in distinct partitions
//        user_edge_requirements.add(new KPartiteEdgePredicate(partitions));
        getEdgeConstraints().add(new KPartiteEdgePredicate(partitions));
        
        // create a subset for each vertex predicate
        if (subsets)
        {
            SubsetManager sm = SubsetManager.getInstance(this);
            for (Iterator p_iter = partitions.iterator(); p_iter.hasNext(); )
                sm.addVertexSubset((Predicate)p_iter.next());
        }
    }
    
    /**
     * <p>
     * Creates a new <code>KPartiteSparseGraph</code> which contains all the
     * vertices and edges in <code>g</code>. The new graph contains all the
     * user data from the original graph and its components.
     * </p>
     * <p>
     * This method performs no tagging or structural conversion. If
     * <code>g</code> is not compatible with the constraints specified by
     * <code>partitions</code>, this constructor will throw an
     * <code>IllegalArgumentException</code>. Thus, each vertex in
     * <code>g</code> must be a member of exactly one partition, and each edge
     * must join vertices in distinct partitions.
     * </p>
     */
    public KPartiteSparseGraph(Graph g, Collection partitions, boolean subsets)
    {
        this(partitions, subsets);
        
        addAllNotInitializers(getEdgeConstraints(), g.getEdgeConstraints());
        addAllNotInitializers(getVertexConstraints(), g.getVertexConstraints());
        for (Iterator iter = g.getVertices().iterator(); iter.hasNext();) 
        {
            Vertex av = (Vertex) iter.next();
            av.copy(this);
        }
        
        for (Iterator iter = g.getEdges().iterator(); iter.hasNext();) 
        {
            Edge ae = (Edge) iter.next();
            ae.copy(this);
        }
        
        this.importUserData(g);
    }
    
    public Collection getPartitions()
    {
        return partitions;
    }
}
