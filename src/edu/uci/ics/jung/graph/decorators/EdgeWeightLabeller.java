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
 * Created on Jun 13, 2003
 *
 */
package edu.uci.ics.jung.graph.decorators;

import java.util.HashMap;
import java.util.Map;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.utils.UserData;

/**
 * 
 * A EdgeWeightLabeller applies a label to the edges of a Graph. 
 * All edge weights are integers; weights need not be unique.
 * (The cost of not being unique is that there's no way to
 * look up edges by weight.)
 * 
 * Note that this stores information with the graph, and
 * as such is not flexible to addition and substraction of
 * nodes.
 * 
 * @author danyelf
 * 
 * TODO : Should store weight in a decorator, per-Edge instead of 
 * per-Graph.
 *
 */
public class EdgeWeightLabeller implements NumberEdgeValue {

	/**
	 * The UserData key that stores edge weights.
	 */
	public static final Object WEIGHT_DEFAULT_KEY = "WeightDefaultKey";

	/**
	 * Finds or creates an edge labeller for the graph, using
	 * the default user data key.
	 * @param g
	 * @return	the labeller
	 */
	public static EdgeWeightLabeller getLabeller( Graph g ) {
		return getLabeller( g , WEIGHT_DEFAULT_KEY );
	}
	
	/**
	 * Checks for an edge labeleller for the graph, using
	 * the default user data key.
	 * @param g
	 * @return	the labeller
	 */
	public static boolean hasWeightLabeller ( Graph g ) {
		return hasWeightLabeller( g , WEIGHT_DEFAULT_KEY );
	}

	/**
	 * Checks an edge labeleller for the graph at the given key.
	 * @return	the labeller
	 */
	public static boolean hasWeightLabeller( Graph g, Object key ) {
		EdgeWeightLabeller id = (EdgeWeightLabeller) g.getUserDatum( key );
		return (id != null);
	}

	/**
	 * Finds or creates an edge labeleller for the graph, using
	 * the given userdata key.
	 * @param g
	 * @return	the labeller
	 */
	public static EdgeWeightLabeller getLabeller( Graph g, Object key ) {
		EdgeWeightLabeller id = (EdgeWeightLabeller) g.getUserDatum( key );
		if (id != null)
			return id;
		id = new EdgeWeightLabeller( g );
		g.addUserDatum( key, id, UserData.REMOVE );
		return id;
	}

	/**
	 * Gets the weight of a particualr edge. Throws an exception if
	 * the edge is not weighted, or if the edge is not a part of
	 * the graph. 
	 * @param e	an edge that has been weighted.
	 */
	public int getWeight( ArchetypeEdge e ) {	
		if (! edgeToWeight.containsKey( e )) {
			throw new IllegalArgumentException("This edge has no assigned weight");
		}
		return ((Number) edgeToWeight.get( e )).intValue();
	}

	/**
	 * Returns the graph associated with this particular 
	 * labeller.
	 */
	public Graph getGraph() {
		return graph;
	}

	/**
	 * Sets an edge to this weight.
	 * @param e	the edge
	 * @param i the weight
	 * @throws if the edge is not part of the graph 
	 */
	public void setWeight(ArchetypeEdge e, int i) {
		if (graph.getEdges().contains( e )) {
			edgeToWeight.put( e, new Integer( i ));
		} else {
			// throw some sort of exception here
			throw new IllegalArgumentException("This edge is not a part of this graph");
		}		
	}
	
    /**
     * Removes the weight stored by this decorator for the indicated edge <code>e</code>,
     * and returns the value of this weight (or <code>null</code> if there was no
     * such weight for this edge).
     */
    public Number removeWeight(ArchetypeEdge e)
    {
        return (Number)edgeToWeight.remove(e);
    }

    /**
     * Clears all weights stored by this decorator.
     */
    public void clear()
    {
        edgeToWeight.clear();
    }
    
	private Map edgeToWeight = new HashMap();
	private Graph graph;

	/**
	 * @param g
	 */
	private EdgeWeightLabeller(Graph g) {
		this.graph = g;
	}

    /**
     * @see edu.uci.ics.jung.graph.decorators.NumberEdgeValue#getNumber(edu.uci.ics.jung.graph.ArchetypeEdge)
     */
    public Number getNumber(ArchetypeEdge e)
    {
        Number value = (Number)edgeToWeight.get(e);
        if (value == null) 
            throw new IllegalArgumentException("This edge is unweighted");
        return value;
    }

    /**
     * @see edu.uci.ics.jung.graph.decorators.NumberEdgeValue#setNumber(edu.uci.ics.jung.graph.ArchetypeEdge, java.lang.Number)
     */
    public void setNumber(ArchetypeEdge e, Number n)
    {
        if (graph.getEdges().contains( e )) {
            edgeToWeight.put( e, n);
        } else {
            // throw some sort of exception here
            throw new IllegalArgumentException("This edge is not a part of this graph");
        }       
    }

}