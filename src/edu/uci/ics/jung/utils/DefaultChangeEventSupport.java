/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Aug 18, 2005
 */

package edu.uci.ics.jung.utils;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public class DefaultChangeEventSupport implements ChangeEventSupport {
    
    Object eventSource;
    /**
     * holds the registered listeners
     */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * Only one <code>ChangeEvent</code> is needed
     * instance since the
     * event's only state is the source property.  The source of events
     * generated is always "this".
     */
    protected transient ChangeEvent changeEvent;
    
    public DefaultChangeEventSupport(Object eventSource) {
        this.eventSource = eventSource;
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.utils.ChangeEventSupport#addChangeListener(javax.swing.event.ChangeListener)
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }
    
    /* (non-Javadoc)
     * @see edu.uci.ics.jung.utils.ChangeEventSupport#removeChangeListener(javax.swing.event.ChangeListener)
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }
    
    /* (non-Javadoc)
     * @see edu.uci.ics.jung.utils.ChangeEventSupport#getChangeListeners()
     */
    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])(listenerList.getListeners(
            ChangeListener.class));
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
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ChangeListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(eventSource);
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }          
        }
    }   

}
