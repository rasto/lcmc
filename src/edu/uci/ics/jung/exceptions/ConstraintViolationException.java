/*
 * Created on Sep 8, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.exceptions;

import org.apache.commons.collections.Predicate;

/**
 * 
 * @author Joshua O'Madadhain
 */
public class ConstraintViolationException extends IllegalArgumentException
{
    protected Predicate constraint;

    /**
     * @param arg0
     */
    public ConstraintViolationException(String arg0, Predicate constraint)
    {
        super(arg0);
        this.constraint = constraint;
    }

    public Predicate getViolatedConstraint()
    {
        return constraint;
    }
}
