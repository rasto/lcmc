/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Oct 9, 2004
 *
  */
package edu.uci.ics.jung.visualization;

import java.io.IOException;
import java.io.Serializable;

import edu.uci.ics.jung.utils.ChangeEventSupport;


/**
 * interface for PersistentLayout
 * Also holds a nested class Point to serialize the
 * Vertex locations
 * 
 * @author Tom Nelson - RABA Technologies
 */
public interface PersistentLayout extends Layout, ChangeEventSupport {
    
    void persist(String fileName) throws IOException;

    void restore(String fileName) throws IOException, ClassNotFoundException;
    
    void lock(boolean state);
    
    /**
     * a serializable class to save locations
     */
    static class Point implements Serializable {
        public double x;
        public double y;
        public Point(double x, double y) {
            this.x=x;
            this.y=y;
        }
    }

}