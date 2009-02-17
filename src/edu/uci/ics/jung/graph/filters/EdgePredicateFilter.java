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
 * Created on Apr 21, 2004
 */
package edu.uci.ics.jung.graph.filters;

import org.apache.commons.collections.Predicate;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.predicates.EdgePredicate;


/**
 * This is a simple Edge filter that accepts the edges which its 
 * Predicate accepts.
 * 
 * @author danyelf
 */
public class EdgePredicateFilter extends GeneralEdgeAcceptFilter {
    
//    protected EdgePredicate predicate;
    protected Predicate predicate;

    public EdgePredicateFilter( EdgePredicate ep ) {
        this.predicate = ep;
    }
    
    public EdgePredicateFilter( Predicate p)
    {
        this.predicate = p;
    }

    /**
     * @see edu.uci.ics.jung.graph.filters.GeneralVertexAcceptFilter#acceptVertex(edu.uci.ics.jung.graph.Vertex)
     */
    public boolean acceptEdge(Edge e) {
//        return predicate.evaluateEdge(e );
        return predicate.evaluate(e);
    }

    /**
     * @see edu.uci.ics.jung.graph.filters.Filter#getName()
     */
    public String getName() {
        return "ePred:" + predicate; 
    }

}
