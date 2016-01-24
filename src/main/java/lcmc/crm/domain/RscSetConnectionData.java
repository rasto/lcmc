/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2016, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.crm.domain;

/** Class that holds data between two resource sests. */
public class RscSetConnectionData {
    private CrmXml.RscSet rscSet1;
    private CrmXml.RscSet rscSet2;
    private String constraintId;
    /** Position in the resource set. */
    private final int connectionPos;
    private final boolean colocation;

    public RscSetConnectionData(final CrmXml.RscSet rscSet1,
                                final CrmXml.RscSet rscSet2,
                                final String constraintId,
                                final int connectionPos,
                                final boolean colocation) {
        this.rscSet1 = rscSet1;
        this.rscSet2 = rscSet2;
        this.constraintId = constraintId;
        this.connectionPos = connectionPos;
        this.colocation = colocation;
    }

    public CrmXml.RscSet getRscSet1() {
        return rscSet1;
    }

    public CrmXml.RscSet getRscSet2() {
        return rscSet2;
    }

    public String getConstraintId() {
        return constraintId;
    }

    public void setConstraintId(final String constraintId) {
        this.constraintId = constraintId;
    }

    public boolean isColocation() {
        return colocation;
    }

    private boolean rscSetsAreEqual(final CrmXml.RscSet set1, final CrmXml.RscSet set2) {
        if (set1 == set2) {
            return true;
        }
        if (set1 == null || set2 == null) {
            return false;
        }
        return set1.equals(set2);
    }

    public boolean equals(final RscSetConnectionData oRdata) {
        final CrmXml.RscSet oRscSet1 = oRdata.getRscSet1();
        final CrmXml.RscSet oRscSet2 = oRdata.getRscSet2();
        return oRdata.isColocation() == colocation
               && rscSetsAreEqual(rscSet1, oRscSet1)
               && rscSetsAreEqual(rscSet2, oRscSet2);
    }

    public boolean equalsAlthoughReversed(final RscSetConnectionData oRdata) {
        final CrmXml.RscSet oRscSet1 = oRdata.getRscSet1();
        final CrmXml.RscSet oRscSet2 = oRdata.getRscSet2();
        return oRdata.isColocation() == colocation
               /* when it's reversed. */
               && ((rscSet1 == null && oRscSet2 == null && rscSetsAreEqual(rscSet2, oRscSet1))
                    || (rscSet2 == null && oRscSet1 == null && rscSetsAreEqual(rscSet1, oRscSet2)));
    }

    public boolean canUseSamePlaceholder(final RscSetConnectionData oRdata) {
        if (oRdata.isColocation() == colocation) {
            /* exactly the same */
            return equals(oRdata);
        }
        final CrmXml.RscSet oRscSet1 = oRdata.getRscSet1();
        final CrmXml.RscSet oRscSet2 = oRdata.getRscSet2();
        /* is subset only if both are zero */
        if ((rscSet1 == oRscSet1
             || rscSet1 == null
             || oRscSet1 == null
             || rscSet1.isSubsetOf(oRscSet1)
             || oRscSet1.isSubsetOf(rscSet1))
            && (rscSet2 == oRscSet2
                || rscSet2 == null
                || oRscSet2 == null
                || rscSet2.isSubsetOf(oRscSet2)
                || oRscSet2.isSubsetOf(rscSet2))) {
             /* at least one subset without rscset being null. */
            if ((rscSet1 != null && rscSet1.isSubsetOf(oRscSet1))
                || (oRscSet1 != null && oRscSet1.isSubsetOf(rscSet1))
                || (rscSet2 != null && rscSet2.isSubsetOf(oRscSet2))
                || (oRscSet2 != null && oRscSet2.isSubsetOf(rscSet2))) {
                return true;
            }
        }
        if ((rscSet1 == oRscSet2
             || rscSet1 == null
             || oRscSet2 == null
             || rscSet1.isSubsetOf(oRscSet2)
             || oRscSet2.isSubsetOf(rscSet1))
            && (rscSet2 == oRscSet1
                || rscSet2 == null
                || oRscSet1 == null
                || rscSet2.isSubsetOf(oRscSet1)
                || oRscSet1.isSubsetOf(rscSet2))) {

            if ((rscSet1 != null && rscSet1.isSubsetOf(oRscSet2))
                || (oRscSet2 != null && oRscSet2.isSubsetOf(rscSet1))
                || (rscSet2 != null && rscSet2.isSubsetOf(oRscSet1))
                || (oRscSet1 != null && oRscSet1.isSubsetOf(rscSet2))) {
                return true;
            }
        }
        return false;
    }

    /** Reverse resource sets. */
    public void reverse() {
        final CrmXml.RscSet old1 = rscSet1;
        rscSet1 = rscSet2;
        rscSet2 = old1;
    }

    /** Returns whether it is an empty connection. */
    public boolean isEmpty() {
        return (rscSet1 == null || rscSet1.isRscIdsEmpty()) && (rscSet2 == null || rscSet2.isRscIdsEmpty());
    }

    /** Returns connection position. */
    public int getConnectionPos() {
        return connectionPos;
    }

    /** String represantation of the resource set data. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(100);
        s.append("rsc set conn id: ");
        s.append(constraintId);
        if (colocation) {
            s.append(" (colocation)");
        } else {
            s.append(" (order)");
        }
        s.append("\n   (rscset1: ");
        if (rscSet1 == null) {
            s.append("null");
        } else {
            s.append(rscSet1.toString());
        }
        s.append(") \n   (rscset2: ");
        if (rscSet2 == null) {
            s.append("null");
        } else {
            s.append(rscSet2.toString());
        }
        s.append(") ");
        return s.toString();
    }
}
