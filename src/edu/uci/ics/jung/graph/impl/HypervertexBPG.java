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
 * Created on Dec 11, 2003
 */
package edu.uci.ics.jung.graph.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.*;

/**
 * A Hypervertex has zero or more hyperEdges attached to it, and
 * is a member of a Hypergraph. 
 * 
 * @author danyelf
 */
public class HypervertexBPG extends AbstractHyperUnitBPG implements Hypervertex {

	public HypervertexBPG() {
	}
	
	HypervertexBPG( BipartiteVertex bpv, HypergraphBPG hypergraphBPG ) {
		super( bpv, hypergraphBPG );
	}
	
    public Set getIncidentElements()
    {
        return getIncidentEdges();
    }
	
	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#getNeighbors()
	 */
	public Set getNeighbors() {
		// ask the underlying vertex for the two-step-away neighbors
		// and then get the corresponding HyperVertex
		Set oldNeighbors = vertex.getNeighbors();

		Set realNeighbors = new HashSet();
		for (Iterator iter = oldNeighbors.iterator(); iter.hasNext();) {
			Vertex v = (Vertex) iter.next();
			realNeighbors.addAll( v.getNeighbors() );
		}

		realNeighbors.remove( vertex );
		return graph.translateUnderlyingVertices( realNeighbors );
	}


	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#getIncidentEdges()
	 */
	public Set getIncidentEdges() {
		return graph.translateUnderlyingEdges( vertex.getNeighbors() );
	}

	/**
	 * Returns the number of edges adjacent to this vertex 
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#degree()
	 */
	public int degree() {
		return vertex.degree();
	}


	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#getEqualVertex(edu.uci.ics.jung.graph.ArchetypeGraph)
	 */
	public ArchetypeVertex getEqualVertex(ArchetypeGraph g) {
		HypergraphBPG bpg = (HypergraphBPG) g;
		// check if that graph's underlying vertex is the same as this one's.
		return bpg.getVertexCorrespondingTo( underlying_vertex() );
	}

    /**
     * @deprecated As of version 1.4, renamed to getEqualVertex(g).
     */
    public ArchetypeVertex getEquivalentVertex(ArchetypeGraph g)
    {
        return getEqualVertex(g);
    }

	/**
	 * Not a very efficient implementation
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#isNeighborOf(edu.uci.ics.jung.graph.ArchetypeVertex)
	 */
	public boolean isNeighborOf(ArchetypeVertex v) {
		return getNeighbors().contains(v);
	}


	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#isIncident(edu.uci.ics.jung.graph.ArchetypeEdge)
	 */
	public boolean isIncident(ArchetypeEdge e) {
		HyperedgeBPG hepbg = (HyperedgeBPG) e;
		return underlying_vertex().isNeighborOf(hepbg.underlying_vertex());
	}

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeVertex#findEdge(ArchetypeVertex)
     */
	public ArchetypeEdge findEdge(ArchetypeVertex v)
    {
        Set incident_edges = getIncidentEdges();
        
        for (Iterator iter = incident_edges.iterator(); iter.hasNext(); )
        {
            ArchetypeEdge e = (ArchetypeEdge)iter.next(); 
            if (e.isIncident(v))
                return e;
        }
        return null;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.ArchetypeVertex#findEdgeSet(ArchetypeVertex)
     */
    public Set findEdgeSet(ArchetypeVertex v)
    {
        Set incident_edges = getIncidentEdges();
        
        Set connecting_edges = new HashSet();
        for (Iterator iter = incident_edges.iterator(); iter.hasNext(); )
        {
            ArchetypeEdge e = (ArchetypeEdge)iter.next();
            if (e.isIncident(v))
                connecting_edges.add(e);
        }
        return connecting_edges;
    }

	/**
	 * Not a very efficient implemenation: for each edge, counts the
	 * neighbors.
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#numNeighbors()
	 */
	public int numNeighbors() {
		return getNeighbors().size();
	}


	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#copy(edu.uci.ics.jung.graph.ArchetypeGraph)
	 */
	public ArchetypeVertex copy(ArchetypeGraph g) {
		HypergraphBPG hg = (HypergraphBPG) g;
		HypervertexBPG hv = new HypervertexBPG();
		hg.addVertex( hv );
		hv.importUserData(this);
		return hv;
	}

    public boolean connectEdge(Hyperedge e)
    {
        return e.connectVertex(this);
    }
    
    public boolean disconnectEdge(Hyperedge e)
    {
        return e.disconnectVertex(this);
    }

}
