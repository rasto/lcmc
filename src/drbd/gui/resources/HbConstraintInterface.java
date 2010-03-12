/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package drbd.gui.resources;

import drbd.data.Host;
import drbd.data.resources.Service;
import drbd.gui.GuiComboBox;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Interface for either order or colocation constraint.
 */
public interface HbConstraintInterface {
    /** Returns true if it is order, false if colocation. */
    boolean isOrder();
    /**
     * Returns all parameters.
     */
    String[] getParametersFromXML();
    /**
     * Adds parameters to the options panel and extra options panel for
     * advanced options.
     */
    void addParams(final JPanel optionsPanel,
                   final String[] params,
                   final int leftWidth,
                   final int rightWidth,
                   final Map<String, GuiComboBox> sameAsFields);
    /**
     * Check fields and return true if all are correct and at least one have
     * changed.
     */
    boolean checkResourceFields(final String param, final String[] params);
    /**
     * Check which fields are correct and return true if all are.
     */
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params);
    /**
     * Check which fields have changed and sets fancy colors and returns true
     * if at least one field have changed.
     */
    boolean checkResourceFieldsChanged(final String param,
                                       final String[] params);
    /**
     * Applies the changes after apply button was pressed.
     */
    void apply(final Host dcHost, final boolean testOnly);
    /**
     * Returns data object of this info.
     */
    Service getService();
    /**
     * Adds field with left and right component to the specified panel.
     */
    void addField(final JPanel panel,
                  final JComponent left,
                  final JComponent right,
                  final int leftWidth,
                  final int rightWidth,
                  final int height);
    /**
     * Adds label and field to the specified panel.
     */
    void addLabelField(final JPanel panel,
                       final String left,
                       final String right,
                       final int leftWidth,
                       final int rightWidth,
                       final int height);
    /**
     * Returns name of this constraint: colocation or order.
     */
    String getName();
    /**
     * Returns resource 1.
     */
    String getRsc1();
    /**
     * Returns resource 2.
     */
    String getRsc2();
}
