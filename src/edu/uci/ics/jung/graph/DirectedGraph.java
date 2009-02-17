/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph;

/**
 * A tagging interface for implementations of <code>Graph</code> 
 * whose edge set consists of implementations of <code>DirectedEdge</code>.
 * Used for enforcing algorithm constraints (for example, to provide a 
 * compile-time parameter check for algorithms that only operate on 
 * directed graphs).
 * 
 * @author Joshua O'Madadhain
 * @author Scott White
 * @author Danyel Fisher
 * 
 * @see DirectedEdge
 */
public interface DirectedGraph extends Graph 
{
}
