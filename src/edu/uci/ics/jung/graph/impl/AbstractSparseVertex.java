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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * This class provides a skeletal implementation of the <code>Vertex</code>
 * interface to minimize the effort required to implement this interface.
 * It is appropriate for sparse graphs (those in which each vertex
 * is connected to only a few other vertices); for dense graphs (those in
 * which each vertex is connected to most other vertices), another
 * implementation might be more appropriate.
 * <P>
 * This class extends <code>UserData</code>, which provides storage and
 * retrieval mechanisms for user-defined data for each edge instance.
 * This allows users to attach data to edges without having to extend
 * this class.
 *
 * @author Scott White
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 *
 * @see AbstractSparseGraph
 * @see AbstractSparseEdge
 */
public abstract class AbstractSparseVertex extends AbstractArchetypeVertex
    implements Vertex, Cloneable
{
    /**
     * The next vertex ID.
     */
	private static int nextGlobalVertexID = 0;

    /**
     * Creates a new instance of a vertex for inclusion
     * in a sparse graph.
     * Sets up the data necessary for definition of
     * vertex equivalence, and initializes the internal
     * data structures.
     *
     * @see #initialize()
     */
	protected AbstractSparseVertex() {
        super();
		this.id = nextGlobalVertexID++;		
		initialize();
	}

    

	/**
     * Returns the edge that connects this vertex to the specified
     * vertex <code>v</code>.  This is a
	 * simple implementation which checks the opposite vertex of
	 * each outgoing edge of this vertex; this solution is general,
     * but not efficient.
     *
	 * @see Vertex#findEdge(Vertex)
	 */
	public Edge findEdge(Vertex v) 
    {
		for (Iterator iter = getOutEdges().iterator(); iter.hasNext();) {
			Edge element = (Edge) iter.next();
			if (element.getOpposite(this).equals(v))
				return element;
		}
		return null;
	}

    public ArchetypeEdge findEdge(ArchetypeVertex v)
    {
        return this.findEdge((Vertex)v);
    }
    
    /**
     * @see Vertex#findEdgeSet(Vertex)
     */
	public Set findEdgeSet(Vertex v) 
    {
	    Set edgeSet = new HashSet();
		for (Iterator iter = getOutEdges().iterator(); iter.hasNext();) {
			Edge element = (Edge) iter.next();
			if (element.getOpposite(this).equals(v))
			    edgeSet.add( element );
		}
		return edgeSet;
	}
    
    /**
     * @see ArchetypeVertex#findEdgeSet(ArchetypeVertex)
     */
    public Set findEdgeSet(ArchetypeVertex v)
    {
        return this.findEdgeSet((Vertex)v);
    }
    
	/**
	 * @see Vertex#copy(ArchetypeGraph)
	 */
	public ArchetypeVertex copy(ArchetypeGraph newGraph) 
    {
        AbstractSparseVertex v = (AbstractSparseVertex)super.copy(newGraph);
        ((Graph)newGraph).addVertex(v);
        return v;
	}


	/**
     * Adds the specified edge <code>e</code> and vertex <code>v</code>
     * to the internal data structures of this vertex.
     *
	 * @param e    the new incident edge of this vertex
	 * @param v    the new neighbor of this vertex
	 */
	protected abstract void addNeighbor_internal(Edge e, Vertex v);

	/**
	 * Removes the specified edge <code>e</code> and vertex <code>v</code>
     * from the internal data structures of this vertex.
	 *
	 * @param e    the incident edge of this vertex which is being removed
	 * @param v    the neighbor of this vertex which is being removed
	 */
	protected abstract void removeNeighbor_internal(Edge e, Vertex v);


    /**
     * Returns a human-readable representation of this vertex.
     *
     * @see java.lang.Object#toString()
     */
	public String toString() {
		return "V" + String.valueOf(id);
	}

}
