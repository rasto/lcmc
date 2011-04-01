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
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.PerspectiveTransform;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.TransformerUtils;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.TestGraphs;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationModel;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.jai.PerspectiveLayoutTransformSupport;
import edu.uci.ics.jung.visualization.jai.PerspectiveTransformSupport;
import edu.uci.ics.jung.visualization.jai.PerspectiveViewTransformSupport;
import edu.uci.ics.jung.visualization.picking.PickedState;


/**
 * Demonstrates the use of <code>PerspectiveTransform</code>
 * applied to either the model (graph layout) or the view
 * (VisualizationViewer)
 * 
 * @author Tom Nelson
 * 
 */
public class PerspectiveTransformerDemo extends JApplet {

    /**
	 * 
	 */
	private static final long serialVersionUID = -702499007038889493L;

	/**
     * the graph
     */
    Graph<String,Number> graph;
    
    Layout<String,Number> graphLayout;
    
    /**
     * a grid shaped graph
     */
    Graph<String,Number> grid;
    
    Layout<String,Number> gridLayout;

   /**
     * the visual component and renderer for the graph
     */
    VisualizationViewer<String,Number> vv;

    /**
     * provides a Perspective transform for the view
     */
    PerspectiveTransformSupport viewSupport;
    
    /**
     * provides a Perspective transform for the model
     */
    PerspectiveTransformSupport layoutSupport;
    
    /**
     * create an instance of a simple graph with controls to
     * demo the zoom and perspective features.
     * 
     */
    @SuppressWarnings("serial")
	public PerspectiveTransformerDemo() {
        
        // create a simple graph for the demo
        graph = TestGraphs.getOneComponentGraph();

        graphLayout = new FRLayout<String,Number>(graph);
        ((FRLayout<String,Number>)graphLayout).setMaxIterations(1000);

        Dimension preferredSize = new Dimension(600,600);
        Map<String,Point2D> map =
            new HashMap<String,Point2D>();
        Transformer<String,Point2D> vlf = TransformerUtils.mapTransformer(map);
        grid = this.generateVertexGrid(map, preferredSize, 25);
        gridLayout = new StaticLayout<String,Number>(grid, vlf, preferredSize);
        
        final VisualizationModel<String,Number> visualizationModel = 
            new DefaultVisualizationModel<String,Number>(graphLayout, preferredSize);
        vv =  new VisualizationViewer<String,Number>(visualizationModel, preferredSize);
        PickedState<Number> pes = vv.getPickedEdgeState();
        vv.getRenderContext().setEdgeDrawPaintTransformer(new PickableEdgePaintTransformer<Number>(pes, Color.black, Color.red));
        vv.getRenderContext().setVertexShapeTransformer(new Transformer<String,Shape>() {

            public Shape transform(String v) {
                return new Rectangle2D.Float(-10,-10,20,20);
            }});
        vv.setBackground(Color.white);

        // add a listener for ToolTips
        vv.setVertexToolTipTransformer(new ToStringLabeller<String>());
        
        Container content = getContentPane();
        GraphZoomScrollPane gzsp = new GraphZoomScrollPane(vv);
        content.add(gzsp);
        
        /**
         * the regular graph mouse for the normal view
         */
        final DefaultModalGraphMouse<Number, Number> graphMouse = 
            new DefaultModalGraphMouse<Number, Number>();

        vv.setGraphMouse(graphMouse);
        
        viewSupport = new PerspectiveViewTransformSupport<String,Number>(vv);
        layoutSupport = new PerspectiveLayoutTransformSupport<String,Number>(vv);
        
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
        ButtonGroup radio = new ButtonGroup();
        JRadioButton normal = new JRadioButton("None");
        normal.addItemListener(new ItemListener() {
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

        final JRadioButton perspectiveView = new JRadioButton("In View");
        perspectiveView.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                viewSupport.activate(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        final JRadioButton perspectiveModel = new JRadioButton("In Layout");
        perspectiveModel.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                layoutSupport.activate(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        radio.add(normal);
        radio.add(perspectiveModel);
        radio.add(perspectiveView);
        normal.setSelected(true);
        
        ButtonGroup graphRadio = new ButtonGroup();
        JRadioButton graphButton = new JRadioButton("Graph");
        graphButton.setSelected(true);
        graphButton.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    visualizationModel.setGraphLayout(graphLayout);
                    vv.repaint();
                }
            }});
        JRadioButton gridButton = new JRadioButton("Grid");
        gridButton.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    visualizationModel.setGraphLayout(gridLayout);
                    vv.repaint();
                }
            }});
        graphRadio.add(graphButton);
        graphRadio.add(gridButton);
        
        JPanel modePanel = new JPanel(new GridLayout(2,1));
        modePanel.setBorder(BorderFactory.createTitledBorder("Display"));
        modePanel.add(graphButton);
        modePanel.add(gridButton);

        JMenuBar menubar = new JMenuBar();
        menubar.add(graphMouse.getModeMenu());
        gzsp.setCorner(menubar);
        
        Container controls = new JPanel(new BorderLayout());
        JPanel zoomControls = new JPanel(new GridLayout(2,1));
        zoomControls.setBorder(BorderFactory.createTitledBorder("Zoom"));
        JPanel perspectiveControls = new JPanel(new GridLayout(3,1));
        zoomControls.add(plus);
        zoomControls.add(minus);
        perspectiveControls.add(normal);
        perspectiveControls.add(perspectiveModel);
        perspectiveControls.add(perspectiveView);
        
        controls.add(zoomControls, BorderLayout.WEST);
        controls.add(modePanel);
        perspectivePanel.add(perspectiveControls, BorderLayout.WEST);
        perspectiveCenterPanel.add(horizontalSlider, BorderLayout.SOUTH);
        perspectivePanel.add(verticalSlider, BorderLayout.EAST);
        perspectiveCenterPanel.add(center);
        perspectivePanel.add(perspectiveCenterPanel);
        controls.add(perspectivePanel, BorderLayout.EAST);

        content.add(controls, BorderLayout.SOUTH);
    }

    private Graph<String,Number> generateVertexGrid(Map<String,Point2D> map,
            Dimension d, int interval) {
        int count = d.width/interval * d.height/interval;
        Graph<String,Number> graph = new SparseMultigraph<String,Number>();
        String[] v = new String[count];
        for(int i=0; i<count; i++) {
            int x = interval*i;
            int y = x / d.width * interval;
            x %= d.width;
            
            Point2D location = new Point2D.Float(x, y);
            v[i] = ""+i;
            map.put(v[i], location);
            graph.addVertex(v[i]);
        }
        return graph;
    }

    /**
     * a driver for this demo
     */
    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(new PerspectiveTransformerDemo());
        f.pack();
        f.setVisible(true);
    }
}
