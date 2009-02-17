/* 
 * Created on Dec 11, 2003
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

import java.util.Iterator;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * A support class for both HyperedgeBPG and HyperVertexBPG,
 * this represents a single object backed by a BipartiteVertex
 * that is a member of a HypergraphBPG. Cannot be instantiated
 * on its own.
 * 
 * @author danyelf
 * @deprecated As of version 1.7, JUNG now includes native versions of hypergraph classes.
 */
public abstract class AbstractHyperUnitBPG implements UserDataContainer {

	protected BipartiteVertex vertex;
	protected HypergraphBPG graph;

	public AbstractHyperUnitBPG() {
		this.vertex = new BipartiteVertex();		
	}

	/** for constructing a new HypergraphBPG based on a previous HypergraphBPG */
	AbstractHyperUnitBPG( BipartiteVertex bpg, HypergraphBPG hypergraphBPG ) {
		this.vertex = bpg;
		this.graph = hypergraphBPG;
	}
	
	protected BipartiteVertex underlying_vertex() {
		return vertex;
	}

	public boolean equals( Object o ) {
		if ( o instanceof AbstractHyperUnitBPG ) {
			AbstractHyperUnitBPG hu = (AbstractHyperUnitBPG) o;
			return (vertex.equals( hu.underlying_vertex()));			
		} else if ( o instanceof Vertex ) {
			return (vertex.equals(o));
		} else 
			return false;
	}
	
	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#getGraph()
	 */
	public ArchetypeGraph getGraph() {
		return graph;
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#addUserDatum(java.lang.Object, java.lang.Object, edu.uci.ics.jung.utils.UserDataContainer.CopyAction)
	 */
	public void addUserDatum(Object key, Object datum, CopyAction copyAct) {
		vertex.addUserDatum(key, datum, copyAct);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#importUserData(edu.uci.ics.jung.utils.UserDataContainer)
	 */
	public void importUserData(UserDataContainer udc) {
		vertex.importUserData(udc);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#getUserDatumKeyIterator()
	 */
	public Iterator getUserDatumKeyIterator() {
		return vertex.getUserDatumKeyIterator();
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#getUserDatumCopyAction(java.lang.Object)
	 */
	public CopyAction getUserDatumCopyAction(Object key) {
		return vertex.getUserDatumCopyAction(key);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#getUserDatum(java.lang.Object)
	 */
	public Object getUserDatum(Object key) {
		return vertex.getUserDatum(key);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#setUserDatum(java.lang.Object, java.lang.Object, edu.uci.ics.jung.utils.UserDataContainer.CopyAction)
	 */
	public void setUserDatum(Object key, Object datum, CopyAction copyAct) {
		vertex.setUserDatum(key, datum, copyAct);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#removeUserDatum(java.lang.Object)
	 */
	public Object removeUserDatum(Object key) {
		return vertex.removeUserDatum(key);
	}

    public boolean containsUserDatumKey(Object key)
    {
        return vertex.containsUserDatumKey(key);
    }
    
	/**
	 * @param hypergraphBPG
	 */
	protected void setGraph(HypergraphBPG hypergraphBPG) {
		this.graph = hypergraphBPG;		
	}

	public void removeVertex(HypervertexBPG hv3) {
		Edge e = vertex.findEdge(hv3.underlying_vertex());
	
		BipartiteGraph bpg = (BipartiteGraph) hv3.underlying_vertex().getGraph();
		bpg.removeEdge( e );			
	}

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
