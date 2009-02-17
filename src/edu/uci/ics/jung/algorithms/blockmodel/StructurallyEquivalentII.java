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
 * Created on Jan 28, 2004
 */
package edu.uci.ics.jung.algorithms.blockmodel;
import java.util.*;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;
/**
 * Checks a graph for sets of structurally equivalent vertices: vertices that
 * share all the same edges. Specifically, In order for a pair of vertices <i>
 * i </i> and <i>j </i> to be structurally equivalent, the set of <i>i </i>'s
 * neighbors must be identical to the set of <i>j </i>'s neighbors, with the
 * exception of <i>i </i> and <i>j </i> themselves. This algorithm finds all
 * sets of equivalent vertices in O(V^2) time.
 * 
 * You can extend this class to have a different definition of equivalence (by
 * overriding "isStructurallyEquivalent"), and may give it hints for
 * accelerating the process by overriding canpossiblycompare. (For example, in
 * a bipartitegraph, canPossiblyCompare may return false for vertices in
 * different partitions. This function should be fast.)
 * 
 * @author danyelf
 */
public class StructurallyEquivalentII extends StructurallyEquivalent {

	public static StructurallyEquivalent getInstance() {
		if (instance == null) {
			instance = new StructurallyEquivalentII();
		}
		return instance;
	}

	public StructurallyEquivalentII() {
	}

	/**
	 * For each vertex pair v, v1 in G, checks whether v and v1 are fully
	 * equivalent: meaning that they connect to the exact same vertices. (Is
	 * this regular equivalence, or whathaveyou?)
	 * 
	 * Returns a Set of Pairs of vertices, where all the vertices in the inner
	 * Pairs are equivalent.
	 * 
	 * @param g
	 */
	public Set checkEquivalent(Graph g) {
		Set rv = new HashSet();
		/*
		 * this is kind of subtle. if a vertex is equivalent to any other
		 * vertex, then all the equivalences will be found on the first pass.
		 * That is : if A is equivalent to B, then there are no neighbors of B
		 * that are equivalent to it that are not also neighbors of A.
		 */
		Set alreadyEquivalent = new HashSet();
		/*
		 * Tracks all vertices that we've used as an origin point.
		 */
		Set alreadyChecked = new HashSet();
		List l = new ArrayList(g.getVertices());
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			Vertex v1 = (Vertex) iter.next();
			alreadyChecked.add(v1);
			if (alreadyEquivalent.contains(v1))
				continue;
			boolean haveHitOne = false;
			Set neighbors = new HashSet( v1.getNeighbors() );
			neighbors.removeAll( alreadyChecked );
			// check if we're equivalent to any neighbor
			for (Iterator iterator = neighbors.iterator(); iterator.hasNext();) {
				Vertex v2 = (Vertex) iterator.next();
				haveHitOne |= checkEquivalence(v1, v2, alreadyEquivalent, rv );
			}

			// if we aren't, then we might be equivalent to one or another 2nd
			// remove neighbor
			// NOTE: v1 can only be equiv to a second-neighbor if it is not
			// equivalnt to a first neighbor
			if (!haveHitOne) {
				Set secondNeighbors = getSecondNeighbors(v1);
				secondNeighbors.removeAll(alreadyChecked);
				for (Iterator iterator = secondNeighbors.iterator(); iterator
						.hasNext();) {
					Vertex v2 = (Vertex) iterator.next();
					checkEquivalence(v1, v2, alreadyEquivalent, rv );
				}

			}
		}
		return rv;
	}
	
	
	boolean checkEquivalence( Vertex v1, Vertex v2, Set alreadyEquivalent, Set rv ) {
		if (alreadyEquivalent.contains(v2))
			return false;
		if (!canpossiblycompare(v1, v2))
			return false;
		if (isStructurallyEquivalent(v1, v2)) {
			Pair p = new Pair(v1, v2);
			alreadyEquivalent.add(v2);
			rv.add(p);
			return true;
		}
		return false;
	}
	
	private Set getSecondNeighbors(Vertex v1) {
		Set secondNeighbors = new HashSet();
		for (Iterator iterator = v1.getNeighbors().iterator(); iterator
				.hasNext();) {
			Vertex intermediate = (Vertex) iterator.next();
			secondNeighbors.addAll(intermediate.getNeighbors());
		}
		return secondNeighbors;
	}

}
