/*
 * Created on Apr 26, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.impl;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Hyperedge;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.Hypervertex;

/**
 * This class provides a skeletal implementation of the <code>Hypervertex</code>
 * interface to minimize the effort required to implement this interface.
 * <P>
 * This class extends <code>UserData</code>, which provides storage and
 * retrieval mechanisms for user-defined data for each edge instance.
 * This allows users to attach data to edges without having to extend
 * this class.</p>
 * <p>
 * Existing subclasses maintain collections of edges, and infer neighbor
 * collections from these classes.  Independent neighbor collections are
 * difficult to maintain for the following reasons: 
 * <ol>
 * <li/>Hyperedges' membership is mutable; when a vertex is added to 
 * or removed from a hyperedge, each
 * vertex connected to the hyperedge must be notified of the change, and
 * when a hyperedge is added to or removed from a graph, each connected
 * vertex must remove all vertices incident to this edge from its 
 * collections...but only if there are no other edges connecting each 
 * vertex pair.
 * <li/>The number of "neighboring" vertices for a
 * hypergraph can be very large if it is connected to several hyperedges.
 * Those who want to provide implementations which maintain something 
 * like the adjacency maps found in the <code>Vertex</code> implementations 
 * will need to provide support for edge set mutability both in the
 * <code>Hypervertex</code> implementation and in the <code>Hyperedge</code>
 * implementation.
 * </ol>
 * </p>
 *
 * @author Joshua O'Madadhain
 *
 * @see SetHypergraph
 * @see AbstractHyperedge
 * 
 * 
 * 
 * @author Joshua O'Madadhain
 */
public abstract class AbstractHypervertex extends AbstractArchetypeVertex implements Hypervertex
{
    /**
     * The next vertex ID.
     */
    private static int nextGlobalVertexID = 0;
    
    public AbstractHypervertex()
    {
        super();
        this.id = nextGlobalVertexID++;
        initialize();
    }
    
    /**
     * @see edu.uci.ics.jung.graph.ArchetypeVertex#copy(edu.uci.ics.jung.graph.ArchetypeGraph)
     */
    public ArchetypeVertex copy(ArchetypeGraph g)
    {
        AbstractHypervertex v = (AbstractHypervertex)super.copy(g);
        ((Hypergraph)g).addVertex(v);
        return v;
    }
    
    protected void initialize()
    {
        super.initialize();
    }
    
    /**
     * Returns a human-readable representation of this edge.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() 
    {
        return "HV" + id;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.Hypervertex#connectEdge(Hyperedge)
     */
    public boolean connectEdge(Hyperedge e)
    {
        return e.connectVertex(this);
    }
    
    /**
     * @see edu.uci.ics.jung.graph.Hypervertex#disconnectEdge(Hyperedge)
     */
    public boolean disconnectEdge(Hyperedge e)
    {
        return e.disconnectVertex(this);
    }
}
