/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */
package edu.uci.ics.jung.samples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.PerspectiveTransform;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.LayeredIcon;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.DefaultVertexIconTransformer;
import edu.uci.ics.jung.visualization.decorators.EllipseVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.decorators.VertexIconShapeTransformer;
import edu.uci.ics.jung.visualization.jai.PerspectiveImageLensSupport;
import edu.uci.ics.jung.visualization.jai.PerspectiveLayoutTransformSupport;
import edu.uci.ics.jung.visualization.jai.PerspectiveTransformSupport;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.Checkmark;


/**
 * Demonstrates the use of images to represent graph vertices.
 * The images are added to the DefaultGraphLabelRenderer and can
 * either be offset from the vertex, or centered on the vertex.
 * Additionally, the relative positioning of the label and
 * image is controlled by subclassing the DefaultGraphLabelRenderer
 * and setting the appropriate properties on its JLabel superclass
 *  FancyGraphLabelRenderer
 * 
 * The images used in this demo (courtesy of slashdot.org) are
 * rectangular but with a transparent background. When vertices
 * are represented by these images, it looks better if the actual
 * shape of the opaque part of the image is computed so that the
 * edge arrowheads follow the visual shape of the image. This demo
 * uses the FourPassImageShaper class to compute the Shape from
 * an image with transparent background.
 * 
 * @author Tom Nelson
 * 
 */
public class PerspectiveVertexImageShaperDemo extends JApplet {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1943329822820885759L;

	/**
     * the graph
     */
    Graph<Number,Number> graph;

    /**
     * the visual component and renderer for the graph
     */
    VisualizationViewer<Number,Number> vv;
    
    /**
     * some icon names to use
     */
    String[] iconNames = {
            "apple",
            "os",
            "x",
            "linux",
            "inputdevices",
            "wireless",
            "graphics3",
            "gamespcgames",
            "humor",
            "music",
            "privacy"
    };
    
    PerspectiveTransformSupport viewSupport;
    PerspectiveTransformSupport layoutSupport;
    /**
     * create an instance of a simple graph with controls to
     * demo the zoom features.
     * 
     */
    @SuppressWarnings("serial")
	public PerspectiveVertexImageShaperDemo() {
        
        // create a simple graph for the demo
        graph = new DirectedSparseMultigraph<Number,Number>();
        Number[] vertices = createVertices(11);
        
        // a Map for the labels
        Map<Number,String> map = new HashMap<Number,String>();
        for(int i=0; i<vertices.length; i++) {
            map.put(vertices[i], iconNames[i%iconNames.length]);
        }
        
        // a Map for the Icons
        Map<Number,Icon> iconMap = new HashMap<Number,Icon>();
        for(int i=0; i<vertices.length; i++) {
            String name = "/images/topic"+iconNames[i]+".gif";
            try {
                Icon icon = 
                    new LayeredIcon(new ImageIcon(PerspectiveVertexImageShaperDemo.class.getResource(name)).getImage());
                iconMap.put(vertices[i], icon);
            } catch(Exception ex) {
                System.err.println("You need slashdoticons.jar in your classpath to see the image "+name);
            }
        }
        
        createEdges(vertices);
        
        final VertexStringerImpl<Number> vertexStringerImpl = 
            new VertexStringerImpl<Number>(map);
        
        final VertexIconShapeTransformer<Number> vertexImageShapeFunction =
            new VertexIconShapeTransformer<Number>(new EllipseVertexShapeTransformer<Number>());
        
        FRLayout<Number,Number> layout = new FRLayout<Number,Number>(graph);
        layout.setMaxIterations(100);
        vv =  new VisualizationViewer<Number,Number>(layout, new Dimension(400,400));

        vv.setBackground(Color.decode("0xffffdd"));
        final DefaultVertexIconTransformer<Number> vertexIconFunction =
        	new DefaultVertexIconTransformer<Number>();
        vertexImageShapeFunction.setIconMap(iconMap);
        vertexIconFunction.setIconMap(iconMap);
        vv.getRenderContext().setVertexShapeTransformer(vertexImageShapeFunction);
        vv.getRenderContext().setVertexIconTransformer(vertexIconFunction);
        vv.getRenderContext().setVertexLabelTransformer(vertexStringerImpl);
        PickedState<Number> ps = vv.getPickedVertexState();
        ps.addItemListener(new PickWithIconListener(vertexIconFunction));


        vv.addPostRenderPaintable(new VisualizationServer.Paintable(){
            int x;
            int y;
            Font font;
            FontMetrics metrics;
            int swidth;
            int sheight;
            String str = "Thank You, slashdot.org, for the images!";
            
            public void paint(Graphics g) {
                Dimension d = vv.getSize();
                if(font == null) {
                    font = new Font(g.getFont().getName(), Font.BOLD, 20);
                    metrics = g.getFontMetrics(font);
                    swidth = metrics.stringWidth(str);
                    sheight = metrics.getMaxAscent()+metrics.getMaxDescent();
                    x = (d.width-swidth)/2;
                    y = (int)(d.height-sheight*1.5);
                }
                g.setFont(font);
                Color oldColor = g.getColor();
                g.setColor(Color.lightGray);
                g.drawString(str, x, y);
                g.setColor(oldColor);
            }
            public boolean useTransform() {
                return false;
            }
        });

        // add a listener for ToolTips
        vv.setVertexToolTipTransformer(new ToStringLabeller<Number>());
        
        Container content = getContentPane();
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        content.add(panel);
        
        final DefaultModalGraphMouse<Number, Number> graphMouse = 
            new DefaultModalGraphMouse<Number, Number>();

        vv.setGraphMouse(graphMouse);
        
        this.viewSupport = new PerspectiveImageLensSupport<Number,Number>(vv);
        this.layoutSupport = new PerspectiveLayoutTransformSupport<Number,Number>(vv);
        
        final ScalingControl scaler = new CrossoverScalingControl();

        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 0.9f, vv.getCenter());
            }
        });
        final JSlider horizontalSlider = new JSlider(-120,120,0){

			/* (non-Javadoc)
			 * @see javax.swing.JComponent#getPreferredSize()
			 */
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(80, super.getPreferredSize().height);
			}
        };
        
        final JSlider verticalSlider = new JSlider(-120,120,0) {

			/* (non-Javadoc)
			 * @see javax.swing.JComponent#getPreferredSize()
			 */
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(super.getPreferredSize().width, 80);
			}
        };
        verticalSlider.setOrientation(JSlider.VERTICAL);
        final ChangeListener changeListener = new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
                int vval = -verticalSlider.getValue();
                int hval = horizontalSlider.getValue();

                Dimension d = vv.getSize();
                 PerspectiveTransform pt = null;
                    pt = PerspectiveTransform.getQuadToQuad(
                            vval,          hval, 
                            d.width-vval, -hval, 
                            d.width+vval, d.height+hval, 
                            -vval,         d.height-hval,
                            
                            0, 0, 
                            d.width, 0, 
                            d.width, d.height, 
                            0, d.height);

                viewSupport.getPerspectiveTransformer().setPerspectiveTransform(pt);
                layoutSupport.getPerspectiveTransformer().setPerspectiveTransform(pt);
                vv.repaint();
			}};
		horizontalSlider.addChangeListener(changeListener);
		verticalSlider.addChangeListener(changeListener);
		
		
        JPanel perspectivePanel = new JPanel(new BorderLayout());
        JPanel perspectiveCenterPanel = new JPanel(new BorderLayout());
        perspectivePanel.setBorder(BorderFactory.createTitledBorder("Perspective Controls"));
        final JButton center = new JButton("Center");
        center.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				horizontalSlider.setValue(0);
				verticalSlider.setValue(0);
			}});

        final JCheckBox noText = new JCheckBox("No Text");
        noText.addItemListener(new ItemListener(){

            public void itemStateChanged(ItemEvent e) {
                JCheckBox cb = (JCheckBox)e.getSource();
                vertexStringerImpl.setEnabled(!cb.isSelected());
                vv.repaint();
            }
        });
        JPanel centerPanel = new JPanel();
        centerPanel.add(noText);

        ButtonGroup radio = new ButtonGroup();
        JRadioButton none = new JRadioButton("None");
        none.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
            	boolean selected = e.getStateChange() == ItemEvent.SELECTED;
                if(selected) {
                    if(viewSupport != null) {
                        viewSupport.deactivate();
                    }
                    if(layoutSupport != null) {
                        layoutSupport.deactivate();
                    }
                }
                center.setEnabled(!selected);
                horizontalSlider.setEnabled(!selected);
                verticalSlider.setEnabled(!selected);
            }
        });
        none.setSelected(true);

        JRadioButton hyperView = new JRadioButton("View");
        hyperView.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                viewSupport.activate(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        JRadioButton hyperModel = new JRadioButton("Layout");
        hyperModel.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                layoutSupport.activate(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        radio.add(none);
        radio.add(hyperView);
        radio.add(hyperModel);
        
        JMenuBar menubar = new JMenuBar();
        JMenu modeMenu = graphMouse.getModeMenu();
        menubar.add(modeMenu);

        panel.setCorner(menubar);
        
        JPanel scaleGrid = new JPanel(new GridLayout(2,0));
        scaleGrid.setBorder(BorderFactory.createTitledBorder("Zoom"));
        JPanel controls = new JPanel(new BorderLayout());
        scaleGrid.add(plus);
        scaleGrid.add(minus);
        controls.add(scaleGrid, BorderLayout.WEST);

        JPanel lensPanel = new JPanel(new GridLayout(2,0));
        lensPanel.add(none);
        lensPanel.add(hyperView);
        lensPanel.add(hyperModel);

        perspectivePanel.add(lensPanel, BorderLayout.WEST);
        perspectiveCenterPanel.add(horizontalSlider, BorderLayout.SOUTH);
        perspectivePanel.add(verticalSlider, BorderLayout.EAST);
        perspectiveCenterPanel.add(center);
        perspectivePanel.add(perspectiveCenterPanel);
        controls.add(perspectivePanel, BorderLayout.EAST);
        
        
        controls.add(centerPanel);
        content.add(controls, BorderLayout.SOUTH);
    }
    
    /**
     * A simple implementation of VertexStringer that
     * gets Vertex labels from a Map  
     * 
     * @author Tom Nelson 
     *
     *
     */
    class VertexStringerImpl<V> implements Transformer<V,String> {

        Map<V,String> map = new HashMap<V,String>();
        
        boolean enabled = true;
        
        public VertexStringerImpl(Map<V,String> map) {
            this.map = map;
        }
        
        public String transform(Object v) {
            if(isEnabled()) {
                return map.get(v);
            } else {
                return "";
            }
        }

        /**
         * @return Returns the enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * @param enabled The enabled to set.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * create some vertices
     * @param count how many to create
     * @return the Vertices in an array
     */
    private Number[] createVertices(int count) {
        Number[] v = new Number[count];
        for (int i = 0; i < count; i++) {
            v[i] = new Integer(i);
            graph.addVertex(v[i]);
        }
        return v;
    }

    /**
     * create edges for this demo graph
     * @param v an array of Vertices to connect
     */
    void createEdges(Number[] v) {
        graph.addEdge(new Double(Math.random()), v[0], v[1], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[3], v[0], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[0], v[4], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[4], v[5], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[5], v[3], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[2], v[1], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[4], v[1], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[8], v[2], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[3], v[8], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[6], v[7], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[7], v[5], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[0], v[9], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[9], v[8], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[7], v[6], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[6], v[5], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[4], v[2], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[5], v[4], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[4], v[10], EdgeType.DIRECTED);
        graph.addEdge(new Double(Math.random()), v[10], v[4], EdgeType.DIRECTED);
    }
    
    public static class PickWithIconListener implements ItemListener {
        DefaultVertexIconTransformer<Number> imager;
        Icon checked;
        
        public PickWithIconListener(DefaultVertexIconTransformer<Number> imager) {
            this.imager = imager;
            checked = new Checkmark(Color.red);
        }

        public void itemStateChanged(ItemEvent e) {
            Icon icon = imager.transform((Number)e.getItem());
            if(icon != null && icon instanceof LayeredIcon) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    ((LayeredIcon)icon).add(checked);
                } else {
                    ((LayeredIcon)icon).remove(checked);
                }
            }
        }
    }

    /**
     * a simple Icon that draws a checkmark in the lower-right quadrant of its
     * area. Used to draw a checkmark on Picked Vertices.
     */
//    public static class Checkmark implements Icon {
//
//            GeneralPath path = new GeneralPath();
//            AffineTransform highlight = AffineTransform.getTranslateInstance(-1,-1);
//            AffineTransform lowlight = AffineTransform.getTranslateInstance(1,1);
//            AffineTransform shadow = AffineTransform.getTranslateInstance(2,2);
//            Color color;
//            public Checkmark() {
//                this(Color.red);
//            }
//            public Checkmark(Color color) {
//                this.color = color;
//                path.moveTo(10,17);
//                path.lineTo(13,20);
//                path.lineTo(20,13);
//            }
//        public void paintIcon(Component c, Graphics g, int x, int y) {
//            Shape shape = AffineTransform.getTranslateInstance(x, y).createTransformedShape(path);
//            Graphics2D g2d = (Graphics2D)g;
//            g2d.addRenderingHints(Collections.singletonMap(RenderingHints.KEY_ANTIALIASING, 
//                    RenderingHints.VALUE_ANTIALIAS_ON));
//            g2d.setStroke(new BasicStroke(4));
//            g2d.setColor(Color.darkGray);
//            g2d.draw(shadow.createTransformedShape(shape));
//            g2d.setColor(Color.black);
//            g2d.draw(lowlight.createTransformedShape(shape));
//            g2d.setColor(Color.white);
//            g2d.draw(highlight.createTransformedShape(shape));
//            g2d.setColor(color);
//            g2d.draw(shape);
//        }
//
//        public int getIconWidth() {
//            return 20;
//        }
//
//        public int getIconHeight() {
//            return 20;
//        }
//    }
//   /**
//     * An icon that is made up of a collection of Icons.
//     * They are rendered in layers starting with the first
//     * Icon added (from the constructor).
//     * 
//     * @author Tom Nelson
//     *
//     */
//    public static class LayeredIcon extends ImageIcon {
//
//		/**
//		 * 
//		 */
//    	private static final long serialVersionUID = -2975294939874762164L;
//		Set<Icon> iconSet = new LinkedHashSet<Icon>();
//
//		public LayeredIcon(Image image) {
//            super(image);
//		}
//
//        public void paintIcon(Component c, Graphics g, int x, int y) {
//            super.paintIcon(c, g, x, y);
//            Dimension d = new Dimension(getIconWidth(), getIconHeight());
//            for (Iterator iterator = iconSet.iterator(); iterator.hasNext();) {
//                Icon icon = (Icon) iterator.next();
//                 Dimension id = new Dimension(icon.getIconWidth(), icon.getIconHeight());
//                 int dx = (d.width - id.width)/2;
//                 int dy = (d.height - id.height)/2;
//                icon.paintIcon(c, g, x+dx, y+dy);
//            }
//        }
//
//		public void add(Icon icon) {
//			iconSet.add(icon);
//		}
//
//		public boolean remove(Icon icon) {
//			return iconSet.remove(icon);
//		}
//	}

    /**
     * a driver for this demo
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Container content = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content.add(new PerspectiveVertexImageShaperDemo());
        frame.pack();
        frame.setVisible(true);
    }
}
