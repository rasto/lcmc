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

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.predicates.VertexPredicate;


/**
 * This is a simple Vertex filter that accepts the vertices which its 
 * Predicate accepts.
 * 
 * @author danyelf
 */
public class VertexPredicateFilter extends GeneralVertexAcceptFilter {
    
//    protected VertexPredicate predicate;
    protected Predicate predicate;

    public VertexPredicateFilter( VertexPredicate vp ) {
        this.predicate = vp;
    }

    public VertexPredicateFilter( Predicate p )
    {
        this.predicate = p;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.filters.GeneralVertexAcceptFilter#acceptVertex(edu.uci.ics.jung.graph.Vertex)
     */
    public boolean acceptVertex(Vertex vert) {
//        return predicate.evaluateVertex(vert);
        return predicate.evaluate(vert);
    }

    /**
     * @see edu.uci.ics.jung.graph.filters.Filter#getName()
     */
    public String getName() {
        return "VPred:" + predicate; 
    }

}
