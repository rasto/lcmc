/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Mar 3, 2004
 */
package edu.uci.ics.jung.graph.predicates;

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * A predicate which tests to see whether a specified
 * vertex has any neighbors.  Not suitable for use
 * as a constraint.
 * 
 * @author Joshua O'Madadhain
 */
public class IsolatedVertexPredicate extends VertexPredicate {

    private static IsolatedVertexPredicate instance;

    private static final String message = "IsolatedVertexPredicate";

    /**
     * Returns an instance of this class.
     */
    public static IsolatedVertexPredicate getInstance() {
        if (instance == null) instance = new IsolatedVertexPredicate();
        return instance;
    }

    public String toString() {
        return message;
    }

    /**
     * Returns <code>true</code> if the argument is a <code>Vertex</code>
     * whose degree is 0.
     * 
     * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
     */
    public boolean evaluateVertex(ArchetypeVertex v) {
        return (v.degree() == 0);
    }

    /**
     * This constructor is protected in order to keep equals working by
     * retaining only one instance. (Currently, since there can only be one
     * instance, equals trivially returns true. If this class is extended, be
     * careful to write equals correctly.)
     */
    protected IsolatedVertexPredicate() {
        super();
    }
}