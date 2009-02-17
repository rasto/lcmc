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
 * Created on Mar 22, 2004
 */
package edu.uci.ics.jung.graph.predicates;

import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * Returns true if this graph is a clique (that is, if
 * each vertex in the graph is a neighbor of each other 
 * vertex; also known as a <i>complete graph</i>).
 * 
 * @author danyelf
 */
public class CliqueGraphPredicate extends GraphPredicate {

    private static final String message = "CliqueGraphPredicate";
    private static CliqueGraphPredicate instance;
    
    protected CliqueGraphPredicate() 
    {        
        super();
    }
    
    public static CliqueGraphPredicate getInstance()
    {
        if (instance == null)
            instance = new CliqueGraphPredicate();
        return instance;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.predicates.GraphPredicate#evaluateGraph(edu.uci.ics.jung.graph.ArchetypeGraph)
     */
    public boolean evaluateGraph(ArchetypeGraph g) {
        for (Iterator iter = g.getVertices().iterator(); iter.hasNext();) {
            Vertex v = (Vertex) iter.next();
            int wanted = g.numVertices() - 1;
            Set s = v.getNeighbors();
            
            if( s.contains( v )) 
                wanted += 1;
            if (s.size() != wanted)
                return false;
        }
        return true;
        
    }

    public String toString() {
        return message;
    }

}
