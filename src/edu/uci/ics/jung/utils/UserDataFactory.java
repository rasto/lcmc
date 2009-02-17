/*
 * Created on Jul 30, 2005
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

/**
 * A class which generates <code>UserDataContainer</code> instances.
 * Used in <code>UserDataDelegate</code>.
 * 
 * @author Joshua O'Madadhain
 */
public interface UserDataFactory
{
    /**
     * Returns a single <code>UserDataContainer</code> instance.
     * Depending on the architecture involved, this may be a singleton
     * instance, or a new instance may be generated each time the
     * method is called.
     */
    public UserDataContainer getInstance();
}
