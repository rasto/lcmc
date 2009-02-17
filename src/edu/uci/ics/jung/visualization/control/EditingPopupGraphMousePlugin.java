/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */
/**
 * 
 */
package edu.uci.ics.jung.visualization.control;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PickSupport;
import edu.uci.ics.jung.visualization.PickedState;
import edu.uci.ics.jung.visualization.SettableVertexLocationFunction;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * a plugin that uses popup menus to create vertices, undirected edges,
 * and directed edges.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 */
public class EditingPopupGraphMousePlugin extends AbstractPopupGraphMousePlugin {
    
    SettableVertexLocationFunction vertexLocations;
    
    public EditingPopupGraphMousePlugin(SettableVertexLocationFunction vertexLocations) {
        this.vertexLocations = vertexLocations;
    }

    protected void handlePopup(MouseEvent e) {
        final VisualizationViewer vv =
            (VisualizationViewer)e.getSource();
        final Layout layout = vv.getGraphLayout();
        final Graph graph = layout.getGraph();
        final Point2D p = e.getPoint();
        final Point2D ivp = vv.inverseViewTransform(e.getPoint());
        PickSupport pickSupport = vv.getPickSupport();
        if(pickSupport != null) {
            
            final Vertex vertex = pickSupport.getVertex(ivp.getX(), ivp.getY());
            final Edge edge = pickSupport.getEdge(ivp.getX(), ivp.getY());
            final PickedState pickedState = vv.getPickedState();
            JPopupMenu popup = new JPopupMenu();
            
            if(vertex != null) {
                Set picked = pickedState.getPickedVertices();
                if(picked.size() > 0) {
                    JMenu directedMenu = new JMenu("Create Directed Edge");
                    popup.add(directedMenu);
                    for(Iterator iterator=picked.iterator(); iterator.hasNext(); ) {
                        final Vertex other = (Vertex)iterator.next();
                        directedMenu.add(new AbstractAction("["+other+","+vertex+"]") {
                            public void actionPerformed(ActionEvent e) {
                                Edge newEdge = new DirectedSparseEdge(other, vertex);
                                graph.addEdge(newEdge);
                                vv.repaint();
                            }
                        });
                    }
                    JMenu undirectedMenu = new JMenu("Create Undirected Edge");
                    popup.add(undirectedMenu);
                    for(Iterator iterator=picked.iterator(); iterator.hasNext(); ) {
                        final Vertex other = (Vertex)iterator.next();
                        undirectedMenu.add(new AbstractAction("[" + other+","+vertex+"]") {
                            public void actionPerformed(ActionEvent e) {
                                Edge newEdge = new UndirectedSparseEdge(other, vertex);
                                graph.addEdge(newEdge);
                                vv.repaint();
                            }
                        });
                    }
                }
                popup.add(new AbstractAction("Delete Vertex") {
                    public void actionPerformed(ActionEvent e) {
                        pickedState.pick(vertex, false);
                        graph.removeVertex(vertex);
                        vv.repaint();
                    }});
            } else if(edge != null) {
                popup.add(new AbstractAction("Delete Edge") {
                    public void actionPerformed(ActionEvent e) {
                        pickedState.pick(edge, false);
                        graph.removeEdge(edge);
                        vv.repaint();
                    }});
            } else {
                popup.add(new AbstractAction("Create Vertex") {
                    public void actionPerformed(ActionEvent e) {
                        Vertex newVertex = new SparseVertex();
                        vertexLocations.setLocation(newVertex, vv.inverseTransform(p));
                        Layout layout = vv.getGraphLayout();
                        for(Iterator iterator=graph.getVertices().iterator(); iterator.hasNext(); ) {
                            layout.lockVertex((Vertex)iterator.next());
                        }
                        graph.addVertex(newVertex);
                        vv.getModel().restart();
                        for(Iterator iterator=graph.getVertices().iterator(); iterator.hasNext(); ) {
                            layout.unlockVertex((Vertex)iterator.next());
                        }
                        vv.repaint();
                    }
                });
            }
            if(popup.getComponentCount() > 0) {
                popup.show(vv, e.getX(), e.getY());
            }
        }
    }
}