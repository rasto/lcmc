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
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.UserData;

import java.util.Set;
import java.util.Iterator;

/**
 * This class provides basic infrastructure for relative authority algorithms that compute the importance of nodes
 * relative to one or more root nodes. The services provided are:
 * <ul>
 * <li>The set of root nodes (priors) is stored and maintained</li>
 * <li>Getters and setters for the prior rank score are provided</li>
 * </ul>
 * 
 * @author Scott White
 */
public abstract class RelativeAuthorityRanker extends AbstractRanker {
    private Set mPriors;
    /**
     * The default key used for the user datum key corresponding to prior rank scores.
     */
    public static final String PRIOR_KEY = "jung.algorithms.importance.RelativeAuthorityRanker.PriorRankScore";

    /**
     * Cleans up all of the prior rank scores on finalize.
     */
    protected void finalizeIterations() {
        super.finalizeIterations();
        for (Iterator vIt = getVertices().iterator();vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            currentVertex.removeUserDatum(PRIOR_KEY);
        }

    }

    /**
     * Returns the user datum key for the prior rank score.
     * @return the user datum key for the prior rank score
     */
    protected String getPriorRankScoreKey() {
        return PRIOR_KEY;
    }

    /**
     * Retrieves the value of the prior rank score.
     * @param v the root node (prior)
     * @return the prior rank score
     */
    protected double getPriorRankScore(Vertex v) {
        return ((MutableDouble) v.getUserDatum(PRIOR_KEY)).doubleValue();

    }

    /**
     * Allows the user to specify a value to set for the prior rank score
     * @param v the root node (prior)
     * @param value the score to set to
     */
    public void setPriorRankScore(Vertex v, double value) {
        MutableDouble doubleVal = (MutableDouble) v.getUserDatum(PRIOR_KEY);
        if (doubleVal == null) {
            doubleVal = new MutableDouble(value);
        } else {
            doubleVal.setDoubleValue(value);
        }
        v.setUserDatum(PRIOR_KEY,doubleVal,UserData.SHARED);
    }

    /**
     * Retrieves the set of priors.
     * @return the set of root nodes (priors)
     */
    protected Set getPriors() { return mPriors; }

    /**
     * Specifies which vertices are root nodes (priors).
     * @param priors the root nodes
     */
    protected void setPriors(Set priors) { mPriors = priors; }
}
