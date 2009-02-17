/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on May 4, 2005
 */

package edu.uci.ics.jung.visualization;

import java.awt.Dimension;

import javax.swing.event.ChangeListener;

import edu.uci.ics.jung.utils.ChangeEventSupport;

/**
 * Interface for the state holding model of the VisualizationViewer.
 * Refactored and extracted from the 1.6.0 version of VisualizationViewer
 * 
 * @author Tom Nelson - RABA Technologies
 */
public interface VisualizationModel extends ChangeEventSupport {

    /**
     * 
     * @return the sleep time of the relaxer thread
     */
    long getRelaxerThreadSleepTime();
    
    /**
     * set a callback to be called during the relaxer iteration
     * @param scb
     */
	void setTextCallback(StatusCallback scb);

    /**
     * restart the layout
     */
    void restart();
    
    /**
     * initialize the layout
     *
     */
    void init();
    
    /**
     * start the relaxer
     *
     */
    void start();
    
    /**
     * suspend the relaxer
     *
     */
    void suspend();
    /**
     * unsuspend the relaxer
     *
     */
    void unsuspend();
    /**
     * iterate over the layout algorithm prior to displaying the graph
     *
     */
    void prerelax();
    
    /**
     * Sets the relaxerThreadSleepTime. @see #getRelaxerThreadSleepTime()
     * @param relaxerThreadSleepTime The relaxerThreadSleepTime to set.
     */
    void setRelaxerThreadSleepTime(long relaxerThreadSleepTime);

    /**
     * set the graph Layout
     * @param layout
     */
    void setGraphLayout(Layout layout);
    
    /**
     * Sets the graph Layout and initialize the Layout size to
     * the passed dimensions. The passed Dimension will often be
     * the size of the View that will display the graph.
     * @param layout
     * @param d
     */
    void setGraphLayout(Layout layout, Dimension d);

    /**
     * Returns the current graph layout.
     */
    Layout getGraphLayout();

    void restartThreadOnly();

    /**
     * Returns a flag that says whether the visRunner thread is running. If
     * it is not, then you may need to restart the thread. 
     */
    boolean isVisRunnerRunning();
    
    /**
     * Request that the relaxer be stopped. The Thread
     * will terminate.
     */
    void stop();

    /**
     * Register <code>l</code> as a listeners to changes in the model. The View registers
     * in order to repaint itself when the model changes.
     */
    void addChangeListener(ChangeListener l);

    /**
     * Removes a ChangeListener.
     * @param l the listener to be removed
     */
    void removeChangeListener(ChangeListener l);

    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     */
    ChangeListener[] getChangeListeners();

}