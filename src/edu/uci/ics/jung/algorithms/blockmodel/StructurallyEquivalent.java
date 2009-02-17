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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * Checks a graph for sets of structurally equivalent vertices: vertices that
 * share all the same edges. Specifically, In order for a pair of vertices <i>
 * i</i> and <i>j</i> to be structurally equivalent, the set of <i>i</i>'s
 * neighbors must be identical to the set of <i>j</i>'s neighbors, with the
 * exception of <i>i</i> and <i>j</i> themselves. This algorithm finds all
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
public class StructurallyEquivalent implements EquivalenceAlgorithm {

	static StructurallyEquivalent instance = null;
	
	public static StructurallyEquivalent getInstance() {
		if ( instance == null ) {
			instance = new StructurallyEquivalent();
		}
		return instance;
	}
	
	public StructurallyEquivalent() {

	}

	public EquivalenceRelation getEquivalences(Graph g) {
		return createEquivalenceClasses(g, checkEquivalent(g));
	}

	/**
	 * Takes in a Set of Pairs (as in the resutls of checkEquivalent) and
	 * massages into a Set of Sets, where each Set is an equivalence class.
	 */
	protected EquivalenceRelation createEquivalenceClasses(Graph g, Set s) {
		Set rv = new HashSet();
		Map intermediate = new HashMap();
		for (Iterator iter = s.iterator(); iter.hasNext();) {
			Pair p = (Pair) iter.next();
			Set res = (Set) intermediate.get(p.getFirst());
			if (res == null)
				res = (Set) intermediate.get(p.getSecond());
			if (res == null) {
				// we haven't seen this one before
				res = new HashSet();
			}
			res.add(p.getFirst());
			res.add(p.getSecond());
			intermediate.put(p.getFirst(), res);
			intermediate.put(p.getSecond(), res);

		}
		rv.addAll(intermediate.values());
		return new EquivalenceRelation(rv, g);
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
		Set alreadyEquivalent = new HashSet();

		List l = new ArrayList(g.getVertices());

		for (Iterator iter = l.iterator(); iter.hasNext();) {
			Vertex v1 = (Vertex) iter.next();

			if (alreadyEquivalent.contains(v1))
				continue;

			for (Iterator iterator = l.listIterator(l.indexOf(v1) + 1); iterator.hasNext();) {
				Vertex v2 = (Vertex) iterator.next();

				if (alreadyEquivalent.contains(v2))
					continue;

				if (!canpossiblycompare(v1, v2))
					continue;

				if (isStructurallyEquivalent(v1, v2)) {
					Pair p = new Pair(v1, v2);
					alreadyEquivalent.add(v2);
					rv.add(p);
				}

			}
		}

		return rv;

	}

	/**
	 * Checks whether a pair of vertices are structurally equivalent.
	 * Specifically, whether v1's predecessors are equal to v2's predecessors,
	 * and same for successors.
	 * 
	 * @param v1
	 * @param v2
	 */
	protected boolean isStructurallyEquivalent(Vertex v1, Vertex v2) {
		
		count ++;
		
		if( v1.degree() != v2.degree()) {
			return false;
		}

		Set n1 = new HashSet(v1.getPredecessors());
		n1.remove(v2);
		n1.remove(v1);
		Set n2 = new HashSet(v2.getPredecessors());
		n2.remove(v1);
		n2.remove(v2);

		Set o1 = new HashSet(v1.getSuccessors());
		Set o2 = new HashSet(v2.getSuccessors());
		o1.remove(v1);
		o1.remove(v2);
		o2.remove(v1);
		o2.remove(v2);

		// this neglects self-loops and directed edges from 1 to other
		boolean b = (n1.equals(n2) && o1.equals(o2));
		if (!b)
			return b;
		
		// if there's a directed edge v1->v2 then there's a directed edge v2->v1
		b &= ( v1.isSuccessorOf(v2) == v1.isSuccessorOf(v2));
		
		// self-loop check
		b &= ( v1.isSuccessorOf(v1) == v2.isSuccessorOf(v2));

		return b;

	}

	public static int count = 0;
	
	/**
	 * This is a space for optimizations. For example, for a bipartitegraph,
	 * vertices from class_A and class_B cannot possibly be compared.
	 * 
	 * @param v1
	 * @param v2
	 */
	protected boolean canpossiblycompare(Vertex v1, Vertex v2) {
		return true;
	}

}
