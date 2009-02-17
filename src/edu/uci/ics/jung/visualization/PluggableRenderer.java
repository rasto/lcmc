/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.CellRendererPane;
import javax.swing.Icon;
import javax.swing.JComponent;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.TruePredicate;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.ConstantDirectionalEdgeValue;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeFontFunction;
import edu.uci.ics.jung.graph.decorators.ConstantEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeStringer;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.ConstantVertexAspectRatioFunction;
import edu.uci.ics.jung.graph.decorators.ConstantVertexFontFunction;
import edu.uci.ics.jung.graph.decorators.ConstantVertexSizeFunction;
import edu.uci.ics.jung.graph.decorators.ConstantVertexStringer;
import edu.uci.ics.jung.graph.decorators.ConstantVertexStrokeFunction;
import edu.uci.ics.jung.graph.decorators.DirectionalEdgeArrowFunction;
import edu.uci.ics.jung.graph.decorators.EdgeArrowFunction;
import edu.uci.ics.jung.graph.decorators.EdgeColorFunction;
import edu.uci.ics.jung.graph.decorators.EdgeFontFunction;
import edu.uci.ics.jung.graph.decorators.EdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.EdgeShapeFunction;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.EllipseVertexShapeFunction;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.PickableVertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexColorFunction;
import edu.uci.ics.jung.graph.decorators.VertexFontFunction;
import edu.uci.ics.jung.graph.decorators.VertexIconFunction;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexShapeFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.decorators.VertexStrokeFunction;
import edu.uci.ics.jung.graph.predicates.EdgePredicate;
import edu.uci.ics.jung.graph.predicates.SelfLoopEdgePredicate;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.ParallelEdgeIndexFunction;
import edu.uci.ics.jung.utils.ParallelEdgeIndexSingleton;
import edu.uci.ics.jung.visualization.transform.MutableAffineTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

/**
 * <p>A renderer with all sorts of buttons to press and dials to turn.
 * Using the appropriate methods, the user can override the default 
 * properties/behaviors for vertex paint, stroke, shape, label, label font,
 * and label centering; and for edge paint, stroke, label, arrows, label font,
 * label positioning, and drawing.
 * </p>
 * <p>Notes on these decorators: 
 * <ul>
 * <li/>The decorators are all orthogonal; changing one does not change the behavior of any
 * other (unless your decorator implementations depend on one another).
 * <li/>The default properties apply to each vertices/edges, but the decorators allow these 
 * properties to be specified individually for each vertex/edge.  See the documentation for 
 * each of these decorators for specific instructions on their use. 
 * <li/>Implementations of these decorator interfaces are provided that allow the user
 * to specify a single (constant) property to apply to all vertices/edges.  
 * <li/>There are additional interfaces and classes that allow the size and aspect ratio 
 * of the vertex shape to be independently manipulated, and that provide factory methods
 * for generating various standard shapes; see <code>SettableVertexShapeFunction</code>,
 * <code>AbstractVertexShapeFunction</code>, <code>VertexShapeFactory</code>, and the 
 * sample <code>samples.graph.PluggableRendererDemo</code>.
 * <li/>This class provides default <code>Stroke</code> implementations for dotted and
 * dashed lines: the <code>DOTTED</code> and <code>DASHED</code> static constants,
 * respectively.
 * <li/>The <code>EdgeArrowPredicate</code> specifies the edges for which arrows
 * should be drawn; the <code>EdgeArrowFunction</code> specifies the shapes of
 * the arrowheads for those edges that pass the <code>EdgeArrowPredicate</code>.
 * <li/>If the specified vertex inclusion <code>Predicate</code> indicates that
 * vertex <code>v</code> is not to be drawn, none of its incident edges will be drawn either.
 * </ul>
 * 
 * <p>By default, self-loops are drawn as circles.</p>
 * 
 * <p>By default, undirected edges are drawn as straight lines, directed edges are 
 * drawn as bent lines, and parallel edges are drawn on top
 * of one another.</p>

 * <p>Arrowheads are drawn so that the point of the arrow is at the boundary of the 
 * vertex shapes. In the current implementation, finding the place
 * where arrows are drawn is fairly slow; for large graphs, this should
 * be turned off.</p>
 * 
 * <p>Setting a stroke width other than 1, or using transparency, 
 * may slow down rendering of the visualization.
 * </p>
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * @author Tom Nelson
 */
public class PluggableRenderer extends AbstractRenderer implements PickedInfo, HasShapeFunctions
{
    
	protected float arrow_placement_tolerance = 1;
    protected final static float[] dotting = {1.0f, 3.0f};
    /**
     * A stroke for a dotted line: 1 pixel width, round caps, round joins, and an 
     * array of {1.0f, 3.0f}.
     */
    public final static Stroke DOTTED = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
            BasicStroke.JOIN_ROUND, 1.0f, dotting, 0f);

    protected final static float[] dashing = {5.0f};
    /**
     * A stroke for a dashed line: 1 pixel width, square caps, beveled joins, and an
     * array of {5.0f}.
     */
    public final static Stroke DASHED = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_BEVEL, 1.0f, dashing, 0f);
    
    /**
     * Specifies the offset for the edge labels.
     */
    public static final int LABEL_OFFSET = 10;

    /**
     * Specifies the maximum number of iterations to run the edge subdivision loop
     * in <code>getLastOutsideSegment</code>; this is done to fix the arrow 
     * orientation problem noted in bug report #1450529.
     */
    protected static final int MAX_ITERATIONS = 10;
    
    protected Predicate vertexIncludePredicate = TruePredicate.getInstance();
    protected VertexStrokeFunction vertexStrokeFunction =
        new ConstantVertexStrokeFunction(1.0f);
    protected VertexShapeFunction vertexShapeFunction = 
        new EllipseVertexShapeFunction(
                new ConstantVertexSizeFunction(20),
                new ConstantVertexAspectRatioFunction(1.0f));
    protected VertexStringer vertexStringer = 
        new ConstantVertexStringer(null);
    protected VertexIconFunction vertexIconFunction;
    protected VertexFontFunction vertexFontFunction = 
        new ConstantVertexFontFunction(new Font("Helvetica", Font.PLAIN, 12));
    protected boolean centerVertexLabel = false;
    
    protected VertexPaintFunction vertexPaintFunction =
        new PickableVertexPaintFunction(this, Color.BLACK, Color.RED, Color.ORANGE);
    
    protected EdgeStringer edgeStringer = 
        new ConstantEdgeStringer(null);
    protected EdgeStrokeFunction edgeStrokeFunction = 
        new ConstantEdgeStrokeFunction(1.0f);
    protected EdgeArrowFunction edgeArrowFunction = 
        new DirectionalEdgeArrowFunction(10, 8, 4);    
    protected Predicate edgeArrowPredicate = Graph.DIRECTED_EDGE; 
    protected Predicate edgeIncludePredicate = TruePredicate.getInstance();
    protected EdgeFontFunction edgeFontFunction =
        new ConstantEdgeFontFunction(new Font("Helvetica", Font.PLAIN, 12));
    protected NumberEdgeValue edgeLabelClosenessFunction = 
        new ConstantDirectionalEdgeValue(0.5, 0.65);
    protected EdgeShapeFunction edgeShapeFunction = 
        new EdgeShape.QuadCurve();
    protected EdgePaintFunction edgePaintFunction =
        new ConstantEdgePaintFunction(Color.black, null);
    protected ParallelEdgeIndexFunction parallelEdgeIndexFunction = 
        ParallelEdgeIndexSingleton.getInstance();
    protected MutableTransformer viewTransformer = new MutableAffineTransformer();
    
    /**
     * the JComponent that this Renderer will display the graph on
     */
    protected JComponent screenDevice;
    
    /**
     * The CellRendererPane is used here just as it is in JTree
     * and JTable, to allow a pluggable JLabel-based renderer for
     * Vertex and Edge label strings and icons.
     */
    protected CellRendererPane rendererPane = new CellRendererPane();
    
    /**
     * A default GraphLabelRenderer - picked Vertex labels are
     * blue, picked edge labels are cyan
     */
    protected GraphLabelRenderer graphLabelRenderer = 
        new DefaultGraphLabelRenderer(Color.blue, Color.cyan);
    
    protected final static EdgePredicate self_loop = SelfLoopEdgePredicate.getInstance();
    
    public PluggableRenderer() 
    {
        this.setEdgeShapeFunction(new EdgeShape.QuadCurve());
    }
    
    /**
     * @return Returns the edgeArrowFunction.
     */
    public EdgeArrowFunction getEdgeArrowFunction() {
        return edgeArrowFunction;
    }

    /**
     * @return Returns the edgeArrowPredicate.
     */
    public Predicate getEdgeArrowPredicate() {
        return edgeArrowPredicate;
    }

    /**
     * @return Returns the edgeFontFunction.
     */
    public EdgeFontFunction getEdgeFontFunction() {
        return edgeFontFunction;
    }

    /**
     * @return Returns the edgeIncludePredicate.
     */
    public Predicate getEdgeIncludePredicate() {
        return edgeIncludePredicate;
    }

    /**
     * @return Returns the edgeLabelClosenessFunction.
     */
    public NumberEdgeValue getEdgeLabelClosenessFunction() {
        return edgeLabelClosenessFunction;
    }

    /**
     * @return Returns the edgePaintFunction.
     */
    public EdgePaintFunction getEdgePaintFunction() {
        return edgePaintFunction;
    }

    /**
     * @return Returns the edgeStringer.
     */
    public EdgeStringer getEdgeStringer() {
        return edgeStringer;
    }

    /**
     * @return Returns the edgeStrokeFunction.
     */
    public EdgeStrokeFunction getEdgeStrokeFunction() {
        return edgeStrokeFunction;
    }

    /**
     * @return Returns the screenDevice.
     */
    public JComponent getScreenDevice() {
        return screenDevice;
    }

    /**
     * @return Returns the vertexFontFunction.
     */
    public VertexFontFunction getVertexFontFunction() {
        return vertexFontFunction;
    }

    /**
     * @return Returns the vertexIncludePredicate.
     */
    public Predicate getVertexIncludePredicate() {
        return vertexIncludePredicate;
    }

    /**
     * @return Returns the vertexPaintFunction.
     */
    public VertexPaintFunction getVertexPaintFunction() {
        return vertexPaintFunction;
    }

    /**
     * @return Returns the vertexStringer.
     */
    public VertexStringer getVertexStringer() {
        return vertexStringer;
    }

    /**
     * @return Returns the vertexIconFunction
     */
    public VertexIconFunction getVertexIconFunction() {
        return vertexIconFunction;
    }

    /**
     * @param vertexIconFunction The VertexIconFunction to set.
     */
    public void setVertexIconFunction(VertexIconFunction vertexIconFunction) {
        this.vertexIconFunction = vertexIconFunction;
    }

    /**
     * @return Returns the vertexStrokeFunction.
     */
    public VertexStrokeFunction getVertexStrokeFunction() {
        return vertexStrokeFunction;
    }

    /**
     * The screen device is the JComponent on which the renderer
     * will display the graph. It is used here to assist with removing
     * unnecessary calls to position and draw graph elements that will
     * not be visible in the display. It is also used as the container
     * for the CellRendererPane.
     * @param screenDevice
     */
    public void setScreenDevice(JComponent screenDevice) {
        this.screenDevice = screenDevice;
        this.screenDevice.add(rendererPane);
    }
    
    /**
     * Specifies the smallest (squared) distance that an arrowhead
     * must be moved in order for the placement code to decide that
     * it's "close enough".  Default value is 1.
     */
    public void setArrowPlacementTolerance(float tolerance) {
        this.arrow_placement_tolerance = tolerance;
    }
    
    /**
     * Sets the <code>EdgeArrowFunction</code> that specifies the 
     * <code>Shape</code> of the arrowheads for each edge.
     * The same shape will be used for both ends of an undirected
     * edge.  The default arrow-drawing implementations assume that arrows 
     * are drawn with their base on the y-axis, pointed left (in the negative
     * x-direction), centered on the x-axis.
     * Note that the <code>EdgeArrowFunction</code> must return a valid shape 
     * for any edge for which the edge arrow <code>Predicate</code> 
     * returns <code>true</code>.
     * <br>Default: wedge arrows for undirected edges, notched arrows for directed edges
     * (<code>DirectionalEdgeArrowFunction</code>)
     * @see edu.uci.ics.jung.decorators.EdgeArrowFunction
     * @see ArrowFactory
     */
    public void setEdgeArrowFunction(EdgeArrowFunction eaf)
    {
        this.edgeArrowFunction = eaf;
    }
    
    /**
     * @return Returns the graphLabelRenderer.
     */
    public GraphLabelRenderer getGraphLabelRenderer() {
        return graphLabelRenderer;
    }
    /**
     * @param graphLabelRenderer The graphLabelRenderer to set.
     */
    public void setGraphLabelRenderer(GraphLabelRenderer graphLabelRenderer) {
        this.graphLabelRenderer = graphLabelRenderer;
    }
    /**
     * Sets the <code>EdgeArrowPredicate</code> that specifies whether
     * arrowheads should be drawn for each edge.  If the predicate evaluates
     * to <code>true</code> for a specified edge, arrows should be drawn
     * for that edge.
     * <br>Default: only directed edges have arrows (<code>Graph.DIRECTED_EDGE</code> instance)
     * @see EdgeArrowFunction
     */
    public void setEdgeArrowPredicate(Predicate p)
    {
        this.edgeArrowPredicate = p;
    }

    /**
     * Sets the <code>EdgeColorFunction</code> that specifies the paint to
     * draw each edge.  
     * <br>Default: Color.BLACK
     * @see java.awt.Color
     * @deprecated Use setEdgePaintFunction instead
     */
    public void setEdgeColorFunction(EdgeColorFunction ecf) 
    {
        this.edgePaintFunction = new EdgeColorToEdgePaintFunctionConverter( ecf );
    }

    
    /**
     * Sets the <code>EdgeFontFunction</code> that specifies the font
     * to use for drawing each edge label.  This can be used (for example) to 
     * emphasize (or to de-emphasize) edges that have a specific property. 
     * <br>Default: 12-point Helvetica
     * @see EdgeFontFunction
     */
    public void setEdgeFontFunction(EdgeFontFunction eff)
    {
        this.edgeFontFunction = eff;
    }
    
    /**
     * Sets the <code>Predicate</code> that specifies whether each
     * edge should be drawn; only those edges for which this 
     * predicate returns <code>true</code> will be drawn.  This can be
     * used to selectively display only those edges that have a
     * specific property, such as a particular decoration or value, or
     * only those edges of a specific type (such as directed edges, 
     * or everything except self-loops).
     * <br>Default: all edges drawn (<code>TruePredicate</code> instance)
     * @see org.apache.commons.collections.Predicate
     */
    public void setEdgeIncludePredicate(Predicate p)
    {
        this.edgeIncludePredicate = p;
    }
    
    /**
     * Sets the <code>NumberEdgeValue</code> that specifies where to draw
     * the label for each edge.  A value of 0 draws the label on top of
     * the edge's first vertex; a value of 1.0 draws the label on top
     * of the edge's second vertex; values between 0 and 1 split the
     * difference (i.e., a value of 0.5 draws the label halfway in between
     * the two vertices).  The effect of values outside the range [0,1] 
     * is undefined.  This function is not used for self-loops.
     * <br>Default: 0.5 for undirected edges, 0.75 for directed edges
     * (<code>ConstantDirectionalEdgeValue</code>)
     * @see edu.uci.ics.jung.graph.decorators.NumberEdgeValue
     */
    public void setEdgeLabelClosenessFunction(NumberEdgeValue nev)
    {
        this.edgeLabelClosenessFunction = nev;
    }

    /**
     * @param impl
     */
    public void setEdgePaintFunction(EdgePaintFunction impl) {
        edgePaintFunction = impl;
        
    }
    
    /**
     * setter for the EdgeShapeFunction
     * @param impl
     */
    public void setEdgeShapeFunction(EdgeShapeFunction impl) {
        edgeShapeFunction = impl;
        if(edgeShapeFunction instanceof EdgeShape.ParallelRendering) {
            ((EdgeShape.ParallelRendering)edgeShapeFunction).setParallelEdgeIndexFunction(this.parallelEdgeIndexFunction);
        }
    }

    /**
     * @return Returns the EdgeShapeFunction that .
     */
    public EdgeShapeFunction getEdgeShapeFunction() {
        return edgeShapeFunction;
    }
    /**
     * Sets the <code>EdgeStringer</code> that specifies the label to
     * draw for each edge.
     * <br>Default: no labels
     * (<code>ConstantEdgeStringer</code>)
     * @see edu.uci.ics.jung.graph.decorators.EdgeStringer
     */
    public void setEdgeStringer(EdgeStringer es)
    {
        this.edgeStringer = es;
    }
    
    /**
     * Sets the <code>EdgeStrokeFunction</code> that specifies the
     * <code>Stroke</code> to use when drawing each edge.
     * <br>Default: 1-pixel-width basic stroke
     * (<code>ConstantEdgeStrokeFunction</code>)
     * @see java.awt.Stroke
     * @see EdgeStrokeFunction
     */
    public void setEdgeStrokeFunction(EdgeStrokeFunction esf)
    {
        this.edgeStrokeFunction = esf;
    }

    /**
     * <p>Sets the <code>VertexPaintFunction</code> to the parameter
     * @see #setVertexPaintFunction(VertexPaintFunction)
     * @see VertexColorFunction
     * @see VertexColorToVertexPaintConverter
     * @deprecated Use setVertexPaintFunction with a VertexPaintFunction if you can
     */
    public void setVertexColorFunction(VertexColorFunction vcf) 
    {
        this.vertexPaintFunction = new VertexColorToVertexPaintConverter( vcf );
    }

    /**
    * <p>Sets the <code>VertexPaintFunction</code> which specifies the 
    * draw (border and text) and fill (interior) Paint for each vertex.</p>  
    * <p>If users want the <code>VertexPaintFunction</code> implementation
    * to highlight selected vertices, they should take this 
    * PluggableRenderer instance as a constructor parameter, and call
    * the <code>isPicked</code> method on it to identify selected vertices.</p>
    * <p>Default: black borders, red foreground (selected vertex is orange).</p>
	*/
    public void setVertexPaintFunction( VertexPaintFunction vpf ) 
    {
    	this.vertexPaintFunction = vpf;
    }
    
    /**
     * Returns the <code>VertexShapeFunction</code> currently being
     * used by this instance.
     */
    public VertexShapeFunction getVertexShapeFunction() 
    {
        return vertexShapeFunction;
    }
    
    /**
     * Sets the <code>VertexFontFunction</code> that specifies the font
     * to use for drawing each vertex label.  This can be used (for example) to 
     * emphasize (or to de-emphasize) vertices that have a specific property. 
     * <br>Default: 12-point Helvetica
     * @see VertexFontFunction
     */
    public void setVertexFontFunction(VertexFontFunction vff)
    {
        this.vertexFontFunction = vff;
    }

    /**
     * Sets the <code>Predicate</code> that specifies whether each
     * vertex should be drawn; only those vertices for which this 
     * predicate returns <code>true</code> will be drawn.  This can be
     * used to selectively display only those vertices that have a
     * specific property, such as a particular decoration or value.
     * <br>Default: all vertices drawn (<code>TruePredicate</code> instance)
     * @see org.apache.commons.collections.Predicate
     */
    public void setVertexIncludePredicate(Predicate p)
    {
        this.vertexIncludePredicate = p;
    }
    
    /**
     * Specifies whether vertex labels are drawn centered on the vertex
     * position (<code>true</code>) or offset to one side (<code>false</code>).
     * <br>Default: offset
     */
    public void setVertexLabelCentering(boolean b)
    {
        centerVertexLabel = b;
    }
    
    /**
     * 
     * @return whether the vertex labels should be centered in the vertex
     */
    public boolean getVertexLabelCentering() 
    {
        return centerVertexLabel;
    }
    
    /**
     * Sets the <code>VertexShapeFunction</code>, 
     * which specifies the <code>Shape</code> for each vertex.
     * Users that wish to independently change the size and
     * aspect ratio of a vertex's shape should take a look
     * at the <code>SettableVertexShapeFunction</code>
     * interface and the <code>AbstractVertexShapeFunction</code>
     * abstract class.
     * <br>Default: 8-pixel-diameter circle 
     * (<code>EllipseVertexShapeFunction</code>)
     * @see java.awt.Shape
     * @see VertexShapeFunction
     */
    public void setVertexShapeFunction(VertexShapeFunction vsf)
    {
        this.vertexShapeFunction = vsf;
    }
    
    /**
     * Sets the <code>VertexStringer</code> that specifies the label to
     * draw for each vertex.
     * <br>Default: no labels
     * (<code>ConstantVertexStringer</code>)
     * @see edu.uci.ics.jung.graph.decorators.VertexStringer
     */
    public void setVertexStringer(VertexStringer vs)
    {
        this.vertexStringer = vs;
    }

    /**
     * Sets the <code>VertexStrokeFunction</code> which 
     * specifies the <code>Stroke</code> to use when drawing
     * each vertex border.  
     * <br>Default: 1-pixel-width basic stroke.
     * @see java.awt.Stroke
     * @see VertexStrokeFunction
     */
    public void setVertexStrokeFunction(VertexStrokeFunction vsf)
    {
        this.vertexStrokeFunction = vsf;
    }
        

    /**
     * Paints <code>e</code>, whose endpoints are at <code>(x1,y1)</code>
     * and <code>(x2,y2)</code>, on the graphics context <code>g</code>.
     * Uses the paint and stroke specified by this instance's 
     * <code>EdgeColorFunction</code> and <code>EdgeStrokeFunction</code>, 
     * respectively.  (If the paint is unspecified, the existing
     * paint for the graphics context is used; the same applies to stroke.)
     * The details of the actual rendering are delegated to
     * <code>drawSelfLoop</code> or <code>drawSimpleEdge</code>, 
     * depending on the type of the edge.  
     * Note that <code>(x1, y1)</code> is the location of
     * e.getEndpoints.getFirst() and <code>(x2, y2)</code> is the location of
     * e.getEndpoints.getSecond().
     * 
     */
    public void paintEdge(Graphics g, Edge e, int x1, int y1, int x2, int y2) 
    {
        if (!edgeIncludePredicate.evaluate(e))
            return;
        
        // don't draw edge if either incident vertex is not drawn
        Pair endpoints = e.getEndpoints();
        Vertex v1 = (Vertex)endpoints.getFirst();
        Vertex v2 = (Vertex)endpoints.getSecond();
        if (!vertexIncludePredicate.evaluate(v1) || 
            !vertexIncludePredicate.evaluate(v2))
            return;
        
        Graphics2D g2d = (Graphics2D) g;

        Stroke new_stroke = edgeStrokeFunction.getStroke(e);
        Stroke old_stroke = g2d.getStroke();
        if (new_stroke != null)
            g2d.setStroke(new_stroke);
        
        drawSimpleEdge(g2d, e, x1, y1, x2, y2);

        // restore paint and stroke
        if (new_stroke != null)
            g2d.setStroke(old_stroke);

    }

    /**
     * Draws the edge <code>e</code>, whose endpoints are at <code>(x1,y1)</code>
     * and <code>(x2,y2)</code>, on the graphics context <code>g</code>.
     * The <code>Shape</code> provided by the <code>EdgeShapeFunction</code> instance
     * is scaled in the x-direction so that its width is equal to the distance between
     * <code>(x1,y1)</code> and <code>(x2,y2)</code>.
     */
    protected void drawSimpleEdge(Graphics2D g, Edge e, int x1, int y1, int x2, int y2)
    {
        Pair endpoints = e.getEndpoints();
        Vertex v1 = (Vertex)endpoints.getFirst();
        Vertex v2 = (Vertex)endpoints.getSecond();
        boolean isLoop = v1.equals(v2);
        Shape s2 = vertexShapeFunction.getShape(v2);
        Shape edgeShape = edgeShapeFunction.getShape(e);
        
        boolean edgeHit = true;
        boolean arrowHit = true;
        Rectangle deviceRectangle = null;
        if(screenDevice != null) {
            Dimension d = screenDevice.getSize();
            if(d.width <= 0 || d.height <= 0) {
                d = screenDevice.getPreferredSize();
            }
            deviceRectangle = new Rectangle(0,0,d.width,d.height);
        }

        AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);
        
        if(isLoop) {
            // this is a self-loop. scale it is larger than the vertex
            // it decorates and translate it so that its nadir is
            // at the center of the vertex.
            Rectangle2D s2Bounds = s2.getBounds2D();
            xform.scale(s2Bounds.getWidth(),s2Bounds.getHeight());
            xform.translate(0, -edgeShape.getBounds2D().getWidth()/2);
        } else {
            // this is a normal edge. Rotate it to the angle between
            // vertex endpoints, then scale it to the distance between
            // the vertices
            float dx = x2-x1;
            float dy = y2-y1;
            float thetaRadians = (float) Math.atan2(dy, dx);
            xform.rotate(thetaRadians);
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            xform.scale(dist, 1.0);
        }
        
        edgeShape = xform.createTransformedShape(edgeShape);
        
        edgeHit = viewTransformer.transform(edgeShape).intersects(deviceRectangle);

        if(edgeHit == true) {
            
            Paint oldPaint = g.getPaint();
            
            // get Paints for filling and drawing
            // (filling is done first so that drawing and label use same Paint)
            Paint fill_paint = edgePaintFunction.getFillPaint(e); 
            if (fill_paint != null)
            {
                g.setPaint(fill_paint);
                g.fill(edgeShape);
            }
            Paint draw_paint = edgePaintFunction.getDrawPaint(e);
            if (draw_paint != null)
            {
                g.setPaint(draw_paint);
                g.draw(edgeShape);
            }
            
            float scalex = (float)g.getTransform().getScaleX();
            float scaley = (float)g.getTransform().getScaleY();
            // see if arrows are too small to bother drawing
            if(scalex < .3 || scaley < .3) return;
            
            if (edgeArrowPredicate.evaluate(e)) {
                
                Shape destVertexShape = 
                    vertexShapeFunction.getShape((Vertex)e.getEndpoints().getSecond());
                AffineTransform xf = AffineTransform.getTranslateInstance(x2, y2);
                destVertexShape = xf.createTransformedShape(destVertexShape);
                
                arrowHit = viewTransformer.transform(destVertexShape).intersects(deviceRectangle);
                if(arrowHit) {
                    
                    AffineTransform at;
                    if (edgeShape instanceof GeneralPath)
                        at = getArrowTransform((GeneralPath)edgeShape, destVertexShape);
                    else
                        at = getArrowTransform(new GeneralPath(edgeShape), destVertexShape);
                    if(at == null) return;
                    Shape arrow = edgeArrowFunction.getArrow(e);
                    arrow = at.createTransformedShape(arrow);
                    // note that arrows implicitly use the edge's draw paint
                    g.fill(arrow);
                }
                if (e instanceof UndirectedEdge) {
                    Shape vertexShape = 
                        vertexShapeFunction.getShape((Vertex)e.getEndpoints().getFirst());
                    xf = AffineTransform.getTranslateInstance(x1, y1);
                    vertexShape = xf.createTransformedShape(vertexShape);
                    
                    arrowHit = viewTransformer.transform(vertexShape).intersects(deviceRectangle);
                    
                    if(arrowHit) {
                        AffineTransform at;
                        if (edgeShape instanceof GeneralPath)
                            at = getReverseArrowTransform((GeneralPath)edgeShape, vertexShape, !isLoop);
                        else
                            at = getReverseArrowTransform(new GeneralPath(edgeShape), vertexShape, !isLoop);
                        if(at == null) return;
                        Shape arrow = edgeArrowFunction.getArrow(e);
                        arrow = at.createTransformedShape(arrow);
                        g.fill(arrow);
                    }
                }
            }
            // use existing paint for text if no draw paint specified
            if (draw_paint == null)
                g.setPaint(oldPaint);
            String label = edgeStringer.getLabel(e);
            if (label != null) {
                labelEdge(g, e, label, x1, x2, y1, y2);
            }
            
            
            // restore old paint
            g.setPaint(oldPaint);
        }
    }
    
    /**
     * Returns a transform to position the arrowhead on this edge shape at the
     * point where it intersects the passed vertex shape.
     */
    public AffineTransform getArrowTransform(GeneralPath edgeShape, Shape vertexShape) {
        float[] seg = new float[6];
        Point2D p1=null;
        Point2D p2=null;
        AffineTransform at = new AffineTransform();
        // when the PathIterator is done, switch to the line-subdivide
        // method to get the arrowhead closer.
        for(PathIterator i=edgeShape.getPathIterator(null,1); !i.isDone(); i.next()) {
            int ret = i.currentSegment(seg);
            if(ret == PathIterator.SEG_MOVETO) {
                p2 = new Point2D.Float(seg[0],seg[1]);
            } else if(ret == PathIterator.SEG_LINETO) {
                p1 = p2;
                p2 = new Point2D.Float(seg[0],seg[1]);
                if(vertexShape.contains(p2)) {
                    at = getArrowTransform(new Line2D.Float(p1,p2),vertexShape);
                    break;
                }
            } 
        }
        return at;
    }

    /**
     * Returns a transform to position the arrowhead on this edge shape at the
     * point where it intersects the passed vertex shape.
     */
    public AffineTransform getReverseArrowTransform(GeneralPath edgeShape, Shape vertexShape) {
        return getReverseArrowTransform(edgeShape, vertexShape, true);
    }
            
    /**
     * <p>Returns a transform to position the arrowhead on this edge shape at the
     * point where it intersects the passed vertex shape.</p>
     * 
     * <p>The Loop edge is a special case because its staring point is not inside
     * the vertex. The passedGo flag handles this case.</p>
     * 
     * @param edgeShape
     * @param vertexShape
     * @param passedGo - used only for Loop edges
     */
    public AffineTransform getReverseArrowTransform(GeneralPath edgeShape, Shape vertexShape,
            boolean passedGo) {
        float[] seg = new float[6];
        Point2D p1=null;
        Point2D p2=null;

        AffineTransform at = new AffineTransform();
        for(PathIterator i=edgeShape.getPathIterator(null,1); !i.isDone(); i.next()) {
            int ret = i.currentSegment(seg);
            if(ret == PathIterator.SEG_MOVETO) {
                p2 = new Point2D.Float(seg[0],seg[1]);
            } else if(ret == PathIterator.SEG_LINETO) {
                p1 = p2;
                p2 = new Point2D.Float(seg[0],seg[1]);
                if(passedGo == false && vertexShape.contains(p2)) {
                    passedGo = true;
                 } else if(passedGo==true &&
                        vertexShape.contains(p2)==false) {
                     at = getReverseArrowTransform(new Line2D.Float(p1,p2),vertexShape);
                    break;
                }
            } 
        }
        return at;
    }

    /**
     * This is used for the arrow of a directed and for one of the
     * arrows for non-directed edges
     * Get a transform to place the arrow shape on the passed edge at the
     * point where it intersects the passed shape
     * @param edgeShape
     * @param vertexShape
     * @return
     */
    public AffineTransform getArrowTransform(Line2D edgeShape, Shape vertexShape) {
        float dx = (float) (edgeShape.getX1()-edgeShape.getX2());
        float dy = (float) (edgeShape.getY1()-edgeShape.getY2());
        // iterate over the line until the edge shape will place the
        // arrowhead closer than 'arrowGap' to the vertex shape boundary
        while((dx*dx+dy*dy) > arrow_placement_tolerance) {
            try {
                edgeShape = getLastOutsideSegment(edgeShape, vertexShape);
            } catch(IllegalArgumentException e) {
                System.err.println(e.toString());
                return null;
            }
            dx = (float) (edgeShape.getX1()-edgeShape.getX2());
            dy = (float) (edgeShape.getY1()-edgeShape.getY2());
        }
        double atheta = Math.atan2(dx,dy)+Math.PI/2;
        AffineTransform at = 
            AffineTransform.getTranslateInstance(edgeShape.getX1(), edgeShape.getY1());
        at.rotate(-atheta);
        return at;
    }

    /**
     * This is used for the reverse-arrow of a non-directed edge
     * get a transform to place the arrow shape on the passed edge at the
     * point where it intersects the passed shape
     * @param edgeShape
     * @param vertexShape
     * @return
     */
    protected AffineTransform getReverseArrowTransform(Line2D edgeShape, Shape vertexShape) {
        float dx = (float) (edgeShape.getX1()-edgeShape.getX2());
        float dy = (float) (edgeShape.getY1()-edgeShape.getY2());
        // iterate over the line until the edge shape will place the
        // arrowhead closer than 'arrowGap' to the vertex shape boundary
        while((dx*dx+dy*dy) > arrow_placement_tolerance) {
            try {
                edgeShape = getFirstOutsideSegment(edgeShape, vertexShape);
            } catch(IllegalArgumentException e) {
                System.err.println(e.toString());
                return null;
            }
            dx = (float) (edgeShape.getX1()-edgeShape.getX2());
            dy = (float) (edgeShape.getY1()-edgeShape.getY2());
        }
        // calculate the angle for the arrowhead
        double atheta = Math.atan2(dx,dy)-Math.PI/2;
        AffineTransform at = AffineTransform.getTranslateInstance(edgeShape.getX1(),edgeShape.getY1());
        at.rotate(-atheta);
        return at;
    }
    
    /**
     * Passed Line's point2 must be inside the passed shape or
     * an IllegalArgumentException is thrown
     * @param line line to subdivide
     * @param shape shape to compare with line
     * @return a line that intersects the shape boundary
     * @throws IllegalArgumentException if the passed line's point1 is not inside the shape
     */
    protected Line2D getLastOutsideSegment(Line2D line, Shape shape) {
        if(shape.contains(line.getP2())==false) {
            String errorString =
                "line end point: "+line.getP2()+" is not contained in shape: "+shape.getBounds2D();
            throw new IllegalArgumentException(errorString);
            //return null;
        }
        Line2D left = new Line2D.Double();
        Line2D right = new Line2D.Double();
        // subdivide the line until its left segment intersects
        // the shape boundary
        int iterations = 0;
        do {
            subdivide(line, left, right);
            line = right;
        } while(shape.contains(line.getP1())==false && iterations++ < MAX_ITERATIONS);
        // now that right is completely inside shape,
        // return left, which must be partially outside
        return left;
    }
   
    /**
     * Passed Line's point1 must be inside the passed shape or
     * an IllegalArgumentException is thrown
     * @param line line to subdivide
     * @param shape shape to compare with line
     * @return a line that intersects the shape boundary
     * @throws IllegalArgumentException if the passed line's point1 is not inside the shape
     */
    protected Line2D getFirstOutsideSegment(Line2D line, Shape shape) {
        
        if(shape.contains(line.getP1())==false) {
            String errorString = 
                "line start point: "+line.getP1()+" is not contained in shape: "+shape.getBounds2D();
            throw new IllegalArgumentException(errorString);
        }
        Line2D left = new Line2D.Float();
        Line2D right = new Line2D.Float();
        // subdivide the line until its right side intersects the
        // shape boundary
        do {
            subdivide(line, left, right);
            line = left;
        } while(shape.contains(line.getP2())==false);
        // now that left is completely inside shape,
        // return right, which must be partially outside
        return right;
    }

    /**
     * divide a Line2D into 2 new Line2Ds that are returned
     * in the passed left and right instances, if non-null
     * @param src the line to divide
     * @param left the left side, or null
     * @param right the right side, or null
     */
    protected void subdivide(Line2D src,
            Line2D left,
            Line2D right) {
        double x1 = src.getX1();
        double y1 = src.getY1();
        double x2 = src.getX2();
        double y2 = src.getY2();
        
        double mx = x1 + (x2-x1)/2.0;
        double my = y1 + (y2-y1)/2.0;
        if (left != null) {
            left.setLine(x1, y1, mx, my);
        }
        if (right != null) {
            right.setLine(mx, my, x2, y2);
        }
    }

   public Component prepareRenderer(GraphLabelRenderer graphLabelRenderer, Object value, 
           boolean isSelected, Vertex vertex) {
       return graphLabelRenderer.getGraphLabelRendererComponent(screenDevice, value, 
               vertexFontFunction.getFont(vertex), isSelected, vertex);
   }

   public Component prepareRenderer(GraphLabelRenderer renderer, Object value, 
           boolean isSelected, Edge edge) {
       return graphLabelRenderer.getGraphLabelRendererComponent(screenDevice, value, 
               edgeFontFunction.getFont(edge), isSelected, edge);
   }

    /**
     * Labels the specified non-self-loop edge with the specified label.
     * Uses the font specified by this instance's 
     * <code>EdgeFontFunction</code>.  (If the font is unspecified, the existing
     * font for the graphics context is used.)  Positions the 
     * label between the endpoints according to the coefficient returned
     * by this instance's edge label closeness function.
     */
    protected void labelEdge(Graphics2D g2d, Edge e, String label, int x1, int x2, int y1, int y2) 
    {
        int distX = x2 - x1;
        int distY = y2 - y1;
        double totalLength = Math.sqrt(distX * distX + distY * distY);

        double closeness = edgeLabelClosenessFunction.getNumber(e).doubleValue();

        int posX = (int) (x1 + (closeness) * distX);
        int posY = (int) (y1 + (closeness) * distY);

        int xDisplacement = (int) (LABEL_OFFSET * (distY / totalLength));
        int yDisplacement = (int) (LABEL_OFFSET * (-distX / totalLength));
        
        Component component = prepareRenderer(graphLabelRenderer, label, isPicked(e), e);
        
        Dimension d = component.getPreferredSize();

        Shape edgeShape = edgeShapeFunction.getShape(e);
        
        double parallelOffset = 1;

        parallelOffset += parallelEdgeIndexFunction.getIndex(e);

        if(edgeShape instanceof Ellipse2D) {
            parallelOffset += edgeShape.getBounds().getHeight();
            parallelOffset = -parallelOffset;
        }
        
        parallelOffset *= d.height;
        
        AffineTransform old = g2d.getTransform();
        AffineTransform xform = new AffineTransform(old);
        xform.translate(posX+xDisplacement, posY+yDisplacement);
        double dx = x2 - x1;
        double dy = y2 - y1;
        if(graphLabelRenderer.isRotateEdgeLabels()) {
            double theta = Math.atan2(dy, dx);
            if(dx < 0) {
                theta += Math.PI;
            }
            xform.rotate(theta);
        }
        if(dx < 0) {
            parallelOffset = -parallelOffset;
        }
        
        xform.translate(-d.width/2, -(d.height/2-parallelOffset));
        g2d.setTransform(xform);
        rendererPane.paintComponent(g2d, component, screenDevice, 
                0, 0,
                d.width, d.height, true);
        g2d.setTransform(old);
    }

    /**
     * Paints the vertex <code>v</code> at the location <code>(x,y)</code>
     * on the graphics context <code>g_gen</code>.  The vertex is painted
     * using the shape returned by this instance's <code>VertexShapeFunction</code>,
     * and the foreground and background (border) colors provided by this
     * instance's <code>VertexColorFunction</code>.  Delegates drawing the
     * label (if any) for this vertex to <code>labelVertex</code>.
     */
    public void paintVertex(Graphics g, Vertex v, int x, int y) 
    {
        if (!vertexIncludePredicate.evaluate(v))
            return;
        
        boolean vertexHit = true;
        Rectangle deviceRectangle = null;
        Graphics2D g2d = (Graphics2D)g;
        if(screenDevice != null) {
            Dimension d = screenDevice.getSize();
            if(d.width <= 0 || d.height <= 0) {
                d = screenDevice.getPreferredSize();
            }
            deviceRectangle = new Rectangle(
                    0,0,
                    d.width,d.height);
        }
        
        
        Stroke old_stroke = g2d.getStroke();
        Stroke new_stroke = vertexStrokeFunction.getStroke(v);
        if (new_stroke != null) {
            g2d.setStroke(new_stroke);
        }
        // get the shape to be rendered
        Shape s = vertexShapeFunction.getShape(v);
        
        // create a transform that translates to the location of
        // the vertex to be rendered
        AffineTransform xform = AffineTransform.getTranslateInstance(x,y);
        // transform the vertex shape with xtransform
        s = xform.createTransformedShape(s);
        
        vertexHit = viewTransformer.transform(s).intersects(deviceRectangle);

        if (vertexHit) {

			if (vertexIconFunction != null) {
				paintIconForVertex(g2d, v, x, y);
			} else {
				paintShapeForVertex(g2d, v, s);
			}

			if (new_stroke != null) {
				g2d.setStroke(old_stroke);
			}
			String label = vertexStringer.getLabel(v);
			if (label != null) {
				labelVertex(g, v, label, x, y);
			}
		}
    }
    
    public void paintShapeForVertex(Graphics2D g2d, Vertex v, Shape shape) {
        Paint oldPaint = g2d.getPaint();
        Paint fillPaint = vertexPaintFunction.getFillPaint(v);
        if(fillPaint != null) {
            g2d.setPaint(fillPaint);
            g2d.fill(shape);
            g2d.setPaint(oldPaint);
        }
        Paint drawPaint = vertexPaintFunction.getDrawPaint(v);
        if(drawPaint != null) {
            g2d.setPaint(drawPaint);
            g2d.draw(shape);
            g2d.setPaint(oldPaint);
        }
    }
    
    /**
     * Paint <code>v</code>'s icon on <code>g</code> at <code>(x,y)</code>.
     */
    public void paintIconForVertex(Graphics g, Vertex v, int x, int y) {
        Icon icon = vertexIconFunction.getIcon(v);
        if(icon == null) {
            Shape s = AffineTransform.getTranslateInstance(x,y).
            	createTransformedShape(getVertexShapeFunction().getShape(v));
            paintShapeForVertex((Graphics2D)g, v, s);
        } else {
        	int xLoc = x - icon.getIconWidth()/2;
        	int yLoc = y - icon.getIconHeight()/2;
        	icon.paintIcon(screenDevice, g, xLoc, yLoc);
        }
    }
    
    /**
     * Labels the specified vertex with the specified label.  
     * Uses the font specified by this instance's 
     * <code>VertexFontFunction</code>.  (If the font is unspecified, the existing
     * font for the graphics context is used.)  If vertex label centering
     * is active, the label is centered on the position of the vertex; otherwise
     * the label is offset slightly.
     */
    protected void labelVertex(Graphics g, Vertex v, String label, int x, int y)
    {
        Component component = prepareRenderer(graphLabelRenderer, label, isPicked(v), v);

        Dimension d = component.getPreferredSize();
        
        int h_offset;
        int v_offset;
        if (centerVertexLabel)
        {
            h_offset = -d.width / 2;
            v_offset = -d.height / 2;

        }
        else
        {
            Rectangle2D bounds = vertexShapeFunction.getShape(v).getBounds2D();
            h_offset = (int)(bounds.getWidth() / 2) + 5;
            v_offset = (int)(bounds.getHeight() / 2) + 5 -d.height;
        }
        
        rendererPane.paintComponent(g, component, screenDevice, x+h_offset, y+v_offset,
                d.width, d.height, true);
        
    }
    
    /**
     * @see AbstractRenderer#isPicked(Vertex)
     * @deprecated Use an independent PickedInfo instead of this version,
     * which relies on the Renderer to supply an instance.
     */
    public boolean isPicked(ArchetypeVertex v)
    {
        return super.isPicked(v);
    }
    
    /**
     * @see AbstractRenderer#isPicked(Edge)
     * @deprecated Use an independent PickedInfo instead of this version,
     * which relies on the Renderer to supply an instance.
     */
    public boolean isPicked(ArchetypeEdge e) {
        return super.isPicked(e);
    }

    /**
     * @return Returns the rendererPane.
     */
    public CellRendererPane getRendererPane() {
        return rendererPane;
    }

    /**
     * @param rendererPane The rendererPane to set.
     */
    public void setRendererPane(CellRendererPane rendererPane) {
        this.rendererPane = rendererPane;
    }

    public ParallelEdgeIndexFunction getParallelEdgeIndexFunction() {
        return parallelEdgeIndexFunction;
    }

    public void setParallelEdgeIndexFunction(
            ParallelEdgeIndexFunction parallelEdgeIndexFunction) {
        this.parallelEdgeIndexFunction = parallelEdgeIndexFunction;
    }

    public void setViewTransformer(MutableTransformer viewTransformer) {
        this.viewTransformer = viewTransformer;
    }
}
