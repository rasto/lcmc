/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.visualization;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.commons.collections.Predicate;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.ToolTipFunction;
import edu.uci.ics.jung.graph.decorators.ToolTipFunctionAdapter;
import edu.uci.ics.jung.utils.ChangeEventSupport;
import edu.uci.ics.jung.utils.DefaultChangeEventSupport;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.visualization.transform.LayoutTransformer;
import edu.uci.ics.jung.visualization.transform.MutableAffineTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.Transformer;
import edu.uci.ics.jung.visualization.transform.ViewTransformer;

/**
 * A class that maintains many of the details necessary for creating 
 * visualizations of graphs.
 * 
 * @author Joshua O'Madadhain
 * @author Tom Nelson
 * @author Danyel Fisher
 */
public class VisualizationViewer extends JPanel 
                implements Transformer, LayoutTransformer, ViewTransformer, 
                HasGraphLayout, ChangeListener, ChangeEventSupport{

    protected ChangeEventSupport changeSupport =
        new DefaultChangeEventSupport(this);

    /**
     * holds the state of this View
     */
    protected VisualizationModel model;

	/**
	 * handles the actual drawing of graph elements
	 */
	protected Renderer renderer;
	
	/** should be set to user-defined class to provide
	 * tooltips on the graph elements
	 */
	protected ToolTipFunction toolTipFunction;
	
	/**
	 * rendering hints used in drawing. Anti-aliasing is on
	 * by default
	 */
	protected Map renderingHints = new HashMap();
		
	/**
	 * pluggable support for picking graph elements by
	 * finding them based on their coordinates. Typically
	 * used in mouse events.
	 */
	protected PickSupport pickSupport;
	
	/**
	 * holds the state of which elements of the graph are
	 * currently 'picked'
	 */
	protected PickedState pickedState;
    
    /**
     * a listener used to cause pick events to result in
     * repaints, even if they come from another view
     */
    protected ItemListener pickEventListener;
	
	/**
	 * an offscreen image to render the graph
	 * Used if doubleBuffered is set to true
	 */
	protected BufferedImage offscreen;
	
	/**
	 * graphics context for the offscreen image
	 * Used if doubleBuffered is set to true
	 */
	protected Graphics2D offscreenG2d;
	
	/**
	 * user-settable choice to use the offscreen image
	 * or not. 'false' by default
	 */
	protected boolean doubleBuffered;
	
    /**
     * Provides support for mutating the AffineTransform that
     * is supplied to the rendering Graphics2D
     */
    protected MutableTransformer viewTransformer = 
        new MutableAffineTransformer(new AffineTransform());
    
    protected MutableTransformer layoutTransformer =
        new MutableAffineTransformer(new AffineTransform());
    
	/**
	 * a collection of user-implementable functions to render under
	 * the topology (before the graph is rendered)
	 */
	protected List preRenderers = new ArrayList();
	
	/**
	 * a collection of user-implementable functions to render over the
	 * topology (after the graph is rendered)
	 */
	protected List postRenderers = new ArrayList();
	
    /**
     * provides MouseListener, MouseMotionListener, and MouseWheelListener
     * events to the graph
     */
    protected GraphMouse graphMouse;
    
    /**
     * if true, then when the View is resized, the current Layout
     * is resized to the same size.
     */
//    protected boolean lockLayoutToViewSize;
    
    protected Map locationMap = new HashMap();

    /**
     * Create an instance with passed parameters.
     * 
     * @param layout		The Layout to apply, with its associated Graph
     * @param renderer		The Renderer to draw it with
     */
	public VisualizationViewer(Layout layout, Renderer renderer) {
	    this(new DefaultVisualizationModel(layout), renderer);
	}
	
    /**
     * Create an instance with passed parameters.
     * 
     * @param layout		The Layout to apply, with its associated Graph
     * @param renderer		The Renderer to draw it with
     * @param preferredSize the preferred size of this View
     */
	public VisualizationViewer(Layout layout, Renderer renderer, Dimension preferredSize) {
	    this(new DefaultVisualizationModel(layout, preferredSize), renderer, preferredSize);
	}
	
	/**
	 * Create an instance with passed parameters.
	 * 
	 * @param model
	 * @param renderer
	 */
	public VisualizationViewer(VisualizationModel model, Renderer renderer) {
	    this(model, renderer, new Dimension(600,600));
	}
	/**
	 * Create an instance with passed parameters.
	 * 
	 * @param model
	 * @param renderer
	 * @param preferredSize initial preferred size of the view
	 */
	public VisualizationViewer(VisualizationModel model, Renderer renderer, 
	        Dimension preferredSize) {
	    this.model = model;
	    model.addChangeListener(this);
	    setDoubleBuffered(false);
		this.addComponentListener(new VisualizationListener(this));

		setPickSupport(new ClassicPickSupport());
		setPickedState(new MultiPickedState());
		setRenderer(renderer);
		renderer.setPickedKey(pickedState);
		
		setPreferredSize(preferredSize);
		renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		initMouseClicker();
        scaleToLayout(model.getGraphLayout().getCurrentSize());
        this.layoutTransformer.addChangeListener(this);
        this.viewTransformer.addChangeListener(this);
	}
	
	/**
	 * set whether this class uses its offscreen image or not. If
	 * true, then doubleBuffering in the superclass is set to 'false'
	 */
	public void setDoubleBuffered(boolean doubleBuffered) {
	    this.doubleBuffered = doubleBuffered;
	}
	
	/**
	 * whether this class uses double buffering. The superclass
	 * will be the opposite state.
	 */
	public boolean isDoubleBuffered() {
	    return doubleBuffered;
	}
	
	/**
	 * Ensure that, if doubleBuffering is enabled, the offscreen
	 * image buffer exists and is the correct size.
	 * @param d
	 */
	protected void checkOffscreenImage(Dimension d) {
	    if(doubleBuffered) {
	        if(offscreen == null || offscreen.getWidth() != d.width || offscreen.getHeight() != d.height) {
	            offscreen = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
	            offscreenG2d = offscreen.createGraphics();
	        }
	    }
	}
	
    /**
     * @return Returns the model.
     */
    public VisualizationModel getModel() {
        return model;
    }
    /**
     * @param model The model to set.
     */
    public void setModel(VisualizationModel model) {
        this.model = model;
    }
	/**
	 * In response to changes from the model, repaint the
	 * view, then fire an event to any listeners.
	 * Examples of listeners are the GraphZoomScrollPane and
	 * the BirdsEyeVisualizationViewer
	 */
	public void stateChanged(ChangeEvent e) {
	    repaint();
	    fireStateChanged();
	}
	
	/**
	 * Creates a default mouseClicker behavior: a default.
	 * @see GraphMouseImpl
     * @deprecated replaced by setGraphMouse()
	 */
	protected void initMouseClicker() {
	    // GraphMouseImpl will give original behavior
	    setGraphMouse(new GraphMouseImpl()	);
	}
	
	/**
	 * convenience pass-thru to model
	 * @param scb
	 */
	public void setTextCallback(StatusCallback scb) {
		model.setTextCallback(scb);
	}
	
	/**
	 * a setter for the GraphMouse. This will remove any
	 * previous GraphMouse (including the one that
	 * is added in the initMouseClicker method.
	 * @param graphMouse new value
	 */
	public void setGraphMouse(GraphMouse graphMouse) {
	    this.graphMouse = graphMouse;
	    MouseListener[] ml = getMouseListeners();
	    for(int i=0; i<ml.length; i++) {
	        if(ml[i] instanceof GraphMouse) {
	            removeMouseListener(ml[i]);
	        }
	    }
	    MouseMotionListener[] mml = getMouseMotionListeners();
	    for(int i=0; i<mml.length; i++) {
	        if(mml[i] instanceof GraphMouse) {
	            removeMouseMotionListener(mml[i]);
	        }
	    }
	    MouseWheelListener[] mwl = getMouseWheelListeners();
	    for(int i=0; i<mwl.length; i++) {
	        if(mwl[i] instanceof GraphMouse) {
	            removeMouseWheelListener(mwl[i]);
	        }
	    }
	    addMouseListener(graphMouse);
	    addMouseMotionListener(graphMouse);
	    addMouseWheelListener(graphMouse);
	}
	
	/**
	 * @return the current <code>GraphMouse</code>
	 */
	public GraphMouse getGraphMouse() {
	    return graphMouse;
	}

	/**
	 * Sets the showing Renderer to be the input Renderer. Also
	 * tells the Renderer to refer to this visualizationviewer
	 * as a PickedKey. (Because Renderers maintain a small
	 * amount of state, such as the PickedKey, it is important
	 * to create a separate instance for each VV instance.)
	 */
	public void setRenderer(Renderer r) {
	    this.renderer = r;
	    if(renderer instanceof PluggableRenderer) {
            PluggableRenderer pr = (PluggableRenderer)renderer;
	        pr.setScreenDevice(this);
            pr.setViewTransformer(getViewTransformer());
	        
	        if(pickSupport instanceof ShapePickSupport) {
	            ((ShapePickSupport)pickSupport).setHasShapes((HasShapeFunctions)renderer);
	        }
	    }
	    
	    r.setPickedKey(pickedState);
	    repaint();
	}
	
	/**
	 * Returns the renderer used by this instance.
	 */
	public Renderer getRenderer() {
	    return renderer;
	}

	/**
	 * Removes the current graph layout, and adds a new one.
     * @param layout the new layout to set
	 */
    public void setGraphLayout(Layout layout) {
        setGraphLayout(layout, true);
    }
    /**
     * Removes the current graph layout, and adds a new one,
     * optionally re-scaling the view to show the entire layout
     * @param layout the new layout to set
     * @param scaleToLayout whether to scale the view to show the whole layout
     */
	public void setGraphLayout(Layout layout, boolean scaleToLayout) {

	    Dimension viewSize = getSize();
	    if(viewSize.width <= 0 || viewSize.height <= 0) {
	        viewSize = getPreferredSize();
	    }
	    model.setGraphLayout(layout, viewSize);
        if(scaleToLayout) scaleToLayout(layout.getCurrentSize());
	}
    
    protected void scaleToLayout(Dimension layoutSize) {
        Dimension viewSize = getSize();
        if(viewSize.width == 0 || viewSize.height == 0) {
            viewSize = getPreferredSize();
        }
        float scalex = (float)viewSize.width/layoutSize.width;
        float scaley = (float)viewSize.height/layoutSize.height;
        float scale = 1;
        if(scalex - 1 < scaley - 1) {
        		scale = scalex;
        } else {
        		scale = scaley;
        }
        // set scale to show the entire graph layout
        viewTransformer.setScale(scale, scale, new Point2D.Float());
    }
	
	/**
	 * Returns the current graph layout.
	 * Passes thru to the model
	 */
	public Layout getGraphLayout() {
	        return model.getGraphLayout();
	}
	
	/**
	 * This is the interface for adding a mouse listener. The GEL
	 * will be called back with mouse clicks on vertices.
	 * @param gel
	 */
	public void addGraphMouseListener( GraphMouseListener gel ) {
		addMouseListener( new MouseListenerTranslator( gel, this ));
	}
	
	/**
	 * Pre-relaxes and starts a visRunner thread
	 * Passes thru to the model
	 */
	public synchronized void init() {
	    model.init();
	}

	/**
	 * Restarts layout, then calls init();
	 * passes thru to the model
	 */
	public synchronized void restart() {
	    model.restart();
	}

	/** 
	 * 
	 * @see javax.swing.JComponent#setVisible(boolean)
	 */
	public void setVisible(boolean aFlag) {
		super.setVisible(aFlag);
		model.getGraphLayout().resize(this.getSize());
	}

	/**
	 * convenience pass-thru to the model
	 */
	public void prerelax() {
	    model.prerelax();
	}

	/**
	 * convenience pass-thru to the model
	 */
	protected synchronized void start() {
	    model.start();
	}

	/**
	 * convenience pass-thru to the model
	 *
	 */
	public synchronized void suspend() {
		model.suspend();
	}

	/**
	 * convenience pass-thru to the model
	 *
	 */
	public synchronized void unsuspend() {
	    model.unsuspend();
	}

	/**
     * @deprecated Use <code>getPickedState.isPicked(e)</code>.
	 */
	public boolean isPicked(Vertex v) {
        return pickedState.isPicked(v);
	}
	
	/**
	 * @deprecated Use <code>getPickedState.isPicked(e)</code>.
	 */
	public boolean isPicked(Edge e) {
        return pickedState.isPicked(e);
	}
	
	/**
	 * @deprecated Use <code>getPickedState.pick(picked, b)</code>.
	 */
	protected void pick(Vertex picked, boolean b) 
    {
        pickedState.pick(picked, b);
	}

	long[] relaxTimes = new long[5];
	long[] paintTimes = new long[5];
	int relaxIndex = 0;
	int paintIndex = 0;
	double paintfps, relaxfps;
	
	/**
	 * Returns a flag that says whether the visRunner thread is running. If
	 * it is not, then you may need to restart the thread. 
	 */
	public boolean isVisRunnerRunning() {
	    return model.isVisRunnerRunning();
	}

	/**
	 * setter for the scale
	 * fires a PropertyChangeEvent with the AffineTransforms representing
	 * the previous and new values for scale and offset
     * @deprecated access via getViewTransformer method
	 * @param scalex
	 * @param scaley
	 */
	public void scale(double scalex, double scaley) {
	    scale(scalex, scaley, null);
	}
	/**
	 * have the model scale the graph with the passed parameters.
	 * If 'from' is null, use the center of this View as the
	 * center to scale from
     * @deprecated access via getViewTransformer method
	 * @param scalex
	 * @param scaley
	 * @param from
	 */
	public void scale(double scalex, double scaley, Point2D from) {
        if(from == null) {
            from = getCenter();
        }
        viewTransformer.scale(scalex, scaley, from);
	}
	
	/**
	 * have the model replace the transform scale values with the
	 * passed parameters
     * @deprecated access via getViewTransformer method
	 * @param scalex
	 * @param scaley
	 */
	public void setScale(double scalex, double scaley) {
	    setScale(scalex, scaley, null);
	}
	
	/**
	 * Have the model replace the transform scale values with the
	 * passed parameters. If 'from' is null, use this View's center
	 * as the center to scale from.
     * @deprecated access via getViewTransformer method
	 * @param scalex
	 * @param scaley
	 */
	public void setScale(double scalex, double scaley, Point2D from) {
        viewTransformer.setScale(scalex, scaley, from);
	}
	
	/**
	 * getter for scalex
     * @deprecated access via getViewTransformer method
	 * @return scalex
	 */
	public double getScaleX() {
	    return viewTransformer.getScaleX();	
	}
	
	/**
	 * getter for scaley
     * @deprecated access via getViewTransformer method
	 */
	public double getScaleY() {
	    return viewTransformer.getScaleY();
	}
	
	/**
	 * getter for offsetx
     * @deprecated use getTranslateX
	 */
	public double getOffsetX() {
	    return getTranslateX();
	}
	/**
	 * gets the translateX from the model
     * @deprecated access via getViewTransformer method
	 * @return the translateX
	 */
	public double getTranslateX() {
	    return viewTransformer.getTranslateX();
	}
	
	/**
	 * getter for offsety
	 * @deprecated use getTranslateY()
	 */
	public double getOffsetY() {
	    return getTranslateY();
	}
	
	/**
	 * gets the translateY from the model
     * @deprecated access via getViewTransformer method
	 * @return the translateY
	 */
	public double getTranslateY() {
	    return viewTransformer.getTranslateY();
	}
	
	/**
	 * set the offset values that will be used in the
	 * translation component of the graph rendering transform.
	 * Changes the transform to the identity transform, then
	 * sets the translation conponents to the passed values
	 * Fires a PropertyChangeEvent with the AffineTransforms representing
	 * the previous and new values for the transform
	 * @deprecated use setTranslate(double, offset, double offset)
	 * @param offsetx
	 * @param offsety
	 */
	public void setOffset(double offsetx, double offsety) {
	    setTranslate(offsetx, offsety);
	}
	
	/**
	 * sets the translate x,y in the model
	 * previous translate values are lost
     * @deprecated access via getViewTransformer method
	 * @param tx
	 * @param ty
	 */
	public void setTranslate(double tx, double ty) {
        viewTransformer.setTranslate(tx, ty);
	}
	
	/**
	 * Translates the model's current transform by tX and ty.
     * @deprecated access via getViewTransformer method
	 */
	public void translate(double tx, double ty) {
	    viewTransformer.translate(tx, ty);
	}
	
	/**
	 * Transform the mouse point with the inverse transform
	 * of the VisualizationViewer. This maps from screen coordinates
	 * to graph coordinates.
	 * @param p the point to transform (typically, a mouse point)
	 * @return a transformed Point2D
	 */
	public Point2D inverseTransform(Point2D p) {
	    return layoutTransformer.inverseTransform(inverseViewTransform(p));
	}
	
	public Point2D inverseViewTransform(Point2D p) {
	    return viewTransformer.inverseTransform(p);
	}

    public Point2D inverseLayoutTransform(Point2D p) {
        return layoutTransformer.inverseTransform(p);
    }

	/**
	 * Transform the mouse point with the current transform
	 * of the VisualizationViewer. This maps from graph coordinates
	 * to screen coordinates.
	 * @param p the point to transform
	 * @return a transformed Point2D
	 */
	public Point2D transform(Point2D p) {
	    // transform with vv transform
	    return viewTransformer.transform(layoutTransform(p));
	}
    
    public Point2D viewTransform(Point2D p) {
        return viewTransformer.transform(p);
    }
    
    public Point2D layoutTransform(Point2D p) {
        return layoutTransformer.transform(p);
    }
    
    /**
     * @param transformer The transformer to set.
     */
    public void setViewTransformer(MutableTransformer transformer) {
        this.viewTransformer.removeChangeListener(this);
        this.viewTransformer = transformer;
        this.viewTransformer.addChangeListener(this);
        if(renderer instanceof PluggableRenderer) {
            ((PluggableRenderer)renderer).setViewTransformer(transformer);
        }
    }

    public void setLayoutTransformer(MutableTransformer transformer) {
        this.layoutTransformer.removeChangeListener(this);
        this.layoutTransformer = transformer;
        this.layoutTransformer.addChangeListener(this);
    }

    public MutableTransformer getViewTransformer() {
        return viewTransformer;
    }

    public MutableTransformer getLayoutTransformer() {
        return layoutTransformer;
    }

    /**
     * @return Returns the renderingHints.
     */
    public Map getRenderingHints() {
        return renderingHints;
    }
    /**
     * @param renderingHints The renderingHints to set.
     */
    public void setRenderingHints(Map renderingHints) {
        this.renderingHints = renderingHints;
    }
    
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);

	    checkOffscreenImage(getSize());
		model.start();

		Graphics2D g2d = (Graphics2D)g;
		if(doubleBuffered) {
			renderGraph(offscreenG2d);
		    g2d.drawImage(offscreen, null, 0, 0);
		} else {
		    renderGraph(g2d);
		}
	}
	
	protected void renderGraph(Graphics2D g2d) {

	    Layout layout = model.getGraphLayout();

		g2d.setRenderingHints(renderingHints);
		
		long start = System.currentTimeMillis();
		
		// the size of the VisualizationViewer
		Dimension d = getSize();
		
		// clear the offscreen image
		g2d.setColor(getBackground());
		g2d.fillRect(0,0,d.width,d.height);

		AffineTransform oldXform = g2d.getTransform();
        AffineTransform newXform = new AffineTransform(oldXform);
        newXform.concatenate(viewTransformer.getTransform());
		
        g2d.setTransform(newXform);

		// if there are  preRenderers set, paint them
		for(Iterator iterator=preRenderers.iterator(); iterator.hasNext(); ) {
		    Paintable paintable = (Paintable)iterator.next();
		    if(paintable.useTransform()) {
		        paintable.paint(g2d);
		    } else {
		        g2d.setTransform(oldXform);
		        paintable.paint(g2d);
                g2d.setTransform(newXform);
		    }
		}
		
        locationMap.clear();
        
		// paint all the edges
        try {
		for (Iterator iter = layout.getGraph().getEdges().iterator();
		iter.hasNext();
		) {
		    Edge e = (Edge) iter.next();
		    Vertex v1 = (Vertex) e.getEndpoints().getFirst();
		    Vertex v2 = (Vertex) e.getEndpoints().getSecond();
            
            Point2D p = (Point2D) locationMap.get(v1);
            if(p == null) {
                
                p = layout.getLocation(v1);
                p = layoutTransformer.transform(p);
                locationMap.put(v1, p);
            }
		    Point2D q = (Point2D) locationMap.get(v2);
            if(q == null) {
                q = layout.getLocation(v2);
                q = layoutTransformer.transform(q);
                locationMap.put(v2, q);
            }

		    if(p != null && q != null) {
		        renderer.paintEdge(
		                g2d,
		                e,
		                (int) p.getX(),
		                (int) p.getY(),
		                (int) q.getX(),
		                (int) q.getY());
		    }
		}
        } catch(ConcurrentModificationException cme) {
            repaint();
        }
		
		// paint all the vertices
        try {
		for (Iterator iter = layout.getGraph().getVertices().iterator();
		iter.hasNext();
		) {
            
		    Vertex v = (Vertex) iter.next();
		    Point2D p = (Point2D) locationMap.get(v);
            if(p == null) {
                p = layout.getLocation(v);
                p = layoutTransformer.transform(p);
                locationMap.put(v, p);
            }
		    if(p != null) {
		        renderer.paintVertex(
		                g2d,
		                v,
		                (int) p.getX(),
		                (int) p.getY());
		    }
		}
        } catch(ConcurrentModificationException cme) {
            repaint();
        }
		
		long delta = System.currentTimeMillis() - start;
		paintTimes[paintIndex++] = delta;
		paintIndex = paintIndex % paintTimes.length;
		paintfps = average(paintTimes);
		
		// if there are postRenderers set, do it
		for(Iterator iterator=postRenderers.iterator(); iterator.hasNext(); ) {
		    Paintable paintable = (Paintable)iterator.next();
		    if(paintable.useTransform()) {
		        paintable.paint(g2d);
		    } else {
		        g2d.setTransform(oldXform);
		        paintable.paint(g2d);
                g2d.setTransform(newXform);
		    }
		}
		g2d.setTransform(oldXform);
	}

	/**
	 * Returns the double average of a number of long values.
	 * @param paintTimes	an array of longs
	 * @return the average of the doubles
	 */
	protected double average(long[] paintTimes) {
		double l = 0;
		for (int i = 0; i < paintTimes.length; i++) {
			l += paintTimes[i];
		}
		return l / paintTimes.length;
	}

	/**
	 * VisualizationListener reacts to changes in the size of the
	 * VisualizationViewer. When the size changes, it ensures
	 * that the offscreen image is sized properly. 
	 * If the layout is locked to this view size, then the layout
	 * is also resized to be the same as the view size.
	 *
	 *
	 */
	protected class VisualizationListener extends ComponentAdapter {
		protected VisualizationViewer vv;
		public VisualizationListener(VisualizationViewer vv) {
			this.vv = vv;
		}

		/**
		 * create a new offscreen image for the graph
		 * whenever the window is resied
		 */
		public void componentResized(ComponentEvent e) {
		    Dimension d = vv.getSize();
		    if(d.width <= 0 || d.height <= 0) return;
		    checkOffscreenImage(d);
	    //    	if(getLockLayoutToViewSize()) {
	        	//    model.resizeLayout(vv.getSize());
	    //    	}
	        	repaint();
		}
	}

	/**
	 * convenience pass-thru to model
	 */
	public synchronized void stop() {
	    model.stop();
	}

	/**
	 * sets the tooltip listener to the user's defined implementation
	 * of ToolTipListener
	 * @param listener the listener to ser
	 */
    public void setToolTipListener(ToolTipListener listener) {
        if(listener instanceof ToolTipFunction) {
            setToolTipFunction((ToolTipFunction)listener);
        } else {
            setToolTipFunction(new ToolTipListenerWrapper(listener));
        }
    }

    public void setToolTipFunction(ToolTipFunction toolTipFunction) {
        this.toolTipFunction = toolTipFunction;
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    /**
     * called by the superclass to display tooltips
     */
    public String getToolTipText(MouseEvent event) {
        if(toolTipFunction != null) {
            if(toolTipFunction instanceof ToolTipListenerWrapper) {
                return toolTipFunction.getToolTipText(event);
            } 
            Point2D p = inverseViewTransform(event.getPoint());
            Vertex vertex = pickSupport.getVertex(p.getX(), p.getY());
            if(vertex != null && willRender(vertex)) {
                return toolTipFunction.getToolTipText(vertex);
            }
            Edge edge = pickSupport.getEdge(p.getX(), p.getY());
            if(edge != null && willRender(edge)) {
                return toolTipFunction.getToolTipText(edge);
            }
            return toolTipFunction.getToolTipText(event);
        }
        return super.getToolTipText(event);
    }
    
    private boolean willRender(Vertex v) {
    	if(renderer instanceof PluggableRenderer) {
    		Predicate vip = ((PluggableRenderer)renderer).getVertexIncludePredicate();
    		return vip == null || vip.evaluate(v) == true;
      	} 
    	return true;
    }

    private boolean willRender(Edge e) {
    	if(renderer instanceof PluggableRenderer) {
    		Predicate eip = ((PluggableRenderer)renderer).getEdgeIncludePredicate();
    		Pair endpoints = e.getEndpoints();
    		boolean edgeAnswer = eip == null || eip.evaluate(e);
    		boolean endpointAnswer = willRender((Vertex)endpoints.getFirst()) && 
    			willRender((Vertex)endpoints.getSecond());
    		return edgeAnswer && endpointAnswer;
      	} 
    	return true;
    }

	/**
	 * The interface for the tool tip listener. Implement this
	 * interface to add custom tool tip to the graph elements.
	 * See sample code for examples
	 */
    public interface ToolTipListener {
        	String getToolTipText(MouseEvent event);
    }
    
    /**
     * used internally to wrap any legacy ToolTipListener
     * implementations so they can be used as a ToolTipFunction
     * @author Tom Nelson - RABA Technologies
     *
     *
     */
    protected static class ToolTipListenerWrapper extends ToolTipFunctionAdapter {
        ToolTipListener listener;
        public ToolTipListenerWrapper(ToolTipListener listener) {
            this.listener = listener;
        }
        public String getToolTipText(MouseEvent e) {
            return listener.getToolTipText(e);
        }
    }
    
    /**
     * an interface for the preRender and postRender
     */
    public interface Paintable {
        public void paint(Graphics g);
        public boolean useTransform();
    }

    /**
     * a convenience type to represent a class that
     * processes all types of mouse events for the graph
     */
    public interface GraphMouse extends MouseListener, MouseMotionListener, MouseWheelListener {}
    
    /** 
     * this is the original GraphMouse class, renamed to use GraphMouse as the interface name,
     * and updated to correctly apply the vv transform to the point point
     *
     */
    protected final class GraphMouseImpl extends MouseAdapter implements GraphMouse {
        protected Vertex picked;
        
        public void mousePressed(MouseEvent e) {
            
            Point2D p = inverseViewTransform(e.getPoint());

            Vertex v = pickSupport.getVertex(p.getX(), p.getY());
            if (v == null) {
                return;
            }
            picked = v;
            pick(picked, true);
            model.getGraphLayout().forceMove(picked, p.getX(), p.getY());
            repaint();
        }
        public void mouseReleased(MouseEvent e) {
            if (picked == null)
                return;
            pick(picked, false);
            picked = null;
            repaint();
        }
        public void mouseDragged(MouseEvent e) {
            if (picked == null)
                return;
            Point2D p = inverseViewTransform(e.getPoint());

            model.getGraphLayout().forceMove(picked, p.getX(), p.getY());
            repaint();
        }
        
        public void mouseMoved(MouseEvent e) {
            return;
        }
        /**
         * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
         */
        public void mouseWheelMoved(MouseWheelEvent e) {
            return;
        }
    }
    
    /**
     * @param paintable The paintable to add.
     */
    public void addPreRenderPaintable(Paintable paintable) {
        if(preRenderers == null) {
            preRenderers = new ArrayList();
        }
        preRenderers.add(paintable);
    }
    
    /**
     * @param paintable The paintable to remove.
     */
    public void removePreRenderPaintable(Paintable paintable) {
        if(preRenderers != null) {
            preRenderers.remove(paintable);
        }
    }
    
    /**
     * @param paintable The paintable to add.
     */
    public void addPostRenderPaintable(Paintable paintable) {
        if(postRenderers == null) {
            postRenderers = new ArrayList();
        }
        postRenderers.add(paintable);
    }
    
    /**
     * @param paintable The paintable to remove.
     */
   public void removePostRenderPaintable(Paintable paintable) {
        if(postRenderers != null) {
            postRenderers.remove(paintable);
        }
    }

    /**
     * Adds a <code>ChangeListener</code>.
     * @param l the listener to be added
     */
    public void addChangeListener(ChangeListener l) {
        changeSupport.addChangeListener(l);
    }
    
    /**
     * Removes a ChangeListener.
     * @param l the listener to be removed
     */
    public void removeChangeListener(ChangeListener l) {
        changeSupport.removeChangeListener(l);
    }
    
    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public ChangeListener[] getChangeListeners() {
        return changeSupport.getChangeListeners();
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created.
     * @see EventListenerList
     */
    public void fireStateChanged() {
        changeSupport.fireStateChanged();
    }   
    
    /**
     * @return Returns the pickedState.
     */
    public PickedState getPickedState() {
        return pickedState;
    }
    /**
     * @param pickedState The pickedState to set.
     */
    public void setPickedState(PickedState pickedState) {
        if(pickEventListener != null && this.pickedState != null) {
            this.pickedState.removeItemListener(pickEventListener);
        }
        this.pickedState = pickedState;
        if(renderer != null) {
            renderer.setPickedKey(pickedState);
        }
        if(pickEventListener == null) {
            pickEventListener = new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    repaint();
                }
            };
        }
        pickedState.addItemListener(pickEventListener);
    }
    
    /**
     * @return Returns the pickSupport.
     */
    public PickSupport getPickSupport() {
        return pickSupport;
    }
    /**
     * @param pickSupport The pickSupport to set.
     */
    public void setPickSupport(PickSupport pickSupport) {
        this.pickSupport = pickSupport;
        this.pickSupport.setHasGraphLayout(this);
        if(pickSupport instanceof ShapePickSupport && renderer instanceof HasShapeFunctions) {
            ((ShapePickSupport)pickSupport).setHasShapes((HasShapeFunctions)renderer);
            ((ShapePickSupport)pickSupport).setLayoutTransformer(this);
        }
    }
    
    public Point2D getCenter() {
        Dimension d = getSize();
        return new Point2D.Float(d.width/2, d.height/2);
    }
}
