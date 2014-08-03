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


package lcmc.gui.dialog;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import lcmc.utilities.Tools;

/**
 * Creates confirm dialog with yes and no options.
 * isPressedYesButton() returns true if 'yes' option was pressed.
 * Use confirmDialog function in utilities/Tools class to use
 * this dialog.
 *
 */
public final class ConfirmDialog extends ConfigDialog {
    private final String description;
    private final String title;
    private final String yesButton;
    private final String noButton;

    public ConfirmDialog(final String title, final String description, final String yesButton, final String noButton) {
        super();
        this.title       = title;
        this.description = description;
        this.yesButton   = yesButton;
        this.noButton    = noButton;
    }

    /**
     * Returns the text of the yes button. ConfirmDialog.Yes from TextResources
     * by default.
     */
    protected String yesButton() {
        if (yesButton == null) {
            return Tools.getString("ConfirmDialog.Yes");
        } else {
            return yesButton;
        }
    }

    /**
     * Returns the text of the no button. ConfirmDialog.No from TextResources
     * by default.
     */
    protected String noButton() {
        if (noButton == null) {
            return Tools.getString("ConfirmDialog.No");
        } else {
            return noButton;
        }
    }

    /** Returns the default button. It is "No" by default. */
    @Override
    protected String defaultButton() {
        return noButton();
    }

    /** Returns an icon for this dialog. Null by default. */
    @Override
    protected ImageIcon icon() {
        return null;
    }

    /**
     * Returns width of the dialog.
     * ConfirmDialog.width from AppDefaults by default.
     */
    @Override
    protected int dialogWidth() {
        return Tools.getDefaultInt("ConfirmDialog.width");
    }

    /**
     * Returns height of the dialog.
     * ConfirmDialog.height from AppDefaults by default.
     */
    @Override
    protected int dialogHeight() {
        return Tools.getDefaultInt("ConfirmDialog.height");
    }

    /**
     * Returns the type of the message JOptionPane.WARNING_MESSAGE by default.
     */
    @Override
    protected int getMessageType() {
        return JOptionPane.WARNING_MESSAGE;
    }

    /** Returns true if Yes button was pressed. */
    public boolean isPressedYesButton() {
        return isPressedButton(yesButton());
    }

    /** Returns array with 'yes' and 'no' buttons. */
    @Override
    protected String[] buttons() {
        return new String[]{yesButton(), noButton()};
    }

    /**
     * Returns icons for yes and no buttons. Null, null by default, (for now).
     */
    @Override
    protected ImageIcon[] getIcons() {
        return new ImageIcon[]{null, null};
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(noButton())});
        enableComponents();
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(noButton()).setEnabled(true);
            }
        });
    }

    /** Inits the dialog after it is visible. */
    @Override
    protected void initDialogAfterVisible() {
        super.initDialogAfterVisible();
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(noButton()).requestFocus();
            }
        });
    }

    /**
     * Returns the dialog title. ConfirmDialog.Title from TextResources by
     * default.
     */
    @Override
    protected String getDialogTitle() {
        if (title == null) {
            return Tools.getString("ConfirmDialog.Title");
        } else {
            return title;
        }
    }

    /**
     * Returns description. Description can be specified as argument
     * in the constructor.
     * ConfirmDialog.Description from TextResources by * default.
     */
    @Override
    protected String getDescription() {
        if (description == null) {
            return Tools.getString("ConfirmDialog.Description");
        } else {
            return description;
        }
    }

    /** Returns pane where user input can be defined. */
    @Override
    protected JComponent getInputPane() {
        return null;
    }
}
