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
 * Created on Feb 17, 2004
 */
package edu.uci.ics.jung.visualization;

import java.awt.event.MouseEvent;

import edu.uci.ics.jung.graph.Vertex;

/**
 * This interface allows users to register listeners to register to receive
 * vertex clicks.
 * 
 * @author danyelf
 */
public interface GraphMouseListener {

	void graphClicked(Vertex v, MouseEvent me);
	void graphPressed(Vertex v, MouseEvent me);
	void graphReleased(Vertex v, MouseEvent me);

}
