/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */

package edu.uci.ics.jung.visualization.contrib;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

/**
 * This pluggable utility paints either a "classic" or a "sleek" filled arrow 
 * on a given edge. To use, create an instance of the Arrow object
 * with your preferred thickness, and then call 
 * arrow.drawArrow( graphics, source_x1, source_y1, dest_x, dest_y2 ) for the edge.
 * 
 * Note that the arrow simply uses the color currently set in the graphics context.
 * 
 * @author Jon Froehlich
 */
public class Arrow {

	public static final String  CLASSIC = "Arrow.CLASSIC";
	public static final String  SLEEK   = "Arrow.SLEEK";
	
	protected String m_arrowType;
	protected int m_arrowLength = 4;
	protected int m_arrowWidth  = 10;
	protected Stroke m_arrowStroke;
	
	public Arrow(String type, int length, int width){
		m_arrowType = type;
		if(length>0){
			m_arrowLength = length;
		}
		
		if(width>0){
			m_arrowWidth = width;
		}
		
		m_arrowStroke = new BasicStroke(2);
		
		if(this.m_arrowType == SLEEK){
			arrowhead = getSleekArrow();
		}else{
			arrowhead = getClassicArrow();
		}

	}

	GeneralPath arrowhead;

    
//    public void drawArrow(Graphics2D g2d, double x1, double y1, 
//        double x2, double y2, Shape vertex, boolean directed)
//    {
//        double theta = Math.atan2((y1 - y2), (x1 - x2)) + Math.PI;
//
//        // calculate offset from center of vertex bounding box;
//        // create coordinates for source and dest centered at dest 
//        // (since vertex shape will be centered at dest)
//        Coordinates source = new Coordinates(x1-x2, y1-y2);
//        Coordinates dest = new Coordinates(0,0);
//        Coordinates c = CoordinateUtil.getClosestIntersection(source, dest, vertex.getBounds2D());
//        if (c == null) // can happen if source and dest are the same
//            return;
//        double bounding_box_offset = CoordinateUtil.distance(c, dest);
//
//        // transform arrowhead into dest coordinate space
//        AffineTransform at = new AffineTransform();
//        at.translate(x2, y2);
//        if (directed)
//            theta += Math.atan2(SettableRenderer.CONTROL_OFFSET, 
//                                CoordinateUtil.distance(source,dest)/2);
//        at.rotate(theta);
//        at.translate(-bounding_box_offset, 0);
//
//        // draw the arrowhead
//        Stroke previous = g2d.getStroke();
//        g2d.setStroke(this.m_arrowStroke);
//        g2d.fill(at.createTransformedShape(arrowhead));
//        g2d.setStroke(previous);
//    }
    
	public void drawArrow(Graphics2D g2d, int sourceX, int sourceY, int destX, int destY, int vertexDiam){
	    Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(this.m_arrowStroke);
		Point point1 = new Point(sourceX, sourceY);
		Point point2 = new Point(destX, destY);
		
		// get angle of line from 0 - 360
		double thetaRadians = Math.atan2(( point1.getY() - point2.getY()),(point1.getX() -
				point2.getX()))+Math.PI;
		
//		float distance = (float) point1.distance(point2)-vertexDiam/2.0f;
		AffineTransform at = new AffineTransform();
		at.translate(point2.getX() , point2.getY() );
		at.rotate(thetaRadians);
		at.translate( - vertexDiam / 2.0f, 0 );
		Shape arrow = at.createTransformedShape(arrowhead);
		g2d.fill(arrow);
		g2d.setStroke(oldStroke);
	}
	
	protected GeneralPath getSleekArrow(){
		GeneralPath arrow = new GeneralPath();
//		float distance = 0;
//		(float) point1.distance(point2)-vertexDiam/2.0f;
		// create arrowed line general path
		int width = (int) (m_arrowWidth/2.0f);
		arrow.moveTo( 0, 0);
		arrow.lineTo( (- m_arrowLength), width);
		arrow.lineTo( (- m_arrowLength) , -width);
		arrow.lineTo( 0, 0 );
		return arrow;
	}
	
	protected GeneralPath getClassicArrow(){
		GeneralPath arrow = new GeneralPath();
//		float distance = (float) point1.distance(point2)-vertexDiam/2.0f;
		float distance = 0;
		// create arrowed line general path
		int width = (int) (m_arrowWidth/2.0f);
		arrow.moveTo( distance , 0);
		arrow.lineTo( (distance - m_arrowLength), width);
		arrow.lineTo( (distance - m_arrowLength) , -width);
		arrow.lineTo( distance , 0 );
		return arrow;
	}
}
