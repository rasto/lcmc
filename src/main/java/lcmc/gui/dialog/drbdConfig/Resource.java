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


package lcmc.gui.dialog.drbdConfig;

import lcmc.utilities.Tools;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.DrbdInfo;
import lcmc.gui.resources.DrbdResourceInfo;
import lcmc.gui.resources.DrbdVolumeInfo;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;

import java.util.Map;
import java.util.HashMap;
import java.awt.Component;
import java.awt.Dimension;

/**
 * An implementation of a dialog where user can enter drbd resource
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Resource extends DrbdConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Wfc timeout option string. */
    private static final String WFC_TIMEOUT_PARAM = "wfc-timeout";
    /** Degr wfc timeout option string. */
    private static final String DEGR_WFC_TIMEOUT_PARAM = "degr-wfc-timeout";

    /** Protocol option string. */
    private static final String PROTOCOL = "protocol";
    /** Allow two primaries string. */
    private static final String ALLOW_TWO_PRIMARIES =
                                                  "allow-two-primaries";
    /** cram-hmac-alg option string. */
    private static final String CRAM_HMAC_ALG = "cram-hmac-alg";
    /** shared-secret option string. */
    private static final String SHARED_SECRET = "shared-secret";
    /** on-io-error option string. */
    private static final String ON_IO_ERROR = "on-io-error";
    /** Common configuration options. */
    private static final String[] COMMON_PARAMS = {PROTOCOL,
                                                   CRAM_HMAC_ALG,
                                                   SHARED_SECRET,
                                                   WFC_TIMEOUT_PARAM,
                                                   DEGR_WFC_TIMEOUT_PARAM,
                                                   ON_IO_ERROR};
    /** Configuration options of the drbd resource. */
    private static final String[] PARAMS = {"name",
                                            PROTOCOL,
                                            ALLOW_TWO_PRIMARIES,
                                            CRAM_HMAC_ALG,
                                            SHARED_SECRET,
                                            WFC_TIMEOUT_PARAM,
                                            DEGR_WFC_TIMEOUT_PARAM,
                                            ON_IO_ERROR};
    /** Length of the secret string. */
    private static final int SECRET_STRING_LENGTH = 32;

    /** Prepares a new <code>Resource</code> object. */
    public Resource(final WizardDialog previousDialog,
                    final DrbdVolumeInfo dvi) {
        super(previousDialog, dvi);
    }

    /** Returns a string with SECRET_STRING_LENGTH random characters. */
    private String getRandomSecret() {
        return Tools.getRandomSecret(SECRET_STRING_LENGTH);
    }

    /** Applies the changes and returns next dialog (BlockDev). */
    @Override
    public WizardDialog nextDialog() {
        final DrbdResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        final DrbdInfo drbdInfo = dri.getDrbdInfo();
        final boolean protocolInNetSection = drbdInfo.atLeastVersion("8.4");
        if (drbdInfo.getDrbdResources().size() <= 1) {
            for (final String commonP : COMMON_PARAMS) {
                if (!protocolInNetSection
                    && PROTOCOL.equals(commonP)) {
                    continue;
                }
                final String value = dri.getComboBoxValue(commonP);
                drbdInfo.getResource().setValue(commonP, value);
                drbdInfo.getWidget(commonP, null).setValue(value);
            }
        }
        Tools.waitForSwing();
        drbdInfo.apply(false);
        dri.apply(false);
        return new Volume(this, getDrbdVolumeInfo());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.DrbdConfig.Resource.Title in TextResources.
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.Resource.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.DrbdConfig.Resource.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.Resource.Description");
    }

    /** Inits dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        final DrbdResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        final boolean ch = dri.checkResourceFieldsChanged(null, PARAMS);
        final boolean cor = dri.checkResourceFieldsCorrect(null, PARAMS);
        if (cor) {
            enableComponents();
        } else {
            /* don't enable */
            enableComponents(new JComponent[]{buttonClass(nextButton())});
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                makeDefaultButton(buttonClass(nextButton()));
            }
        });
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    pressNextButton();
                }
            });
        }
    }

    /** Returns input pane where user can configure a drbd resource. */
    @Override
    protected JComponent getInputPane() {
        final DrbdResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        final DrbdInfo drbdInfo = dri.getDrbdInfo();
        dri.getInfoPanel();
        dri.waitForInfoPanel();
        Tools.waitForSwing();
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        /* common options */
        final Map<String, String> commonPreferredValue =
                                                new HashMap<String, String>();
        commonPreferredValue.put(PROTOCOL, "C");
        commonPreferredValue.put(DEGR_WFC_TIMEOUT_PARAM, "0");
        commonPreferredValue.put(CRAM_HMAC_ALG, "sha1");
        commonPreferredValue.put(SHARED_SECRET, getRandomSecret());
        commonPreferredValue.put(ON_IO_ERROR, "detach");
        if (drbdInfo.getDrbdResources().size() <= 1) {
            for (final String commonP : COMMON_PARAMS) {
                /* for the first resource set common options. */
                final String commonValue =
                                      drbdInfo.getResource().getValue(commonP);
                if (commonPreferredValue.containsKey(commonP)) {
                    final String defaultValue =
                               drbdInfo.getParamDefault(commonP);
                    if ((defaultValue == null && "".equals(commonValue))
                        || (defaultValue != null
                            && defaultValue.equals(commonValue))) {
                        drbdInfo.getWidget(commonP, null).setValue(
                                            commonPreferredValue.get(commonP));
                        dri.getResource().setValue(
                                            commonP,
                                            commonPreferredValue.get(commonP));
                    } else {
                        dri.getResource().setValue(commonP, commonValue);
                    }
                }
            }
        } else {
            /* resource options, if not defined in common section. */
            for (final String commonP : COMMON_PARAMS) {
                final String commonValue =
                                      drbdInfo.getResource().getValue(commonP);
                if ("".equals(commonValue)
                    && commonPreferredValue.containsKey(commonP)) {
                    dri.getResource().setValue(
                                            commonP,
                                            commonPreferredValue.get(commonP));
                }
            }
        }


        /* address combo boxes */
        dri.addHostAddresses(optionsPanel,
                             ClusterBrowser.SERVICE_LABEL_WIDTH,
                             ClusterBrowser.SERVICE_FIELD_WIDTH,
                             true,
                             buttonClass(nextButton()));
        dri.addWizardParams(
                  optionsPanel,
                  PARAMS,
                  buttonClass(nextButton()),
                  Tools.getDefaultSize("Dialog.DrbdConfig.Resource.LabelWidth"),
                  Tools.getDefaultSize("Dialog.DrbdConfig.Resource.FieldWidth"),
                  null);

        inputPane.add(optionsPanel);
        final JScrollPane sp = new JScrollPane(inputPane);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        return sp;
    }
}
