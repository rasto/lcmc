/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
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

package lcmc.gui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lcmc.utilities.Tools;

/**
 * @author Rasto Levrinc
 */
public final class Check {
    private String toolTipCache = null;
    private final Collection<Check> subChecks = new ArrayList<Check>();
    private final List<String> incorrectFields;
    private final List<String> changedFields;

    /** Prepares new {@code Check} object. */
    public Check(final List<String> incorrectFields,
                 final List<String> changedFields) {
        super();
        this.incorrectFields = incorrectFields;
        this.changedFields = changedFields;
    }

    public void addCheck(final Check subCheck) {
        subChecks.add(subCheck);
        toolTipCache = null;
    }

    public void addChanged(final String changed) {
        changedFields.add(changed);
        toolTipCache = null;
    }

    public void addIncorrect(final String incorrect) {
        incorrectFields.add(incorrect);
        toolTipCache = null;
    }

    /** All the fields are correct. */
    public boolean isCorrect() {
        if (incorrectFields != null && !incorrectFields.isEmpty()) {
            return false;
        }
        for (final Check subCheck : subChecks) {
            if (!subCheck.isCorrect()) {
                return false;
            }
        }
        return true;
    }

    /** At least one field has changed. */
    public boolean isChanged() {
        if (changedFields != null && !changedFields.isEmpty()) {
            return true;
        }
        for (final Check subCheck : subChecks) {
            if (subCheck.isChanged()) {
                return true;
            }
        }
        return false;
    }
    public String getToolTip() {
        if (toolTipCache == null) {
            final StringBuilder tt = new StringBuilder();
            tt.append("<html>")
              .append(getToolTipInside())
              .append("</html>");
            toolTipCache = tt.toString();
        }
        return toolTipCache;
    }

    public CharSequence getToolTipInside() {
        final StringBuilder toolTip = new StringBuilder();
        if (!incorrectFields.isEmpty()) {
            toolTip.append("incorrect: ")
                   .append(Tools.join(" ", incorrectFields))
                   .append("<br>");
        }
        if (!changedFields.isEmpty()) {
            toolTip.append("changed: ")
                   .append(Tools.join(" ", changedFields))
                   .append("<br>");
        }
        for (final Check subCheck : subChecks) {
            final CharSequence subToolTip = subCheck.getToolTipInside();
            if (subToolTip.length() > 0) {
                toolTip.append("<p>")
                       .append(subCheck.getToolTipInside())
                       .append("<p>");
            }
        }
        return toolTip;
    }
}
