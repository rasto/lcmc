/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.data;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lcmc.utilities.Tools;

/**
 * This class holds a set of all clusters.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Clusters {
    /** Set of cluster objects. */
    private final Set<Cluster> clusters = new TreeSet<Cluster>();
    /** Clusters set lock. */
    private final ReadWriteLock mClustersLock = new ReentrantReadWriteLock();
    /** Clusters set read lock. */
    private final Lock mClustersReadLock = mClustersLock.readLock();
    /** Clusters set write lock. */
    private final Lock mClustersWriteLock = mClustersLock.writeLock();

    /** Adds cluster to the set of clusters. */
    void addCluster(final Cluster cluster) {
        mClustersWriteLock.lock();
        try {
            clusters.add(cluster);
        } finally {
            mClustersWriteLock.unlock();
        }
    }

    /** Removes cluster from the clusters. */
    void removeCluster(final Cluster cluster) {
        mClustersWriteLock.lock();
        try {
            clusters.remove(cluster);
        } finally {
            mClustersWriteLock.unlock();
        }
    }

    /** Returns true if cluster is in the clusters or false if it is not. */
    boolean isClusterInClusters(final Cluster cluster) {
        mClustersReadLock.lock();
        try {
            return clusters.contains(cluster);
        } finally {
            mClustersReadLock.unlock();
        }
    }

    /** Gets set of clusters. */
    public Set<Cluster> getClusterSet() {
        mClustersReadLock.lock();
        try {
            return new LinkedHashSet<Cluster>(clusters);
        } finally {
            mClustersReadLock.unlock();
        }
    }

    /** Return whether cluster with this name already exists. */
    public boolean isClusterName(final String name) {
        mClustersReadLock.lock();
        try {
            for (final Cluster cluster : clusters) {
                if (name.equals(cluster.getName())) {
                    return true;
                }
            }
            return false;
        } finally {
            mClustersReadLock.unlock();
        }
    }

    /** Return default name with incremented index. */
    public String getDefaultClusterName() {
        return getNextClusterName(Tools.getString("Clusters.DefaultName"));
    }

    /** Return default name with incremented index. */
    public String getNextClusterName(final String defaultName) {
        mClustersReadLock.lock();
        int index = 0;
        try {
            for (final Cluster cluster : clusters) {
                /* find the bigest index of cluster default name and
                 * increment it by one */
                final String name = cluster.getName();
                final Pattern p = Pattern.compile('^'
                                                  + defaultName
                                                  + "(\\d+)$");
                final Matcher m = p.matcher(name);
                if (m.matches()) {
                    final int i = Integer.parseInt(m.group(1));
                    if (i > index) {
                        index = i;
                    }
                }
            }
        } finally {
            mClustersReadLock.unlock();
        }
        return defaultName + Integer.toString(index + 1);
    }
}
