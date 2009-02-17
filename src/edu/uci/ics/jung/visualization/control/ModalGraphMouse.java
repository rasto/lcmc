/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Aug 26, 2005
 */

package edu.uci.ics.jung.visualization.control;

import java.awt.event.ItemListener;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.uci.ics.jung.visualization.VisualizationViewer.GraphMouse;

/**
 * Interface for a GraphMouse that supports modality.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 */
public interface ModalGraphMouse extends GraphMouse {
    
    void setMode(Mode mode);
    
    /**
     * @return Returns the modeListener.
     */
    ItemListener getModeListener();
    
    /**
     *  The Mode class implements the typesafe enum pattern.
     *  This pattern is fully described in Joshua Bloch's book
     *  Effective Java Programming Language Guide, Item 21.
     *
     *  Created: Sun Aug 28 10:25:16 2005
     *
     *  @author Tom Nelson
     *  @version 1.0
     */
    
    public class Mode implements Serializable {

        private final String mode;
        
        /**
         *  A private constructor which creates the instances
         *  for this enumerated type.
         *
         * @param mode Value to assign to mode
         */
        private Mode (String mode) {
            this.mode = mode;
        }
        
        /**
         *  Gets the value of mode
         *
         *  @return the value of mode
         */
        public String getMode() {
            return mode;
        }
        
        /**
         *  Gets a list of Mode constants.
         *
         *  @return a list of Mode constants
         */
        public static List getValues() {
            return VALUES;
        }
        
        /**
         *  Returns a string representation of this object.
         *
         *  @return a string representation of this object.
         */
        public String toString() {
            return mode;
        }
        
        // Ordinal of the next Mode to be created
        private static int nextOrdinal = 0;
        
        // Assign an ordinal to this suit
        private final int ordinal = nextOrdinal ++;
        
        // The set of Mode constants.
        public static final Mode TRANSFORMING = new Mode("Transforming");
        public static final Mode PICKING      = new Mode("Picking");
        public static final Mode EDITING      = new Mode("Editing");
        
        private static final Mode[] PRIVATE_VALUES = 
        { TRANSFORMING, PICKING, EDITING };
        
        public static final List VALUES =
            Collections.unmodifiableList (Arrays.asList(PRIVATE_VALUES));
        
        /**
         *  Designates an alternative object to be used when
         *  reading an object from a stream.  This prevents
         *  duplicate constants from coexisting.  This method
         *  is automatically called as a result of deserialization.
         *
         *  @throws ObjectStreamException when a deserialization error occurs
         */
        private Object readResolve() throws ObjectStreamException {
            return PRIVATE_VALUES[ordinal];
        }
        
    } // Mode

}