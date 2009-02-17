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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import edu.uci.ics.jung.utils.ChangeEventSupport;
import edu.uci.ics.jung.utils.DefaultChangeEventSupport;

/**
 * The model containing state values for 
 * visualizations of graphs. 
 * Refactored and extracted from the 1.6.0 version of VisualizationViewer
 * 
 * @author Tom Nelson
 */
public class DefaultVisualizationModel implements VisualizationModel, ChangeEventSupport {
    
    ChangeEventSupport changeSupport =
        new DefaultChangeEventSupport(this);

    /**
     * a callback called during relaxer iteration
     */
	protected StatusCallback statusCallback;

    /**
	 * the thread that applies the current layout algorithm
	 */
	Thread relaxer;
	
	/**
	 * when <code>true</code>, the relaxer thread will enter a wait state
	 * until unsuspend is called
	 */
	boolean manualSuspend;
	
	/**
	 * the layout algorithm currently in use
	 */
	protected Layout layout;
	
	/**
	 * how long the relaxer thread pauses between iteration loops.
	 */
	protected long relaxerThreadSleepTime = 100L;
	
    protected ChangeListener changeListener;
    
    /**
     * 
     * @param layout The Layout to apply, with its associated Graph
     */
	public DefaultVisualizationModel(Layout layout) {
        this(layout, null);
	}
    
	/**
	 * 
	 * @param layout
	 * @param d The preferred size of the View that will display this graph
	 */
	public DefaultVisualizationModel(Layout layout, Dimension d) {
        if(changeListener == null) {
            changeListener = new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    fireStateChanged();
                }
            };
        }
		setGraphLayout(layout, d);
		init();
	}
	
    /**
     * Returns the time between iterations of the 
     * Relaxer thread. The Relaxer thread sleeps for 
     * a moment before calling the Layout to update
     * again. This tells
     * how long the current delay is. 
     * The default, 20 milliseconds, essentially
     * causes the system to run the next iteration
     * with virtually no pause.
     * @return Returns the relaxerThreadSleepTime.
     */
    public long getRelaxerThreadSleepTime() {
        return relaxerThreadSleepTime;
    }
    
    /**
     * Sets the relaxerThreadSleepTime. 
     * @see #getRelaxerThreadSleepTime()
     * @param relaxerThreadSleepTime The relaxerThreadSleepTime to set.
     */
    public void setRelaxerThreadSleepTime(long relaxerThreadSleepTime) {
        this.relaxerThreadSleepTime = relaxerThreadSleepTime;
    }
    
	/**
	 * Removes the current graph layout, and adds a new one.
	 * @param layout   the new layout to use
	 * @param viewSize the size of the View that will display this layout
	 */
	public void setGraphLayout(Layout layout, Dimension viewSize) {
	    if(this.layout != null && this.layout instanceof ChangeEventSupport) {
	        ((ChangeEventSupport)this.layout).removeChangeListener(changeListener);
        }
        if(viewSize == null) {
            viewSize = new Dimension(600,600);
        }
		suspend();
		Dimension layoutSize = layout.getCurrentSize();
		// if the layout has NOT been initialized yet, initialize it
		// now to the size of the VisualizationViewer window
		if(layoutSize == null) {
		    layout.initialize(viewSize);
		} else {
		    layout.restart();      
        }
		layoutSize = layout.getCurrentSize();

		this.layout = layout;
        if(this.layout instanceof ChangeEventSupport) {
            ((ChangeEventSupport)this.layout).addChangeListener(changeListener);
        }
		prerelax();
		unsuspend();
	}

	/**
	 * set the graph Layout and if it is not already initialized, initialize
	 * it to the default VisualizationViewer preferred size of 600x600
	 */
	public void setGraphLayout(Layout layout) {
	    setGraphLayout(layout, null);
	}

    /**
	 * Returns the current graph layout.
	 */
	public Layout getGraphLayout() {
	        return layout;
	}

    /**
	 * starts a visRunner thread without prerelaxing
	 */
	public synchronized void restartThreadOnly() {
	    if (visRunnerIsRunning ) {
	        stop();
	        //throw new FatalException("Can't init while a visrunner is running");
	    }
		relaxer = new VisRunner();
		relaxer.setPriority(Thread.MIN_PRIORITY);
		relaxer.start();
	}
	
	/**
	 * Pre-relaxes and starts a visRunner thread
	 */
	public synchronized void init() {
	    if (visRunnerIsRunning ) {
	        stop();
	       // throw new FatalException("Can't init while a visrunner is running");
	    }
		prerelax();
		relaxer = new VisRunner();
		relaxer.start();
	}

	/**
	 * Restarts layout, then calls init();
	 */
	public synchronized void restart() {
	    if (visRunnerIsRunning ) {
	        stop();
	        //throw new FatalException("Can't restart while a visrunner is running");
	    }
	    stop = false;
		layout.restart();
		init();
		fireStateChanged();
	}

	/**
	 * Runs the visualization forward a few hundred iterations (for half a 
	 * second)
	 */
	public void prerelax() {
		suspend();

		int i = 0;
		if (layout.isIncremental()) {
			// then increment layout for half a second
			long timeNow = System.currentTimeMillis();
			while (System.currentTimeMillis() - timeNow < 500 && !layout.incrementsAreDone()) {
				i++;
				layout.advancePositions();
			}
		}
		unsuspend();
	}
	
	/**
	 * If the visualization runner is not yet running, kick it off.
	 */
	public synchronized void start() {
		synchronized (pauseObject) {
			pauseObject.notifyAll();
		}
	}

	/**
	 * set a flag to suspend the relaxer thread
	 */
	public synchronized void suspend() {
		manualSuspend = true;
	}

	/**
	 * un-set the suspend flag for the relaxer thead
	 */
	public synchronized void unsuspend() {
		manualSuspend = false;
		synchronized (pauseObject) {
			pauseObject.notifyAll();
		}
	}

	public Object pauseObject = new String("PAUSE OBJECT");

	long[] relaxTimes = new long[5];
	long[] paintTimes = new long[5];
	int relaxIndex = 0;
	int paintIndex = 0;
	double paintfps, relaxfps;


	boolean stop = false;

	boolean visRunnerIsRunning = false; 
	
	/**
	 * the relaxer thread that applies the Layout algorithm to the graph
	 *
	 */
	protected class VisRunner extends Thread {
		public VisRunner() {
			super("Relaxer Thread");
		}

		public void run() {
		    visRunnerIsRunning = true;
		    try {
		        while (!layout.incrementsAreDone() && !stop) {
		            synchronized (pauseObject) {
		                while (manualSuspend && !stop) {
		                    try {
		                        pauseObject.wait();
		                    } catch (InterruptedException e) {
//		                        System.err.println("vis runner wait interrupted");
		                    }
		                }
		            }
		            long start = System.currentTimeMillis();
		            layout.advancePositions();
		            long delta = System.currentTimeMillis() - start;
		            
		            if (stop)
		                return;
		            
		            String status = layout.getStatus();
		            if (statusCallback != null && status != null) {
		                statusCallback.callBack(status);
		            }
		            
		            if (stop)
		                return;
		            
		            relaxTimes[relaxIndex++] = delta;
		            relaxIndex = relaxIndex % relaxTimes.length;
		            relaxfps = average(relaxTimes);
		            
		            if (stop)
		                return;
		            fireStateChanged();
		            
		            if (stop)
		                return;
		            
		            try {
		                sleep(relaxerThreadSleepTime);
		            } catch (InterruptedException ie) {
//		                System.err.println("vis runner sleep insterrupted");
		            }
		        }
		    } finally {
		        visRunnerIsRunning = false;
		    }
		}
	}
	
	/**
	 * Returns a flag that says whether the visRunner thread is running. If
	 * it is not, then you may need to restart the thread.
	 */
	public boolean isVisRunnerRunning() {
	    return visRunnerIsRunning;
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
	 * @param scb
	 */
	public void setTextCallback(StatusCallback scb) {
		this.statusCallback = scb;
	}

	/**
	 * set a flag to stop the VisRunner relaxer thread
	 */
	public synchronized void stop() {
		manualSuspend = false;
		stop = true;
		// interrupt the relaxer, in case it is paused or sleeping
		// this should ensure that visRunnerIsRunning gets set to false
		try { relaxer.interrupt(); }
        catch(Exception ex) {
            // the applet security manager may have prevented this.
            // just sleep for a second to let the thread stop on its own
            System.err.println("got "+ex);
            try { Thread.sleep(1000); }
            catch(InterruptedException ie) {} // swallow
        }
		synchronized (pauseObject) {
			pauseObject.notifyAll();
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
     * The primary listeners will be views that need to be repainted
     * because of changes in this model instance
     * @see EventListenerList
     */
    public void fireStateChanged() {
        changeSupport.fireStateChanged();
    }   
    
}
