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

/**
 * An extended interface for signalling a layout that the
 * underlying graph has been changed. The complete sequence for 
 * updating a graph is:
 * <pre>
 *  visualizationViewer = graphdraw.getVisualization();
 *  layoutMut = (LaoyutMutable) visualizationViewer.getLayout();
 *	visualizationViewer.suspend();
 *  // make your changes to the graph here
 * 	graph.addVertex(new SparseVertex());
 * 
 *	layoutMut.update();
 *	visualizationViewer.unsuspend();
 *	graphDraw.repaint()
 * </pre>
 * 
 * @author winterf@sourceforge.net
 */
public interface LayoutMutable extends Layout {

		void update();
}
