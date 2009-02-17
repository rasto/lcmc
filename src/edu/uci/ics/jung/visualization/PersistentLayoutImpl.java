/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Oct 8, 2004
 *
 */
package edu.uci.ics.jung.visualization;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.ChangeEventSupport;
import edu.uci.ics.jung.utils.DefaultChangeEventSupport;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

/**
 * Implementation of PersistentLayout.
 * Defers to another layout until 'restore' is called,
 * then it uses the saved vertex locations
 * 
 * @author Tom Nelson - RABA Technologies
 * 
 *  
 */
public class PersistentLayoutImpl extends LayoutDecorator 
    implements PersistentLayout {

    protected ChangeEventSupport changeSupport =
        new DefaultChangeEventSupport(this);

    /**
     * a container for Vertices
     */
    protected Map map;

    /**
     * a key for this class
     */
    protected Object key;

    /**
     * a collection of Vertices that should not move
     */
    protected Set dontmove;

    /**
     * whether the graph is locked (stops the VisualizationViewer rendering thread)
     */
    protected boolean locked;

    private static final Object BASE_KEY = "edu.uci.ics.jung.Base_Visualization_Key";
    
    protected RadiusGraphElementAccessor elementAccessor;

    /**
     * create an instance with a passed layout
     * create containers for graph components
     * @param layout 
     */
    public PersistentLayoutImpl(Layout layout) {
        super(layout);
        this.map = new HashMap();
        this.dontmove = new HashSet();
        this.elementAccessor = new RadiusGraphElementAccessor(layout);
        if(layout instanceof ChangeEventSupport) {
            ((ChangeEventSupport)layout).addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    fireStateChanged();
                }
            });
        }
    }

    /**
     * This method calls <tt>initialize_local_vertex</tt> for each vertex, and
     * also adds initial coordinate information for each vertex. (The vertex's
     * initial location is set by calling <tt>initializeLocation</tt>.
     */
    protected void initializeLocations() {
        for (Iterator iter = getGraph().getVertices().iterator(); iter
                .hasNext();) {
            Vertex v = (Vertex) iter.next();

            Coordinates coord = (Coordinates) v.getUserDatum(getBaseKey());
            if (coord == null) {
                coord = new Coordinates();
                v.addUserDatum(getBaseKey(), coord, UserData.REMOVE);
            }
            if (!dontmove.contains(v))
                initializeLocation(v, coord, getCurrentSize());
        }
    }


    /**
     * Sets persisted location for a vertex within the dimensions of the space.
     * If the vertex has not been persisted, sets a random location. If you want
     * to initialize in some different way, override this method.
     * 
     * @param v
     * @param coord
     * @param d
     */
    protected void initializeLocation(Vertex v, Coordinates coord, Dimension d) {
        double x;
        double y;
        Point point = (Point) map.get(new Integer(v.hashCode()));
        if (point == null) {
            x = Math.random() * d.getWidth();
            y = Math.random() * d.getHeight();
        } else {
            x = point.x;
            y = point.y;
        }
        coord.setX(x);
        coord.setY(y);
    }

    /**
     * save the Vertex locations to a file
     * @param fileName the file to save to	
     * @throws an IOException if the file cannot be used
     */
    public void persist(String fileName) throws IOException {
        Set set = getGraph().getVertices();
        for (Iterator iterator = set.iterator(); iterator.hasNext();) {
            Vertex v = (Vertex) iterator.next();
            Point p = new Point(getX(v), getY(v));
            map.put(new Integer(v.hashCode()), p);
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                fileName));
        oos.writeObject(map);
        oos.close();
    }

    /**
     * Restore the graph Vertex locations from a file
     * @param fileName the file to use
     * @throws IOException for file problems
     * @throws ClassNotFoundException for classpath problems
     */
    public void restore(String fileName) throws IOException,
            ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
                fileName));
        map = (Map) ois.readObject();
        ois.close();
        initializeLocations();
        locked = true;
    }

    public void lock(boolean locked) {
        this.locked = locked;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.uci.ics.jung.visualization.Layout#incrementsAreDone()
     */
    public boolean incrementsAreDone() {
        return locked;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.uci.ics.jung.visualization.Layout#lockVertex(edu.uci.ics.jung.graph.Vertex)
     */
    public void lockVertex(Vertex v) {
        dontmove.add(v);
        delegate.lockVertex(v);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.uci.ics.jung.visualization.Layout#unlockVertex(edu.uci.ics.jung.graph.Vertex)
     */
    public void unlockVertex(Vertex v) {
        dontmove.remove(v);
        delegate.unlockVertex(v);
    }

    /**
     * Returns a visualization-specific key (that is, specific to 
     * the layout in use) that can be used to access
     * UserData related to the <tt>AbstractLayout</tt>.
     */
    public Object getBaseKey() {
        if (key == null)
            key = new Pair(delegate, BASE_KEY);
        return key;
    }
    
    public void update() {
        if(delegate instanceof LayoutMutable) {
            ((LayoutMutable)delegate).update();
        }
    }

    public void addChangeListener(ChangeListener l) {
        if(delegate instanceof ChangeEventSupport) {
            ((ChangeEventSupport)delegate).addChangeListener(l);
        }
        changeSupport.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        if(delegate instanceof ChangeEventSupport) {
            ((ChangeEventSupport)delegate).removeChangeListener(l);
        }
        changeSupport.removeChangeListener(l);
    }

    public ChangeListener[] getChangeListeners() {
        return changeSupport.getChangeListeners();
    }

    public void fireStateChanged() {
        changeSupport.fireStateChanged();
    }
}