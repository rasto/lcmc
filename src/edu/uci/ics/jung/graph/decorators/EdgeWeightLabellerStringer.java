/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
/*
 * Created on Jun 13, 2003
 *
 */
package edu.uci.ics.jung.graph.decorators;

import java.text.NumberFormat;

import edu.uci.ics.jung.graph.ArchetypeEdge;

/**
 *
 * An EdgeStringer provides a string Label for any edge: the
 * String is the Weight produced by the EdgeWeightLabeller that
 * it takes as input.
 *
 * @author danyelf
 *
 */
public class EdgeWeightLabellerStringer implements EdgeStringer {

    protected EdgeWeightLabeller ewl;
    protected NumberFormat numberFormat;

    public EdgeWeightLabellerStringer( EdgeWeightLabeller ewl ) {
        this.ewl = ewl;
        if (numberFormat == null ) {
            numberFormat = prepareNumberFormat();
        }
    }
    
    protected NumberFormat prepareNumberFormat() {
        NumberFormat nf = NumberFormat.getInstance();
        return nf;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.EdgeStringer#getLabel(ArchetypeEdge)
     */
    public String getLabel(ArchetypeEdge e) {
        return numberFormat.format(ewl.getWeight(e));
    }
   
}