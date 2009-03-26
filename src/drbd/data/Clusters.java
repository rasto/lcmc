/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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

package drbd.data;

import java.util.Set;
import java.util.LinkedHashSet;
import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import drbd.utilities.Tools;

/**
 * This class holds a set of all clusters.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Clusters implements Serializable {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Set of cluster objects. */
    private final Set<Cluster> clusters = new LinkedHashSet<Cluster>();

    /**
     * Adds cluster to the set of clusters.
     */
    public final void addCluster(final Cluster cluster) {
        clusters.add(cluster);
    }

    /**
     * removes cluster from the clusters.
     */
    public final void removeCluster(final Cluster cluster) {
        clusters.remove(cluster);
    }

    /**
     * Returns true if cluster is in the clusters or false if it is not.
     */
    public final boolean existsCluster(final Cluster cluster) {
        return clusters.contains(cluster);
    }

    /**
     * Gets set of clusters.
     */
    public final Set<Cluster> getClusterSet() {
        return clusters;
    }

    /**
     * Return default name with incremented index.
     */
    public final String getDefaultClusterName() {
        int index = 0;
        final String defaultName = Tools.getString("ClusterDefaultName");
        if (clusters != null) {
            for (final Cluster cluster : clusters) {
            /* find the bigest index of cluster default name and increment it
             * by one */
                final String name = cluster.getName();
                final Pattern p = Pattern.compile("^"
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
        }
        return defaultName + Integer.toString(index + 1);
    }
}
