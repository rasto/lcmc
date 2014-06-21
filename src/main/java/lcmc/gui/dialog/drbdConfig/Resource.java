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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lcmc.configs.AppDefaults;
import lcmc.data.drbd.DrbdXML;
import lcmc.data.Host;
import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.gui.resources.drbd.ResourceInfo;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.gui.widget.Widget;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where user can enter drbd resource
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Resource extends DrbdConfig {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Resource.class);
    /** Wfc timeout option string. */
    private static final String WFC_TIMEOUT_PARAM = "wfc-timeout";
    /** Degr wfc timeout option string. */
    private static final String DEGR_WFC_TIMEOUT_PARAM = "degr-wfc-timeout";

    /** Allow two primaries string. */
    private static final String ALLOW_TWO_PRIMARIES =
                                                  "allow-two-primaries";
    /** cram-hmac-alg option string. */
    private static final String CRAM_HMAC_ALG = "cram-hmac-alg";
    /** shared-secret option string. */
    private static final String SHARED_SECRET = "shared-secret";
    /** on-io-error option string. */
    private static final String ON_IO_ERROR = "on-io-error";
    /** memlimit proxy option string. */
    private static final String PROXY_MEMLIMIT = "memlimit";
    private static final String PROXY_PLUGIN_ZLIB = "plugin-zlib";
    private static final String PROXY_PLUGIN_LZMA = "plugin-lzma";
    /** Common configuration options. */
    private static final String[] COMMON_PARAMS = {DrbdXML.PROTOCOL_PARAM,
                                                   DrbdXML.PING_TIMEOUT_PARAM,
                                                   CRAM_HMAC_ALG,
                                                   SHARED_SECRET,
                                                   WFC_TIMEOUT_PARAM,
                                                   DEGR_WFC_TIMEOUT_PARAM,
                                                   ON_IO_ERROR,
                                                   PROXY_MEMLIMIT,
                                                   PROXY_PLUGIN_ZLIB,
                                                   PROXY_PLUGIN_LZMA};
    /** Configuration options of the drbd resource. */
    private static final String[] PARAMS = {"name",
                                            DrbdXML.PROTOCOL_PARAM,
                                            DrbdXML.PING_TIMEOUT_PARAM,
                                            ALLOW_TWO_PRIMARIES,
                                            CRAM_HMAC_ALG,
                                            SHARED_SECRET,
                                            WFC_TIMEOUT_PARAM,
                                            DEGR_WFC_TIMEOUT_PARAM,
                                            ON_IO_ERROR,
                                            PROXY_MEMLIMIT,
                                            PROXY_PLUGIN_ZLIB,
                                            PROXY_PLUGIN_LZMA};
    /** Length of the secret string. */
    private static final int SECRET_STRING_LENGTH = 32;
    /** Whether to add proxy host. */
    private boolean proxyHostNextDialog = false;

    /** Prepares a new {@code Resource} object. */
    public Resource(final WizardDialog previousDialog,
                    final VolumeInfo dvi) {
        super(previousDialog, dvi);
    }

    /** Returns a string with SECRET_STRING_LENGTH random characters. */
    private String getRandomSecret() {
        return Tools.getRandomSecret(SECRET_STRING_LENGTH);
    }

    /** Applies the changes and returns next dialog (BlockDev). */
    @Override
    public WizardDialog nextDialog() {
        final ResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        if (proxyHostNextDialog) {
            proxyHostNextDialog = false;
            final Host proxyHost = new Host();
            proxyHost.setCluster(dri.getCluster());
            return new NewProxyHost(this,
                                    proxyHost,
                                    getDrbdVolumeInfo(),
                                    this);
        }
        final GlobalInfo globalInfo = dri.getDrbdInfo();
        final boolean protocolInNetSection = globalInfo.atLeastVersion("8.4");
        if (globalInfo.getDrbdResources().size() <= 1) {
            for (final String commonP : COMMON_PARAMS) {
                if (!protocolInNetSection
                    && DrbdXML.PROTOCOL_PARAM.equals(commonP)) {
                    continue;
                }
                final Widget wi = globalInfo.getWidget(commonP, null);
                if (wi == null) {
                    LOG.appError("widget for param: " + commonP + " was not created");
                    return null;
                }
                final Value value = dri.getComboBoxValue(commonP);
                globalInfo.getResource().setValue(commonP, value);
                wi.setValue(value);
            }
        }
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

    @Override
    protected void initDialogBeforeCreated() {
        final ResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        dri.waitForInfoPanel();
    }

    /** Inits dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        final ResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        final boolean cor = dri.checkResourceFields(null, PARAMS).isCorrect();
        if (cor) {
            enableComponents();
        } else {
            /* don't enable */
            enableComponents(new JComponent[]{buttonClass(nextButton())});
        }
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                makeDefaultButton(buttonClass(nextButton()));
            }
        });
        if (Tools.getApplication().getAutoOptionGlobal("autodrbd") != null) {
            pressNextButton();
        }
    }

    /** Returns input pane where user can configure a drbd resource. */
    @Override
    protected JComponent getInputPane() {
        final ResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        final GlobalInfo globalInfo = dri.getDrbdInfo();
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.LINE_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        /* common options */
        final Map<String, Value> commonPreferredValue =
                                                new HashMap<String, Value>();
        commonPreferredValue.put(DrbdXML.PROTOCOL_PARAM, DrbdXML.PROTOCOL_C);
        commonPreferredValue.put(DEGR_WFC_TIMEOUT_PARAM, new StringValue("0"));
        commonPreferredValue.put(CRAM_HMAC_ALG, new StringValue("sha1"));
        commonPreferredValue.put(SHARED_SECRET, new StringValue(getRandomSecret()));
        commonPreferredValue.put(ON_IO_ERROR, new StringValue("detach"));
        commonPreferredValue.put(PROXY_MEMLIMIT,
                                 new StringValue("100",
                                                 DrbdXML.getUnitMiBytes("")));
        commonPreferredValue.put(PROXY_PLUGIN_ZLIB, new StringValue("level 9"));
        final boolean protocolInNetSection = globalInfo.atLeastVersion("8.4");
        if (globalInfo.getDrbdResources().size() <= 1) {
            for (final String commonP : COMMON_PARAMS) {
                if (!protocolInNetSection
                    && DrbdXML.PROTOCOL_PARAM.equals(commonP)) {
                    continue;
                }
                final Widget wi = globalInfo.getWidget(commonP, null);
                if (wi == null) {
                    LOG.appError("widget for param: " + commonP + " was not created");
                    return new JPanel();
                }
                /* for the first resource set common options. */
                final Value commonValue =
                                      globalInfo.getResource().getValue(commonP);
                if (commonPreferredValue.containsKey(commonP)) {
                    final Value defaultValue =
                               globalInfo.getParamDefault(commonP);
                    if (Tools.areEqual(defaultValue, commonValue)) {
                        wi.setValue(commonPreferredValue.get(commonP));
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
                final Value commonValue =
                                      globalInfo.getResource().getValue(commonP);
                if (commonValue == null || commonValue.isNothingSelected()
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
                             ClusterBrowser.SERVICE_FIELD_WIDTH << 1,
                             true,
                             buttonClass(nextButton()));
        dri.addWizardParams(
                  optionsPanel,
                  PARAMS,
                  buttonClass(nextButton()),
                  Tools.getDefaultSize(
                                "Dialog.DrbdConfig.Resource.LabelWidth"),
                  Tools.getDefaultSize(
                        "Dialog.DrbdConfig.Resource.FieldWidth") << 1,
                  null);

        inputPane.add(optionsPanel);
        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(getProxyHostsPanel());
        inputPane.add(buttonPanel);
        final JScrollPane sp = new JScrollPane(inputPane);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        return sp;
    }

    /**
     * Return "Add Proxy Host" button.
     */
    private JPanel getProxyHostsPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(Tools.getBorder(
                    Tools.getString("Dialog.DrbdConfig.Resource.ProxyHosts")));

        final MyButton btn = new MyButton(
                        Tools.getString("Dialog.DrbdConfig.Resource.AddHost"));
        btn.setBackgroundColor(AppDefaults.LIGHT_ORANGE);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                btn.setEnabled(false);
                                proxyHostNextDialog = true;
                                buttonClass(nextButton()).pressButton();
                            }
                        });
                    }
                });
                t.start();
            }
        });
        panel.add(btn);

        final ResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        for (final Host h : dri.getCluster().getProxyHosts()) {
            panel.add(new JLabel(h.getName()));
        }
        return panel;
    }
}
