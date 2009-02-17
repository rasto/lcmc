/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms.importance;

import edu.uci.ics.jung.graph.Vertex;

/**
 * A data container for a node ranking.
 * 
 * @author Scott White
 */
public class NodeRanking extends Ranking {

    /**
     * Allows the values to be set on construction.
     * @param originalPos The original (0-indexed) position of the instance being ranked
     * @param rankScore The actual rank score (normally between 0 and 1)
     * @param vertex The vertex being ranked
     */
    public NodeRanking(int originalPos, double rankScore, Vertex vertex) {
        super(originalPos, rankScore);
        this.vertex = vertex;
    }

    /**
     * The vertex being ranked
     */
    public Vertex vertex;
}
