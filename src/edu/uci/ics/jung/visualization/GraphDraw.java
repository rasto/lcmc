/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.ConstantEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.EdgeColorFunction;
import edu.uci.ics.jung.graph.decorators.EdgeThicknessFunction;
import edu.uci.ics.jung.graph.decorators.PickableVertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.VertexColorFunction;
import edu.uci.ics.jung.graph.filters.Filter;
import edu.uci.ics.jung.graph.filters.LevelFilter;
import edu.uci.ics.jung.graph.filters.SerialFilter;
import edu.uci.ics.jung.graph.filters.impl.DropSoloNodesFilter;

/**
 * A Swing-only component for drawing graphs. Allows a series of manipulations
 * to access and show graphs, to set their various colors and lines, and to
 * dynamically change values. This is a good starting place for getting a graph
 * up quickly.
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * @deprecated As of version 1.7, use <code>VisualizationViewer</code> directly instead.
 */
public class GraphDraw extends JComponent implements StatusCallback {

	protected VisualizationViewer vv;
	protected Filter mainFilter;

	protected List allFilters = new LinkedList();
	protected List sliders = new LinkedList();

	protected JPanel toolbar;
	protected JLabel statusbar;

	/**
	 * Creates a graph drawing environment that draws this graph object. By
	 * default, uses the Spring layout, the Fade renderer and the
	 * AbstractSettable renderer, the Drop Solo Nodes filter, and no adjustable
	 * filters at all. By default, now HIDES the status bar; call showStatus()
	 * to show it. 
	 * 
	 * @param g
	 */
	public GraphDraw(Graph g) {
        this(new SpringLayout(g));
	}

    /**
     * Creates a graph drawing environment with the specified layout algorithm
     * @param layout
     */
	public GraphDraw(Layout layout)
    {
        Graph g = layout.getGraph();
        StringLabeller sl = StringLabeller.getLabeller(g);
        PluggableRenderer pr = new PluggableRenderer();
        pr.setVertexStringer(sl);
        vv = new VisualizationViewer(layout, pr);
        setLayout(new BorderLayout());
        add(vv, BorderLayout.CENTER);

        toolbar = new JPanel();
        add(toolbar, BorderLayout.WEST);
        toolbar.setVisible(false);
        
        statusbar = new JLabel(" ");
        add(statusbar, BorderLayout.SOUTH);
        vv.setTextCallback(this);
        hideStatus();

        mainFilter = DropSoloNodesFilter.getInstance();
    }
    
    /**
	 * Returns the visualizationviewer that actually does the graph drawing.
	 */
	public VisualizationViewer getVisualizationViewer() {
		return vv;
	}
	
	/**
	 * This is the interface for adding a mouse listener. The GEL
	 * will be called back with mouse clicks on vertices.
	 * @param gel
     * @deprecated Use getVisualizationViewer.addMouseListener( new MouseListenerTranslator(gel, vv));
	 */
	public void addGraphMouseListener( GraphMouseListener gel ) {
		vv.addMouseListener( new MouseListenerTranslator( gel, vv ));
	}
	
	/**
	 * Shows the status bar at bottom left
	 */
	public void showStatus() {
		statusbar.setVisible(true);
	}

	/**
	 * Hides the status bar at bottom left
	 */
	public void hideStatus() {
		statusbar.setVisible(false);
	}

	public void setBackground(Color bg) {
		super.setBackground(bg);
		vv.setBackground(bg);
	}

	public void callBack(String status) {
		statusbar.setText(status);
	}

	/**
	 * A method to set the renderer.
	 * 
	 * @param r the new renderer
     * @deprecated Use getVisualizationViewer().setRenderer(r) instead.
	 */
	public void setRenderer(Renderer r) {
		vv.setRenderer(r);
	}

    /**
     * Resets the renderer to its original state.
     *
     * @deprecated
     */
	public void resetRenderer() {
        PluggableRenderer pr = new PluggableRenderer();
        pr.setVertexStringer(StringLabeller.getLabeller(vv.getGraphLayout().getGraph()));
        vv.setRenderer(pr);
	}

    /**
     * @deprecated As of version 1.5.2, replaced by getRenderer.
     */
	public Renderer getRender() {
        return vv.getRenderer();
	}

    /**
     * Returns the renderer currently in use.
     * 
     * @deprecated Use getVisualizationViewer.getRenderer() instead.
     */
    public Renderer getRenderer()
    {
        return vv.renderer;
    }
    
	/**
	 * A passthrough to the renderer used by this instance of 
     * <code>VisualizationViewer</code>.  Has no effect if 
     * the renderer is not an instance of <code>PluggableRenderer</code>.
	 * 
	 * @param c the new edge color
     * @deprecated Modify this property via the renderer instead.
	 */
	public void setEdgeColor(Color c) {
        Renderer r = vv.getRenderer();
        if (! (r instanceof PluggableRenderer))
            return;
        PluggableRenderer pr = (PluggableRenderer)r;
        pr.setEdgePaintFunction(new ConstantEdgePaintFunction(c, null));
	}

	/**
     * A passthrough to the renderer used by this instance of 
     * <code>VisualizationViewer</code>.  Has no effect if 
     * the renderer is not an instance of <code>PluggableRenderer</code>.
	 * 
	 * @param ecf
	 *            the new <code>EdgeColorFunction</code>
     * @deprecated Modify this property via the renderer instead.
	 */
	public void setEdgeColorFunction(EdgeColorFunction ecf) {
        Renderer r = vv.getRenderer();
        if (! (r instanceof PluggableRenderer))
            return;
        PluggableRenderer pr = (PluggableRenderer)r;
        pr.setEdgePaintFunction(new EdgeColorToEdgePaintFunctionConverter(ecf));
	}

	/**
     * A passthrough to the renderer used by this instance of 
     * <code>VisualizationViewer</code>.  Has no effect if 
     * the renderer is not an instance of <code>PluggableRenderer</code>.
	 * 
	 * @param i
	 *            the thickness of the edge
     * @deprecated Modify this property via the renderer instead.
	 */
	public void setEdgeThickness(int i) 
    {
        Renderer r = vv.getRenderer();
        if (! (r instanceof PluggableRenderer))
            return;
        PluggableRenderer pr = (PluggableRenderer)r;
        pr.setEdgeStrokeFunction(new ConstantEdgeStrokeFunction(new BasicStroke(i)));
	}

	/**
     * A passthrough to the renderer used by this instance of 
     * <code>VisualizationViewer</code>.  Has no effect if 
     * the renderer is not an instance of <code>PluggableRenderer</code>.
	 * 
	 * @param etf
	 *            the new <code>EdgeThicknessFunction</code>
     * @deprecated Modify this property via the renderer instead.
	 */
	public void setEdgeThicknessFunction(EdgeThicknessFunction etf) {
        Renderer r = vv.getRenderer();
        if (! (r instanceof PluggableRenderer))
            return;
        PluggableRenderer pr = (PluggableRenderer)r;
        pr.setEdgeStrokeFunction(new EdgeThicknessToEdgeStrokeFunctionConverter(etf));
	}

	/**
     * A passthrough to the renderer used by this instance of 
     * <code>VisualizationViewer</code>.  Has no effect if 
     * the renderer is not an instance of <code>PluggableRenderer</code>.
     * Resets the background (fill) and the picked color to Color.RED
     * and Color.ORANGE, respectively; to modify all of these at once,
     * access the renderer directly (see <code>PickableVertexPaintFunction</code>). 
	 * 
	 * @param vertexColor the new foreground (draw) color of the vertices
     * @deprecated Modify this property via the renderer instead.
	 */
	public void setVertexForegroundColor(Color vertexColor) {
        Renderer r = vv.getRenderer();
        if (! (r instanceof PluggableRenderer))
            return;
        PluggableRenderer pr = (PluggableRenderer)r;
        pr.setVertexPaintFunction(new PickableVertexPaintFunction(vv.getPickedState(), vertexColor, Color.RED, Color.ORANGE));
	}

	/**
     * A passthrough to the renderer used by this instance of 
     * <code>VisualizationViewer</code>.  Has no effect if 
     * the renderer is not an instance of <code>PluggableRenderer</code>.
     * Resets the background (fill) and the foreground (draw) color to Color.RED
     * and Color.BLACK, respectively; to modify all of these at once,
     * access the renderer directly (see <code>PickableVertexPaintFunction</code>). 
	 * 
	 * @param vertexColor the new picked color of the vertices
     * @deprecated Modify this property via the renderer instead.
	 */
	public void setVertexPickedColor(Color vertexColor) {
        Renderer r = vv.getRenderer();
        if (! (r instanceof PluggableRenderer))
            return;
        PluggableRenderer pr = (PluggableRenderer)r;
        pr.setVertexPaintFunction(new PickableVertexPaintFunction(vv.getPickedState(), Color.BLACK, Color.RED, vertexColor));
	}

	/**
     * A passthrough to the renderer used by this instance of 
     * <code>VisualizationViewer</code>.  Has no effect if 
     * the renderer is not an instance of <code>PluggableRenderer</code>.
     * Resets the picked color and the foreground (draw) color to Color.ORANGE
     * and Color.BLACK, respectively; to modify all of these at once,
     * access the renderer directly (see <code>PickableVertexPaintFunction</code>). 
	 * 
	 * @param vertexColor
	 *            the background color of the vertex that is to be set
     * @deprecated Modify this property via the renderer instead.
	 */
	public void setVertexBGColor(Color vertexColor) {
        Renderer r = vv.getRenderer();
        if (! (r instanceof PluggableRenderer))
            return;
        PluggableRenderer pr = (PluggableRenderer)r;
        pr.setVertexPaintFunction(new PickableVertexPaintFunction(vv.getPickedState(), Color.BLACK, vertexColor, Color.ORANGE));
	}

	/**
     * A passthrough to the renderer used by this instance of 
     * <code>VisualizationViewer</code>.  Has no effect if 
     * the renderer is not an instance of <code>PluggableRenderer</code>.
	 * 
	 * @param vcf
	 *            the new <code>VertexColorFunction</code>
     * @deprecated Modify this property via the renderer instead.
	 */
	public void setVertexColorFunction(VertexColorFunction vcf) {
        Renderer r = vv.getRenderer();
        if (! (r instanceof PluggableRenderer))
            return;
        PluggableRenderer pr = (PluggableRenderer)r;
        pr.setVertexPaintFunction(new VertexColorToVertexPaintConverter(vcf));
	}

	/**
	 * Replaces the layout used by the VisualizationViewer with <code>l</code>.
	 * 
	 * @param l
	 *            the new graph layout algorithm
     * @deprecated Use getVisualizationViewer.setGraphLayout(l) instead.
	 */
	public void setGraphLayout(Layout l) {
		vv.setGraphLayout(l);
	}

	/**
	 * Removes all the filters, deleting the sliders that drive them.
     * @deprecated Use the PluggableRenderer's vertex and edge visibility
     * predicates instead.  See the release notes for version 1.6 for more
     * information.
	 */
	public void removeAllFilters() {
		toolbar.removeAll();
        toolbar.setVisible(false);
		sliders.clear();
		allFilters.clear();
		mainFilter = new SerialFilter(allFilters);
	}

	/**
	 * Adds a Filter that doesn't slide.
	 * 
	 * @param f
     * @deprecated Use the PluggableRenderer's vertex and edge visibility
     * predicates instead.  See the release notes for version 1.6 for more
     * information.
	 */
	public void addStaticFilter(Filter f) {
		allFilters.add(f);
		mainFilter = new SerialFilter(allFilters);
	}

	/**
	 * Creates a new slider based off of a <tt>LevelFilter</tt>. The
	 * function adds the <tt>Filter</tt> into the sequence of filters
	 * preserved by the current visualization, creates a JSlider to go with it,
	 * and then returns it.
	 * <p>
	 * TODO: The situation may not be entirely right until applyFilter has been
	 * called.
	 * 
	 * @param l
	 *            The Filter to use.
	 * @param low
	 *            The low value on the filter: this will be the low point on
	 *            the slider
	 * @param high
	 *            The high value on the filter: this will be the high point on
	 *            the slider
	 * @param defaultVal
	 *            The starting point on the filter
	 * @return the slider (which will have been also added to the Sliders
	 *         panel)
     * @deprecated Use the PluggableRenderer's vertex and edge visibility
     * predicates instead.  See the release notes for version 1.6 for more
     * information.
	 */
	public JSlider addSlider(LevelFilter l, int low, int high, int defaultVal) {
		JSlider js = new JSlider(JSlider.VERTICAL, low, high, defaultVal);
		l.setValue(defaultVal);
		sliders.add(js);
		toolbar.add(js);
        toolbar.setVisible(true);
		js.addChangeListener(new SliderChangeListener(this, js, l));
		allFilters.add(l);
		mainFilter = new SerialFilter(allFilters);
		return js;
	}

	/**
	 * Adds a tool to the toolbar.
	 * 
	 * @param jc
	 *            the tool--any JComponent--to be added to the Toolbar on the
	 *            left side
     * @deprecated Use the PluggableRenderer's vertex and edge visibility
     * predicates instead.  See the release notes for version 1.6 for more
     * information.
	 */
	public void addTool(JComponent jc) {
		toolbar.add(jc);
        toolbar.setVisible(true);
	}

	protected class SliderChangeListener implements ChangeListener {
		protected LevelFilter levelFilter;
		protected JSlider js;
		protected GraphDraw gd;

		int oldValue = -1;

		public SliderChangeListener(GraphDraw gd, JSlider js, LevelFilter l) {
			this.levelFilter = l;
			this.js = js;
			this.gd = gd;
		}

		public void stateChanged(ChangeEvent e) {
			int val = js.getValue();
			if (oldValue == val) {
				return;
			}
			oldValue = val;
			levelFilter.setValue(val);
            Layout l = gd.getVisualizationViewer().getGraphLayout();
			Graph fg = gd.mainFilter.filter(l.getGraph()).assemble();
			l.applyFilter(fg);
		}

	}

    /**
	 * Returns the currently operative layout.
     * @deprecated Use <code>getVisualizationViewer().getGraphLayout()</code> instead.
	 */
	public Layout getGraphLayout() {
        return vv.getGraphLayout();
	}

	/**
	 * This is the "scramble" button--it resets the layout.
     * @deprecated Use <code>getVisualizationViewer().restart()</code> instead.
	 */
	public void restartLayout() {
		vv.restart();
	}

	/**
     * @deprecated Use <code>getVisualizationViewer().stop()</code> instead.
	 */
	public void stop() {
		vv.stop();
	}
}
