/*
 * Created on Apr 13, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph;

import java.util.Collection;


/**
 * An interface for k-partite graphs.  k-partite graphs'
 * vertices are distributed among k partitions 
 * (disjoint subsets), and their edges are constrained to 
 * connect vertices in distinct partitions.
 * 
 * @author Joshua O'Madadhain
 */
public interface KPartiteGraph extends Graph
{
    /**
     * Returns the array of predicates which define the partitions
     * of this graph.
     */
    public Collection getPartitions();
}
