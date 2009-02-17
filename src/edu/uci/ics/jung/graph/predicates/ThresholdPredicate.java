/*
 * Copyright (c) 2004, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Aug 9, 2004
 */
package edu.uci.ics.jung.graph.predicates;

import org.apache.commons.collections.Predicate;

/**
 * A predicate which passes Numbers whose value satisfies a threshold requirement.
 *  
 * @author Joshua O'Madadhain
 */
public class ThresholdPredicate implements Predicate
{
    protected double threshold;
    protected boolean greater_equal;
    
    /**
     * Creates a ThresholdPredicate with the specified threshold value.
     * If <code>greater_equal</code> is true, only objects whose double 
     * values are greater than or equal to <code>threshold</code> will evaluate to 
     * <code>true</code>; otherwise, only objects whose values are less
     * than or equal will evaluate to true.
     *   
     * @param threshold     the threshold value
     * @param greater_equal
     */
    public ThresholdPredicate(double threshold, boolean greater_equal)
    {
        this.threshold = threshold;
        this.greater_equal = greater_equal;
    }

    /**
     * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
     */
    public boolean evaluate(Object arg0)
    {
        double value = ((Number)arg0).doubleValue();
        if (greater_equal && value >= threshold)
            return true;
        if (!greater_equal && value <= threshold)
            return true;
        return false;
    }
}
