/*
 * Copyright (c) 2004, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Jun 18, 2004
 */
package edu.uci.ics.jung.graph.predicates;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.predicates.VertexPredicate;

/**
 * Evaluates to <code>true</code> if and only if 
 * the specified vertex is a source, i.e., has 
 * no incoming directed edges.
 *  
 * @author Joshua O'Madadhain
 */
public class SourceVertexPredicate extends VertexPredicate
{
    protected static SourceVertexPredicate instance;

    protected SourceVertexPredicate()
    {
        super();
    }
    
    public static SourceVertexPredicate getInstance()
    {
        if (instance == null)
            instance = new SourceVertexPredicate();
        return instance;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.predicates.VertexPredicate#evaluateVertex(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
    public boolean evaluateVertex(ArchetypeVertex arg0)
    {
        if (! (arg0 instanceof Vertex))
            return false;
        
        return (((Vertex)arg0).inDegree() == 0);
    }
}
