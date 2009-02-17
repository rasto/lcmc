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


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * An implementation of <code>Vertex</code> that resides in a 
 * sparse graph which may contain directed and/or undirected edges, 
 * as well as parallel edges.
 * <P>
 * This implementation stores hash tables that map the successors
 * of this vertex to its outgoing edges, and its predecessors to
 * its incoming edges.  This enables an efficient implementation of
 * <code>findEdge(Vertex)</code>, but causes the routines that
 * return the sets of neighbors and of incident edges to require
 * time proportional to the number of neighbors.
 *
 * @author Joshua O'Madadhain
 * @author Scott White
 * @author Danyel Fisher
 * 
 * @see SparseGraph
 */
public class SparseVertex extends SimpleSparseVertex 
{
    /**
     * Creates a new instance of a vertex for inclusion in a 
     * sparse graph.
     */
    public SparseVertex()
    {
		super();
	}

    /**
     * @see Vertex#getInEdges()
     */
    public Set getInEdges() 
    {
        Collection inEdgeSets = getPredsToInEdges().values();
        Collection edgeSets = getNeighborsToEdges().values();
        Set inEdges = new HashSet();
        
        for (Iterator i_iter = inEdgeSets.iterator(); i_iter.hasNext(); )
            inEdges.addAll((Set)i_iter.next());
            
        for (Iterator e_iter = edgeSets.iterator(); e_iter.hasNext(); )
            inEdges.addAll((Set)e_iter.next());    
            
        return Collections.unmodifiableSet(inEdges);
    }

    /**
     * @see Vertex#getOutEdges()
     */
    public Set getOutEdges() {
        Collection outEdgeSets = getSuccsToOutEdges().values();
        Collection edgeSets = getNeighborsToEdges().values();
        Set outEdges = new HashSet();
        
        for (Iterator o_iter = outEdgeSets.iterator(); o_iter.hasNext(); )
            outEdges.addAll((Set)o_iter.next());

        for (Iterator e_iter = edgeSets.iterator(); e_iter.hasNext(); )
            outEdges.addAll((Set)e_iter.next());    
            
        return Collections.unmodifiableSet(outEdges);
    }


    /**
     * Returns the edge that connects this
     * vertex to the specified vertex <code>v</code>, or
     * <code>null</code> if there is no such edge.
     * Implemented using a hash table for a performance
     * improvement over the implementation in 
     * <code>AbstractSparseVertex</code>.
     * 
     * Looks for a directed edge first, and then for an
     * undirected edge if no directed edges are found.
     * 
     * @see Vertex#findEdge(Vertex)
     */
    public Edge findEdge(Vertex v)
    {
        Set outEdges = (Set)getSuccsToOutEdges().get(v);
        if (outEdges == null)
            outEdges = (Set)getNeighborsToEdges().get(v);
        if (outEdges == null)
            return null;
        return (Edge)outEdges.iterator().next();
    }

    /**
     * @see Vertex#findEdgeSet(Vertex)
     */
    public Set findEdgeSet(Vertex v)
    {
        // v. 1.4: no longer using CollectionUtils.union to combine these sets, 
        // since this method does not accept null arguments
        Set edgeSet = new HashSet();
        Set outEdges = (Set)getSuccsToOutEdges().get(v);
        Set edges = (Set)getNeighborsToEdges().get(v);
        if (outEdges != null)
            edgeSet.addAll(outEdges);
        if (edges != null)
            edgeSet.addAll(edges);
        return Collections.unmodifiableSet(edgeSet);
    }

    /**
     * Returns a list of all incident edges of this vertex.
     * Requires time proportional to the number of incident edges.
     *  
     * @see AbstractSparseVertex#getEdges_internal()
     */
    protected Collection getEdges_internal() {
        HashSet edges = new HashSet();

        Collection inEdgeSets = getPredsToInEdges().values();
        Collection outEdgeSets = getSuccsToOutEdges().values();
        Collection edgeSets = getNeighborsToEdges().values();
        
        for (Iterator e_iter = inEdgeSets.iterator(); e_iter.hasNext(); )
            edges.addAll((Set)e_iter.next());

        for (Iterator e_iter = outEdgeSets.iterator(); e_iter.hasNext(); )
            edges.addAll((Set)e_iter.next());

        for (Iterator e_iter = edgeSets.iterator(); e_iter.hasNext(); )
            edges.addAll((Set)e_iter.next());
        
        return edges;
    }

    /**
     * @see AbstractSparseVertex#addNeighbor_internal(Edge, Vertex)
     */
    protected void addNeighbor_internal(Edge e, Vertex v)
    {
        if (e instanceof DirectedEdge)
        {
            DirectedEdge de = (DirectedEdge) e;
            boolean added = false;
            if (this == de.getSource())
            {
                Map stoe = getSuccsToOutEdges();
                Set outEdges = (Set)stoe.get(v);
                if (outEdges == null)
                {
                    outEdges = new HashSet();
                    stoe.put(v, outEdges);
                }
                outEdges.add(de);
                added = true;
            }
            if (this == de.getDest())
            {
                Map ptie = getPredsToInEdges();
                Set inEdges = (Set)ptie.get(v);
                if (inEdges == null)
                {
                    inEdges = new HashSet();
                    ptie.put(v, inEdges);
                }
                inEdges.add(de);
                added = true;
            }
            if (!added)
                throw new IllegalArgumentException("Internal error: " + 
                    "this vertex is not incident to " + e);
        }
        else if (e instanceof UndirectedEdge)
        {   
            Map nte = getNeighborsToEdges();
            Set edges = (Set)nte.get(v);

            if (edges == null)
            {
                edges = new HashSet();
                nte.put(v, edges);
            }
            edges.add(e);
        }
        else throw new IllegalArgumentException("Edge is neither directed" +
            "nor undirected");
    }

    /**
     * @see AbstractSparseVertex#removeNeighbor_internal(Edge, Vertex)
     */
	protected void removeNeighbor_internal(Edge e, Vertex v)
    {
        boolean predecessor = false;
        boolean successor = false;

        if (e instanceof DirectedEdge)
        {
            Map ptie = getPredsToInEdges();
            Set inEdges = (Set)ptie.get(v);
            Map stoe = getSuccsToOutEdges();
            Set outEdges = (Set)stoe.get(v);
            DirectedEdge de = (DirectedEdge)e;

            if (de.getSource() == v && inEdges != null) 
            {   // -> v is predecessor and not yet removed
                predecessor = inEdges.remove(e);
                if (inEdges.isEmpty()) // remove entry if it's now obsolete
                    ptie.remove(v);
            }
            if (de.getDest() == v && outEdges != null) 
            {   // -> v is successor and not yet removed
                successor = outEdges.remove(e);
                if (outEdges.isEmpty()) // remove entry if it's now obsolete
                    stoe.remove(v);
            }
            if (!predecessor && !successor && !(this == v))
                throw new FatalException("Internal error in data structure" +
                    " for vertex " + this);                
        }
        else if (e instanceof UndirectedEdge)
        {
            // if v doesn't point to e, and it's not a self-loop
            // that's been removed in a previous call to removeNeighbor...
            Map nte = getNeighborsToEdges();
            Set edges = (Set)nte.get(v);
            if (edges != null)
            {
                boolean removed = edges.remove(e);
                if (edges.isEmpty())
                    nte.remove(v);
                if (!removed && this != v)
                    throw new FatalException("Internal error in data structure" +
                        "for vertex " + this);
            }
            else if (this != v)
                throw new FatalException("Internal error in data structure" +
                    "for vertex " + this);

            // if it *is* a self-loop, we're already done
        }
        
        else
            throw new FatalException("Edge is neither directed nor undirected");
	}

}