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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Hyperedge;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.Hypervertex;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * Implements a hypergraph built over an underlying
 * Bipartite graph, using the equivalence explained in 
 * the FAQ. Fully implements the Hypergraph interface;
 * its vertices and edges fully implement their interfaces.
 * 
 * Use and create in the standard way; the underlying graph
 * is invisible to the user (but can be extracted
 * with a call to getBipartiteGraphEquivalent() ).
 * 
 * @author danyelf
 * @deprecated As of version 1.7, replaced by <code>SetHypergraph</code>.
 * @see SetHypergraph
 */
public class HypergraphBPG extends AbstractArchetypeGraph implements Hypergraph {

	protected BipartiteGraph bpg;

	public HypergraphBPG() {
        initialize();
	}

    protected void initialize()
    {
        this.bpg = new BipartiteGraph();
        hyperedges = new HashMap();
        hypervertices = new HashMap();
        vertexToHyperVertex = new XToHyperX(hypervertices);
        vertexToHyperEdge = new XToHyperX(hyperedges);
        super.initialize();
    }
    
	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#newInstance()
	 */
	public ArchetypeGraph newInstance() {
		return new HypergraphBPG();
	}

	public static final BipartiteGraph.Choice VERTEX = BipartiteGraph.CLASSA;
	public static final BipartiteGraph.Choice EDGE = BipartiteGraph.CLASSB;

	Map hypervertices, hyperedges;
	private Transformer vertexToHyperVertex, vertexToHyperEdge;

	/**
	 * Adds <code>v</code> to this graph.
	 */
	public Hypervertex addVertex(Hypervertex v) {
		HypervertexBPG hv = (HypervertexBPG) v;
		this.bpg.addVertex(hv.underlying_vertex(), VERTEX);
		hypervertices.put(hv.underlying_vertex(), hv);
		hv.setGraph(this);
		mGraphListenerHandler.handleAdd(v);
		return v;
	}

	/**
	 * Adds a single edge to the graph
	 * 
	 * @see edu.uci.ics.jung.graph.Hypergraph#addEdge(edu.uci.ics.jung.graph.Hyperedge)
	 */
	public Hyperedge addEdge(Hyperedge e) {
		HyperedgeBPG he = (HyperedgeBPG) e;
		this.bpg.addVertex( he.underlying_vertex(), EDGE );
		hyperedges.put( he.underlying_vertex(), he );
		he.setGraph( this );
		mGraphListenerHandler.handleAdd(e);
		return e;
	}

	/**
	 * Returns a set of all the vertices in the graph.
	 * 
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#getVertices()
	 */
	public Set getVertices() {
		return new HashSet( hypervertices.values());
	}

	/**
	 * Returns the set of all edges in the graph.
	 * 
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#getEdges()
	 */
	public Set getEdges() {
		return new HashSet( hyperedges.values() );
	}

	/**
	 * Returns a count of the number of vertices in the graph.
	 *  
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#numVertices()
	 */
	public int numVertices() {
		return hypervertices.size();
	}

	/**
	 * Returns a count of the number of edges in the graph.
	 * 
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#numEdges()
	 */
	public int numEdges() {
		return hyperedges.size();
	}

	public void removeVertex(Hypervertex v) {
		HypervertexBPG hv = (HypervertexBPG) v;
		hypervertices.remove( hv.underlying_vertex() );
		bpg.removeVertex( hv.underlying_vertex() );
		hv.setGraph(null);
		mGraphListenerHandler.handleRemove(v);
	}

	public void removeEdge(Hyperedge e) {
		HyperedgeBPG he = (HyperedgeBPG) e;
		hyperedges.remove( he.underlying_vertex() );
		bpg.removeVertex( he.underlying_vertex() );
		he.setGraph(null);
		mGraphListenerHandler.handleRemove(e);
	}

    public void addVertices(Set vertices)
    {
        for (Iterator iter = vertices.iterator(); iter.hasNext(); )
            addVertex((HypervertexBPG)iter.next());
    }
    
    public void addEdges(Set edges)
    {
        for (Iterator iter = edges.iterator(); iter.hasNext(); )
            addEdge((HyperedgeBPG)iter.next());
    }
    
	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#removeVertices(java.util.Set)
	 */
	public void removeVertices(Set vertices) {
		for (Iterator iter = vertices.iterator(); iter.hasNext();) {
			HypervertexBPG v = (HypervertexBPG) iter.next();
			removeVertex( v );
		}
	}

	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#removeEdges(java.util.Set)
	 */
	public void removeEdges(Set edges) {
		for (Iterator iter = edges.iterator(); iter.hasNext();) {
			HyperedgeBPG e = (HyperedgeBPG) iter.next();
			removeEdge( e );
		}
	}

	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#removeAllEdges()
	 */
	public void removeAllEdges() {
		removeEdges( new HashSet( hyperedges.values() ));
	}

	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#removeAllVertices()
	 */
	public void removeAllVertices() {
		removeVertices( new HashSet( hypervertices.values())  );
	}

	/**
	 * @see edu.uci.ics.jung.graph.ArchetypeGraph#copy()
	 */
	public ArchetypeGraph copy() {
		HypergraphBPG cln = (HypergraphBPG) this.newInstance();
		cln.bpg = (BipartiteGraph) bpg.copy();
		cln.updateHyperTable();
		return cln;
	}

	/**
	 * 
	 */
	private void updateHyperTable() {
		// creates a HyperEdge and HyperVertex for each Edge and Vertex in the tables
		for (Iterator iter = bpg.getAllVertices(VERTEX).iterator();
			iter.hasNext();
			) {
			BipartiteVertex bpv = (BipartiteVertex) iter.next();
			hypervertices.put(bpv, new HypervertexBPG(bpv, this));
		}
		for (Iterator iter = bpg.getAllVertices(EDGE).iterator();
			iter.hasNext();
			) {
			BipartiteVertex bpe = (BipartiteVertex) iter.next();
			hyperedges.put(bpe, new HyperedgeBPG(bpe, this));
		}
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#addUserDatum(java.lang.Object, java.lang.Object, edu.uci.ics.jung.utils.UserDataContainer.CopyAction)
	 */
	public void addUserDatum(Object key, Object datum, CopyAction copyAct) {
		bpg.addUserDatum(key, datum, copyAct);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#importUserData(edu.uci.ics.jung.utils.UserDataContainer)
	 */
	public void importUserData(UserDataContainer udc) {
		bpg.importUserData(udc);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#getUserDatumKeyIterator()
	 */
	public Iterator getUserDatumKeyIterator() {
		return bpg.getUserDatumKeyIterator();
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#getUserDatumCopyAction(java.lang.Object)
	 */
	public CopyAction getUserDatumCopyAction(Object key) {
		return bpg.getUserDatumCopyAction(key);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#getUserDatum(java.lang.Object)
	 */
	public Object getUserDatum(Object key) {
		return bpg.getUserDatum(key);
	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#setUserDatum(java.lang.Object, java.lang.Object, edu.uci.ics.jung.utils.UserDataContainer.CopyAction)
	 */
	public void setUserDatum(Object key, Object datum, CopyAction copyAct) {
		bpg.setUserDatum(key, datum, copyAct);

	}

	/**
	 * @see edu.uci.ics.jung.utils.UserDataContainer#removeUserDatum(java.lang.Object)
	 */
	public Object removeUserDatum(Object key) {
		return bpg.removeUserDatum(key);
	}

	/**
	 * Adds a vertex that is already part of the BPG. Is a part of
	 * the Vertex.copy() system.
	 * @param hv
	 */
	void addVertex_without_adding(HypervertexBPG hv) {
		hypervertices.put( hv.underlying_vertex(), hv );
		hv.setGraph( this );
	}

	/**
	 * @param vertex2
	 * @return the vertex in the hypergraph corresponding to the underlying 
     * bipartite vertex <code>vertex2</code>
	 */
	public ArchetypeVertex getVertexCorrespondingTo(BipartiteVertex vertex2) {
		return (HypervertexBPG) hypervertices.get(vertex2);
	}

	/**
	 * @param vertex2
     * @return the edge in the hypergraph corresponding to the underlying 
     * bipartite vertex <code>vertex2</code>
	 */
	public ArchetypeEdge getEdgeCorrespondingTo(BipartiteVertex vertex2) {
		return (HyperedgeBPG) hyperedges.get(vertex2);
	}

	/**
	 * @param realNeighbors
	 * @return
	 */
	Set translateUnderlyingVertices(Set vertices) {
		Collection translated = CollectionUtils.collect( vertices, vertexToHyperVertex );
		return new HashSet( translated );
	}

	/**
	 * @param set
	 * @return
	 */
	Set translateUnderlyingEdges(Set vertices) {
		Collection translated = CollectionUtils.collect( vertices, vertexToHyperEdge);
		return new HashSet( translated );
	}


	private class XToHyperX implements Transformer {
		private Map map;
		XToHyperX( Map m ) {
			this.map = m;
		}
		public Object transform(Object o) {
			return map.get(o);
		}
	}

	/**
	 * Returns a BipartiteGraph equivalent to this Graph. Each
	 * vertex in the BipartiteGraph will be Equal to a HyperVertex
	 * or a HyperEdge in this graph.
	 * (Note that equals is NOT symmetrical in this case;
	 * <tt>hyperedge.equals( vertex )</tt>
	 * will return true for one of these vertices, but
	 * <tt>vertex.equals( hyperedge )</tt>
	 * will return false.
	 */
	public BipartiteGraph getBipartiteGraphEquivalent() {
		return (BipartiteGraph) bpg.copy();
	}

}
