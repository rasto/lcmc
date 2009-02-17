/*
 * Created on Mar 10, 2005
 *
 *Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */

package edu.uci.ics.jung.graph.decorators;

import java.awt.Color;
import java.awt.Paint;

import edu.uci.ics.jung.graph.Vertex;

/**
 * This class replaces VertexColorFunction. (All COLORs are PAINTs, so this is a
 * direct drop-in replacement.) Use VertexColorToVertexPaintConvertor if you
 * want to convert an existing VertexColorFunction.
 * <p>
 * The fill Paint is used to fill the vertex's shape, and the draw Paint is 
 * used to draw its outline. Expect code that looks a little like this
 * to execute it:
 * 
 * <pre>
 *     graphics.setPaint( vpf.getFillPaint( v ) );
 *     graphics.fill( shape );
 * 	   graphics.setPaint( vpf.getDrawPaint( v ));
 * 	   graphics.setStroke ...
 * 	   graphics.draw( shape );
 * </pre>
 * 
 * If you want the interior or outline to be transparent, you
 * should have it return VertexPaintFunction.TRANSPARENT for the appropriate type.
 * 
 * @author Danyel Fisher - Microsoft Research
 * @author Tom Nelson - RABA Technologies
 * @author Joshua O'Madadhain
 */
public interface VertexPaintFunction {

	public Paint getFillPaint(Vertex v);

	public Paint getDrawPaint(Vertex v);

	public static final Paint TRANSPARENT = new Color( 0, 0, 0, 0);
	
}

