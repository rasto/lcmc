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

import java.util.Set;

import edu.uci.ics.jung.graph.*;

/**
 * A HyperEdge has zero or more HyperVertices attached to it;
 * this implements that as part of an underlying HyperGraph. 
 * 
 * @author danyelf
 * @deprecated As of version 1.7, replaced by native implementations of <code>Hyperedge</code>.
 * @see SetHyperedge
 * @see ListHyperedge
 */
public class HyperedgeBPG extends AbstractHyperUnitBPG implements Hyperedge{

	public HyperedgeBPG() {
	}

	HyperedgeBPG( BipartiteVertex bpv, HypergraphBPG hypergraphBPG ) {
		super( bpv, hypergraphBPG );
	}

    public Set getIncidentElements()
    {
        return getIncidentVertices();
    }

	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeEdge#getIncidentVertices()
	 */
	public Set getIncidentVertices() {
		return graph.translateUnderlyingVertices(vertex.getNeighbors());
	}

	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeEdge#getEqualEdge(edu.uci.ics.jung.graph.ArchetypeGraph)
	 */
	public ArchetypeEdge getEqualEdge(ArchetypeGraph g) {
		HypergraphBPG bpg = (HypergraphBPG) g;
		// check if that graph's underlying vertex is the same as this one's.
		return bpg.getEdgeCorrespondingTo( underlying_vertex() );
	}

    /**
     * @deprecated As of version 1.4, renamed to getEqualEdge(g).
     */
    public ArchetypeEdge getEquivalentEdge(ArchetypeGraph g)
    {
        return getEqualEdge(g);
    }
    
	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeEdge#numVertices()
	 */
	public int numVertices() {
		return vertex.degree();
	}

	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeEdge#isIncident(edu.uci.ics.jung.graph.ArchetypeVertex)
	 */
	public boolean isIncident(ArchetypeVertex v) {
		HypervertexBPG hv = (HypervertexBPG) v;
		return vertex.isNeighborOf(hv.underlying_vertex());
	}

	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeEdge#copy(edu.uci.ics.jung.graph.ArchetypeGraph)
	 */
	public ArchetypeEdge copy(ArchetypeGraph g) {
		HypergraphBPG hg = (HypergraphBPG) g;
		HyperedgeBPG he = new HyperedgeBPG();
		hg.addEdge( he );
		he.importUserData(this);
		return he;
	}

	/**
	 * Registers an additional vertex <code>hv3_x</code> onto this Edge.
	 */
	public boolean connectVertex(Hypervertex hv3_x) 
    {
		HypervertexBPG hv3 = (HypervertexBPG) hv3_x;
		BipartiteGraph bpg = (BipartiteGraph) hv3.underlying_vertex().getGraph();
		BipartiteVertex v1 = hv3.underlying_vertex();
		BipartiteVertex v2 = underlying_vertex();
        
        if (v1.isNeighborOf(v2))
            return false;
        
		bpg.addBipartiteEdge(new BipartiteEdge(v1, v2));		
        
        return true;
	}
    
    public boolean disconnectVertex(Hypervertex v)
    {
        HypervertexBPG hv3 = (HypervertexBPG)v;
        BipartiteGraph bpg = (BipartiteGraph) hv3.underlying_vertex().getGraph();
        BipartiteVertex v1 = hv3.underlying_vertex();
        BipartiteVertex v2 = underlying_vertex();
        Edge e = v1.findEdge(v2);
        if (e != null)
        {
            bpg.removeEdge(e);        
            return true;
        }
        return false;
    }

}
