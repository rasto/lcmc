/*
 * Created on May 9, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.decorators;

import java.util.HashMap;
import java.util.Map;

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * A simple implementation of <code>NumberVertexValue</code> backed by a 
 * <code>Map</code>.
 * 
 * @author Joshua O'Madadhain
 */
public class MapNumberVertexValue implements NumberVertexValue
{
    protected Map map;
    
    public MapNumberVertexValue()
    {
        this.map = new HashMap();
    }
    
    public Number getNumber(ArchetypeVertex v)
    {
        return (Number)map.get(v);
    }

    public void setNumber(ArchetypeVertex v, Number n)
    {
        map.put(v, n);
    }
}
