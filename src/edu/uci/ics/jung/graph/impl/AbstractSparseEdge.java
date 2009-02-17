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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * This class provides a skeletal implementation of the <code>Edge</code>
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
 * @author Joshua O'Madadhain
 * @author Danyel Fisher
 * @author Scott White
 *
 * @see AbstractSparseGraph
 * @see AbstractSparseVertex
 */
public abstract class AbstractSparseEdge extends AbstractArchetypeEdge implements Edge
{
    /**
     * One of the two incident vertices of this edge.  If this edge is
     * directed, this is its source.
     */
	protected Vertex mFrom;

    /**
     * One of the two incident vertices of this edge.  If this edge is
     * directed, this is its destination.
     */
	protected Vertex mTo;

	/**
     * The next edge ID.
     */
    private static int nextGlobalEdgeID = 0;


	/**
     * Creates an edge connecting vertices <code>from</code> and
     * <code>to</code>.  The order of the arguments is significant for
     * implementations of
     * <code>DirectedEdge</code> which extend this class, and is not
     * significant for implementations of <code>UndirectedEdge</code>
     * which extend this class.
     * <P>
     * Disallows the following:
     * <ul>
     * <li>null arguments
     * <li>arguments which are not elements of the same graph
     * <li>arguments which are not elements of any graph (orphaned vertices)
     * <li>parallel edges (> 1 edge connecting vertex <code>from<code> to
     *     vertex <code>to</code>)
     * </ul>
     * Any of these will cause an <code>IllegalArgumentException</code> to
     * be thrown.
     *
	 * @param from one incident vertex (if edge is directed, the source)
	 * @param to   the other incident vertex (if edge is directed, the destination)
     * @throws IllegalArgumentException
	 */
	public AbstractSparseEdge(Vertex from, Vertex to)
    {
        super();
        if (from == null || to == null)
           throw new IllegalArgumentException("Vertices passed in can not be null");

    	if( from.getGraph() != to.getGraph())
    		throw new IllegalArgumentException("Vertices must be from same graph");

        if (from.getGraph() == null || to.getGraph() == null)
            throw new IllegalArgumentException("Orphaned vertices can not " +
                "be connected by an edge");

		mFrom = from;
		mTo = to;
        
        this.id = nextGlobalEdgeID++;
    }


    /**
     * Returns a human-readable representation of this edge.
     *
	 * @see java.lang.Object#toString()
	 */
	public String toString() 
    {
    	    return "E" +  String.valueOf(id) + "(" + mFrom.toString() + "," + mTo.toString() + ")";
    }

	/**
     * Attaches this edge to the specified graph, and invokes the methods that
     * add this edge to the incident vertices' data structures.
     * <P>
     * <b>Note</b>: this method will not properly update the incident
     * vertices' data structures unless the vertices are instances of
     * <code>AbstractSparseVertex</code>.
     *
	 * @param graph    the graph to which this edge is being added
	 */
	void addGraph_internal(AbstractSparseGraph graph) {
        
        // we already checked for null in the constructor, so we don't need to check here
		if (graph != mFrom.getGraph() )
			throw new IllegalArgumentException("graph to which edge " +
                "is being added does not match graph of incident vertices " +
                mFrom + " " + mTo );

        super.addGraph_internal(graph);

        // In theory, we could do this in the constructor.  The problem with 
        // doing so is that we'd have to figure out a way for neighbors, etc.
        // to be removed automatically via garbage collection.  (Which we
        // could probably do with WeakReferences, but that puts the burden on the
        // vertex implementor to cope.)
		if (mFrom instanceof AbstractSparseVertex)
			((AbstractSparseVertex)mFrom).addNeighbor_internal( this, mTo );

		if (mTo instanceof AbstractSparseVertex)
			((AbstractSparseVertex)mTo).addNeighbor_internal( this, mFrom );
	}

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#getIncidentVertices()
     */
	public Set getIncidentVertices() {
        Set vertices = new LinkedHashSet(2);
		vertices.add(mFrom);
		vertices.add(mTo);

        return Collections.unmodifiableSet(vertices);
	}

    /**
     * @see edu.uci.ics.jung.graph.Edge#getOpposite(Vertex)
     */
    public Vertex getOpposite(Vertex vertex) {
        if (vertex == mFrom)
            return mTo;
        if (vertex == mTo)
            return mFrom;

        throw new IllegalArgumentException("Vertex " + vertex +
            " is not incident to this edge " + this );
    }




    /**
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#numVertices()
     */
    public int numVertices() {
        return 2;
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#isIncident(ArchetypeVertex)
     */
    public boolean isIncident(ArchetypeVertex v) {
        return (v == mFrom) || (v == mTo);
    }


	/**
     * Creates a copy of this edge in the specified graph <code>newGraph</code>,
     * and copies this edge's user data to the new edge.
     *
	 * @see edu.uci.ics.jung.graph.ArchetypeEdge#copy(edu.uci.ics.jung.graph.ArchetypeGraph)
	 */
    public ArchetypeEdge copy(ArchetypeGraph newGraph)
    {
		AbstractSparseEdge e = (AbstractSparseEdge)super.copy(newGraph);
        e.mFrom = (Vertex)mFrom.getEqualVertex(newGraph);
        e.mTo = (Vertex)mTo.getEqualVertex(newGraph);
        ((Graph)newGraph).addEdge(e);
        return e;
    }


	/**
	 * @see edu.uci.ics.jung.graph.Edge#getEndpoints()
	 */
	public Pair getEndpoints() {
		return new Pair( mFrom, mTo );
	}

}
