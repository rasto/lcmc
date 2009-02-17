/*
 * Created on Apr 18, 2006
 *
 * Copyright (c) 2006, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.TruePredicate;

/**
 * Generates <code>Collection</code>s based on input <code>Collection</code> instances,
 * optionally filtered and sorted according to specified parameters. 
 * 
 * @author Joshua O'Madadhain
 */
public class CollectionFactory
{
    protected static Map collection_data = new HashMap();

    public static Collection getCollection(Collection c)
    {
        CollectionData cd = (CollectionData)collection_data.get(c);
        if (cd == null)
            return c;
        
        return cd.getBackingCollection();
    }

    public static Collection getCollection(Collection c, Comparator comp, Predicate p)
    {
        CollectionData cd = new CollectionData(c, comp, p, true);
        return cd.getBackingCollection();
    }
    
    public static Collection getCollection(Collection c, Comparator comp)
    {
        CollectionData cd = new CollectionData(c, comp, null, true);
        return cd.getBackingCollection();
    }
    
    public static Collection getCollection(Collection c, Predicate p)
    {
        CollectionData id = new CollectionData(c, null, p, true);
        return id.getBackingCollection();
    }

    public static void addCollection(Collection c, Comparator comp, Predicate p, boolean dynamic)
    {
        CollectionData id = new CollectionData(c, comp, p, dynamic);
        collection_data.put(c, id);
    }

    public static void addCollection(Collection c, Comparator comp)
    {
        addCollection(c, comp, null, false);
    }
    
    public static void addCollection(Collection c, Predicate p)
    {
        addCollection(c, null, p, false);
    }

    public static void addCollection(Collection c, Comparator comp, Predicate p)
    {
        addCollection(c, comp, p, false);
    }
    
    /**
     * If <code>dynamic</code> is true, the collection <code>c</code> backing the 
     * <code>Iterator</code> is automatically rebuilt-sorted
     * and/or re-filtered each time <code>getIterator(c)</code> is called.
     * (This is done in case either the collection, the comparator,
     * or the predicate has changed.)
     * Otherwise, the collection is (re)built only when 
     * <code>buildIterator</code> is called.
     * 
     * 
     */
    public static void setDynamic(Collection c, boolean dynamic)
    {
        CollectionData id = (CollectionData)collection_data.get(c);
        id.setDynamic(dynamic);
    }
    
    public static void setComparator(Collection c, Comparator comp)
    {
        CollectionData id = (CollectionData)collection_data.get(c);
        id.setComparator(comp);
    }
    
    public static void setPredicate(Collection c, Predicate p)
    {
        CollectionData id = (CollectionData)collection_data.get(c);
        id.setPredicate(p);
    }

    public static void clear()
    {
        collection_data.clear();
    }
    
    public static void removeCollection(Collection c)
    {
        collection_data.remove(c);
    }
    
    protected static class CollectionData
    {
        /**
         * If <code>is_dynamic</code> is true, the backing collection is automatically re-sorted
         * and/or re-filtered each time an <code>Iterator</code> is requested.
         * (This is done in case either the collection, the comparator,
         * or the predicate has changed.)
         * Otherwise, the collection is (re)built only when 
         * <code>buildBackingCollection</code> is called.
         */
        protected boolean is_dynamic;

        protected Collection collection;
        protected Comparator comp;
        protected Collection backing_collection;
        protected Predicate p;
        
        public CollectionData(Collection c, Comparator comp, Predicate p, boolean dynamic)
        {
            this.collection = c;
            this.comp = comp;
            this.p = p;
            this.is_dynamic = dynamic;
        }

        public void setComparator(Comparator comp)
        {
            if (! this.comp.equals(comp))
                this.backing_collection = null;
            this.comp = comp;
        }

        public void setPredicate(Predicate p)
        {
            if (! this.p.equals(p))
                this.backing_collection = null;
            this.p = p;
        }

        public void setDynamic(boolean dynamic)
        {
            if (! dynamic)
                this.backing_collection = null;
            this.is_dynamic = dynamic;
        }
        
        protected Collection getBackingCollection()
        {
            if (is_dynamic)
                return buildBackingCollection(collection, p, comp);
            else
            {
                if (backing_collection == null)
                    this.backing_collection = buildBackingCollection(collection, p, comp);
                     
                return backing_collection;
            }
        }

        protected List buildBackingCollection(Collection c, Predicate p, Comparator comp)
        {
            List new_backing_collection = new ArrayList(c.size());

            if (p != null && p != TruePredicate.getInstance())
            {
                for (Iterator iter = c.iterator(); iter.hasNext(); )
                {
                    Object o = iter.next();
                    if (p.evaluate(o))
                        new_backing_collection.add(o);
                }
            }

            if (comp != null)
                Collections.sort(new_backing_collection, comp);

            return new_backing_collection;
        }
    }
}
