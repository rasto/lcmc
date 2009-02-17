/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * 
 * Created on Oct 29, 2003
 */
package edu.uci.ics.jung.utils;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.apache.commons.collections.IteratorUtils;

/**
 * An array-based binary heap implementation of a priority queue, 
 * which also provides
 * efficient <code>update()</code> and <code>contains</code> operations.
 * It contains extra infrastructure (a hash table) to keep track of the 
 * position of each element in the array; thus, if the key value of an element
 * changes, it may be "resubmitted" to the heap via <code>update</code>
 * so that the heap can reposition it efficiently, as necessary.  
 * 
 * @author Joshua O'Madadhain
 */
public class MapBinaryHeap 
    extends AbstractCollection 
    implements Collection
{
	private Vector heap;            // holds the heap as an implicit binary tree
    private HashMap object_indices; // maps each object in the heap to its index in the heap
    private Comparator comp;
    private final static int TOP = 0;   // the index of the top of the heap

    /**
     * Creates a <code>MapBinaryHeap</code> whose heap ordering
     * is based on the ordering of the elements specified by <code>c</code>.
     */
    public MapBinaryHeap(Comparator comp)
    {
        initialize(comp);
    }
    
    /**
     * Creates a <code>MapBinaryHeap</code> whose heap ordering
     * will be based on the <i>natural ordering</i> of the elements,
     * which must be <code>Comparable</code>.
     */
    public MapBinaryHeap()
    {
        initialize(new ComparableComparator());
    }

    /**
     * Creates a <code>MapBinaryHeap</code> based on the specified
     * collection whose heap ordering
     * will be based on the <i>natural ordering</i> of the elements,
     * which must be <code>Comparable</code>.
     */
    public MapBinaryHeap(Collection c)
    {
        this();
        addAll(c);
    }
    
    /**
     * Creates a <code>MapBinaryHeap</code> based on the specified collection 
     * whose heap ordering
     * is based on the ordering of the elements specified by <code>c</code>.
     */
    public MapBinaryHeap(Collection c, Comparator comp)
    {
        this(comp);
        addAll(c);
    }
    
    private void initialize(Comparator comp)
    {
        this.comp = comp;
        object_indices = new HashMap();
        heap = new Vector();
    }
    
	/**
	 * @see Collection#clear()
	 */
	public void clear()
	{
        object_indices = new HashMap();
        heap = new Vector();
	}

	/**
	 * Inserts <code>o</code> into this collection.
	 */
	public boolean add(Object o)
	{
        int i = heap.size();  // index 1 past the end of the heap
        heap.setSize(i+1);
        percolateUp(i, o);
        return true;
	}

	/**
	 * Returns <code>true</code> if this collection contains no elements, and
     * <code>false</code> otherwise.
	 */
	public boolean isEmpty()
	{
        return heap.isEmpty();
	}

	/**
	 * Returns the element at the top of the heap; does not
     * alter the heap.
	 */
	public Object peek() throws NoSuchElementException
	{
        return heap.elementAt(TOP);
	}

	/**
	 * Removes the element at the top of this heap, and returns it.
	 */
	public Object pop() throws NoSuchElementException
	{
        Object top = heap.elementAt(TOP);
        if (top == null)
        	return top;
        
        Object bottom_elt = heap.lastElement();
        heap.setElementAt(bottom_elt, TOP);
        object_indices.put(bottom_elt, new Integer(TOP));
        
        heap.setSize(heap.size() - 1);  // remove the last element
        if (heap.size() > 1)
        	percolateDown(TOP);

        object_indices.remove(top);
        return top;
	}

    /**
     * Returns the size of this heap.
     */
    public int size() 
    {
        return heap.size();
    }
       
    /**
     * Informs the heap that this object's internal key value has been
     * updated, and that its place in the heap may need to be shifted
     * (up or down).
     * @param o
     */
    public void update(Object o)
    {
        // Since we don't know whether the key value increased or 
        // decreased, we just percolate up followed by percolating down;
        // one of the two will have no effect.
        
        int cur = ((Integer)object_indices.get(o)).intValue(); // current index
        int new_idx = percolateUp(cur, o);
        percolateDown(new_idx);
    }

    /**
     * @see Collection#contains(java.lang.Object)
     */
    public boolean contains(Object o)
    {
        return object_indices.containsKey(o);
    }
    
    /**
     * Moves the element at position <code>cur</code> closer to 
     * the bottom of the heap, or returns if no further motion is
     * necessary.  Calls itself recursively if further motion is 
     * possible.
     */
    private void percolateDown(int cur)
    {
        int left = lChild(cur);
        int right = rChild(cur);
        int smallest;

        if ((left < heap.size()) && (comp.compare(heap.elementAt(left), heap.elementAt(cur)) < 0))
        	smallest = left;
        else
        	smallest = cur;

        if ((right < heap.size()) && (comp.compare(heap.elementAt(right), heap.elementAt(smallest)) < 0))
            smallest = right;

        if (cur != smallest)
        {
            swap(cur, smallest);
            percolateDown(smallest);
        }
    }

    /**
     * Moves the element <code>o</code> at position <code>cur</code> 
     * as high as it can go in the heap.  Returns the new position of the 
     * element in the heap.
     */
    private int percolateUp(int cur, Object o)
    {
        int i = cur;
        
        while ((i > TOP) && (comp.compare(heap.elementAt(parent(i)), o) > 0))
        {
            Object parentElt = heap.elementAt(parent(i));
            heap.setElementAt(parentElt, i);
            object_indices.put(parentElt, new Integer(i));  // reset index to i (new location)
            i = parent(i);
        }
        
        // place object in heap at appropriate place
        object_indices.put(o, new Integer(i));
        heap.setElementAt(o, i);

        return i;
    }
    
    /**
     * Returns the index of the left child of the element at 
     * index <code>i</code> of the heap.
     * @param i
     * @return
     */
    private int lChild(int i)
    {
    	return (i<<1) + 1;
    }
    
    /**
     * Returns the index of the right child of the element at 
     * index <code>i</code> of the heap.
     * @param i
     * @return
     */
    private int rChild(int i)
    {
    	return (i<<1) + 2;
    }
    
    /**
     * Returns the index of the parent of the element at 
     * index <code>i</code> of the heap.
     * @param i
     * @return
     */
    private int parent(int i)
    {
    	return (i-1)>>1;
    }
    
    /**
     * Swaps the positions of the elements at indices <code>i</code>
     * and <code>j</code> of the heap.
     * @param i
     * @param j
     */
    private void swap(int i, int j)
    {
        Object iElt = heap.elementAt(i);
        Object jElt = heap.elementAt(j);

        heap.setElementAt(jElt, i);
        object_indices.put(jElt, new Integer(i));

        heap.setElementAt(iElt, j);
        object_indices.put(iElt, new Integer(j));
    }
    
    /**
     * Comparator used if none is specified in the constructor.
     * @author Joshua O'Madadhain
     */
    private class ComparableComparator implements Comparator
    {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object arg0, Object arg1)
        {
            if (!(arg0 instanceof Comparable) || !(arg1 instanceof Comparable))
                throw new IllegalArgumentException("Arguments must be Comparable");
            Comparable i1 = (Comparable)arg0;
            Comparable i2 = (Comparable)arg1;
            
            return i1.compareTo(i2);
        }
    }

    /**
     * Returns an <code>Iterator</code> that does not support modification
     * of the heap.
     */
    public Iterator iterator()
    {
        return IteratorUtils.unmodifiableIterator(heap.iterator());
    }

    /**
     * This data structure does not support the removal of arbitrary elements.
     */
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This data structure does not support the removal of arbitrary elements.
     */
    public boolean removeAll(Collection c)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This data structure does not support the removal of arbitrary elements.
     */
    public boolean retainAll(Collection c)
    {
        throw new UnsupportedOperationException();
    }

}
