/*
 * Created on Dec 7, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.predicates;

import org.apache.commons.collections.Predicate;

/**
 * A tagging interface for predicates that should not be copied
 * along with their graph.  This interface should generally be implemented
 * by graph-specific constraints (e.g. <code>NotInGraphVertexPredicate</code>).
 * 
 * @author Joshua O'Madadhain
 */
public interface UncopyablePredicate extends Predicate
{

}
