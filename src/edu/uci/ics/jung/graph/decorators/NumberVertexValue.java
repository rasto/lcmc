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
 * Created on Aug 11, 2004
 *
 */
package edu.uci.ics.jung.graph.decorators;

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * A generalized interface for setting and getting <code>Number</code>s
 * of <code>ArchetypeVertex</code>s.  Using this interface allows 
 * algorithms to work without having to know how vertices store this
 * data.
 *  
 * @author Joshua O'Madadhain
 */
public interface NumberVertexValue
{
    /**
     * @param v     the vertex to examine
     * @return      the Number associated with this vertex
     */
    public Number getNumber(ArchetypeVertex v);
    
    /**
     * 
     * @param v     the vertex whose value we're setting
     * @param n     the Number to which we're setting the vertex's value
     */
    public void setNumber(ArchetypeVertex v, Number n);
}
