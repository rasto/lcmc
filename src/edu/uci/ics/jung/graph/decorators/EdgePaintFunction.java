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
 * Created on Mar 10, 2005
 */
package edu.uci.ics.jung.graph.decorators;

import java.awt.Color;
import java.awt.Paint;

import edu.uci.ics.jung.graph.Edge;

/**
 * This class replaces EdgeColorFunction. (All COLORs are PAINTs, so this is a
 * direct drop-in replacement.) Use EdgeColorToEdgePaintConvertor if you
 * want to convert an existing EdgeColorFunction.
 * <p>
 * The fill Paint is used to fill the edge's shape, and the draw Paint is 
 * used to draw its outline. Expect code that looks a little like this
 * to execute it:
 * 
 * <pre>
 *     graphics.setPaint( epf.getFillPaint( e ) );
 *     graphics.fill( shape );
 *     graphics.setPaint( epf.getDrawPaint( e ));
 *     graphics.setStroke ...
 *     graphics.draw( shape );
 * </pre>
 * 
 * If you want the interior or outline to be transparent, you
 * should have it return EdgePaintFunction.TRANSPARENT for the appropriate type.
 * 
 * @author Tom Nelson - RABA Technologies
 * @author Joshua O'Madadhain
 */
public interface EdgePaintFunction  {
    
    public Paint getDrawPaint(Edge e);

    public Paint getFillPaint(Edge e);
    
    public static final Paint TRANSPARENT = new Color( 0, 0, 0, 0);
}
