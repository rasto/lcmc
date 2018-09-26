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


package lcmc.drbd.ui.configdialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.configs.AppDefaults;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.drbd.domain.DrbdXml;
import lcmc.common.ui.WizardDialog;
import lcmc.drbd.ui.resource.GlobalInfo;
import lcmc.drbd.ui.resource.ResourceInfo;
import lcmc.cluster.ui.widget.Widget;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;

/**
 * An implementation of a dialog where user can enter drbd resource
 * information.
 */
public final class Resource extends DrbdConfig {
    private final HostFactory hostFactory;
    private final Supplier<NewProxyHostDialog> newProxyHostDialogProvider;
    private final Volume volumeDialog;
    private final Application application;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;

    private static final Logger LOG = LoggerFactory.getLogger(Resource.class);
    private static final String WFC_TIMEOUT_PARAM = "wfc-timeout";
    private static final String DEGR_WFC_TIMEOUT_PARAM = "degr-wfc-timeout";

    private static final String ALLOW_TWO_PRIMARIES_PARAM = "allow-two-primaries";
    private static final String CRAM_HMAC_ALG_PARAM = "cram-hmac-alg";
    private static final String SHARED_SECRET_PARAM = "shared-secret";
    private static final String ON_IO_ERROR_PARAM = "on-io-error";
    private static final String PROXY_MEMLIMIT_PARAM = "memlimit";
    private static final String PROXY_PLUGIN_ZLIB_PARAM = "plugin-zlib";
    private static final String PROXY_PLUGIN_LZMA_PARAM = "plugin-lzma";

    private static final String[] COMMON_PARAMS = {DrbdXml.PING_TIMEOUT_PARAM,
                                                   CRAM_HMAC_ALG_PARAM,
                                                   SHARED_SECRET_PARAM,
                                                   WFC_TIMEOUT_PARAM,
                                                   DEGR_WFC_TIMEOUT_PARAM,
                                                   ON_IO_ERROR_PARAM,
                                                   PROXY_MEMLIMIT_PARAM,
                                                   PROXY_PLUGIN_ZLIB_PARAM,
                                                   PROXY_PLUGIN_LZMA_PARAM};
    private static final String[] PARAMS = {"name",
                                            DrbdXml.PROTOCOL_PARAM,
                                            DrbdXml.PING_TIMEOUT_PARAM,
                                            ALLOW_TWO_PRIMARIES_PARAM,
                                            CRAM_HMAC_ALG_PARAM,
                                            SHARED_SECRET_PARAM,
                                            WFC_TIMEOUT_PARAM,
                                            DEGR_WFC_TIMEOUT_PARAM,
                                            ON_IO_ERROR_PARAM,
                                            PROXY_MEMLIMIT_PARAM,
                                            PROXY_PLUGIN_ZLIB_PARAM,
                                            PROXY_PLUGIN_LZMA_PARAM};
    private static final int SECRET_STRING_LENGTH = 32;
    private boolean proxyHostNextDialog = false;

    public Resource(HostFactory hostFactory, Supplier<NewProxyHostDialog> newProxyHostDialogProvider, Volume volumeDialog, Supplier<ProgressBar> progressBarProvider, Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData) {
        super(progressBarProvider, application, swingUtils, widgetFactory, mainData);
        this.hostFactory = hostFactory;
        this.newProxyHostDialogProvider = newProxyHostDialogProvider;
        this.volumeDialog = volumeDialog;
        this.application = application;
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
    }

    private String getRandomSecret() {
        return Tools.getRandomSecret(SECRET_STRING_LENGTH);
    }

    /** Applies the changes and returns next dialog (BlockDev). */
    @Override
    public WizardDialog nextDialog() {
        final ResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        if (proxyHostNextDialog) {
            proxyHostNextDialog = false;
            final Host proxyHost = hostFactory.createInstance();
            proxyHost.setCluster(dri.getCluster());
            final NewProxyHostDialog newProxyHostDialog = newProxyHostDialogProvider.get();
            newProxyHostDialog.init(this, proxyHost, getDrbdVolumeInfo(), this, new DrbdInstallation());
            return newProxyHostDialog;
        }
        final GlobalInfo globalInfo = dri.getBrowser().getGlobalInfo();
        final boolean protocolInNetSection = globalInfo.atLeastVersion("8.4");
        if (globalInfo.getDrbdResources().size() <= 1) {
            for (final String commonP : COMMON_PARAMS) {
                if (!protocolInNetSection && DrbdXml.PROTOCOL_PARAM.equals(commonP)) {
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
        volumeDialog.init(this, getDrbdVolumeInfo());
        return volumeDialog;
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.Resource.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.Resource.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        final ResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        dri.waitForInfoPanel();
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

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
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                makeDefaultButton(buttonClass(nextButton()));
            }
        });
        if (application.getAutoOptionGlobal("autodrbd") != null) {
            pressNextButton();
        }
    }

    @Override
    protected JComponent getInputPane() {
        final ResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.LINE_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        /* common options */
        final Map<String, Value> commonPreferredValue = new HashMap<String, Value>();
        commonPreferredValue.put(DrbdXml.PROTOCOL_PARAM, DrbdXml.PROTOCOL_C);
        commonPreferredValue.put(DEGR_WFC_TIMEOUT_PARAM, new StringValue("0"));
        commonPreferredValue.put(CRAM_HMAC_ALG_PARAM, new StringValue("sha1"));
        commonPreferredValue.put(SHARED_SECRET_PARAM, new StringValue(getRandomSecret()));
        commonPreferredValue.put(ON_IO_ERROR_PARAM, new StringValue("detach"));
        commonPreferredValue.put(PROXY_MEMLIMIT_PARAM, new StringValue("100", DrbdXml.getUnitMiBytes("")));
        commonPreferredValue.put(PROXY_PLUGIN_ZLIB_PARAM, new StringValue("level 9"));
        final GlobalInfo globalInfo = dri.getBrowser().getGlobalInfo();
        final boolean protocolInNetSection = globalInfo.atLeastVersion("8.4");
        if (globalInfo.getDrbdResources().size() <= 1) {
            for (final String commonP : COMMON_PARAMS) {
                if (!protocolInNetSection && DrbdXml.PROTOCOL_PARAM.equals(commonP)) {
                    continue;
                }
                final Widget widget = globalInfo.getWidget(commonP, null);
                if (widget == null) {
                    LOG.appError("widget for param: " + commonP + " was not created");
                    return new JPanel();
                }
                /* for the first resource set common options. */
                final Value commonValue = globalInfo.getResource().getValue(commonP);
                if (commonPreferredValue.containsKey(commonP)) {
                    final Value defaultValue = globalInfo.getParamDefault(commonP);
                    if (Tools.areEqual(defaultValue, commonValue)) {
                        widget.setValue(commonPreferredValue.get(commonP));
                        dri.getResource().setValue(commonP, commonPreferredValue.get(commonP));
                    } else {
                        dri.getResource().setValue(commonP, commonValue);
                    }
                }
            }
        } else {
            /* resource options, if not defined in common section. */
            for (final String commonP : COMMON_PARAMS) {
                final Value commonValue = globalInfo.getResource().getValue(commonP);
                if (commonValue == null || commonValue.isNothingSelected()
                    && commonPreferredValue.containsKey(commonP)) {
                    dri.getResource().setValue(commonP, commonPreferredValue.get(commonP));
                }
            }
        }

        /* address combo boxes */
        dri.addHostAddresses(optionsPanel,
                             application.getServiceLabelWidth(),
                             application.getServiceFieldWidth() << 1,
                             true,
                             buttonClass(nextButton()));
        dri.addWizardParams(optionsPanel,
                            PARAMS,
                            buttonClass(nextButton()),
                            application.getDefaultSize("Dialog.DrbdConfig.Resource.LabelWidth"),
                            application.getDefaultSize("Dialog.DrbdConfig.Resource.FieldWidth") << 1,
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

    private JPanel getProxyHostsPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(Tools.getBorder(Tools.getString("Dialog.DrbdConfig.Resource.ProxyHosts")));

        final MyButton btn = widgetFactory.createButton(Tools.getString("Dialog.DrbdConfig.Resource.AddHost"));
        btn.setBackgroundColor(AppDefaults.LIGHT_ORANGE);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        swingUtils.invokeLater(new Runnable() {
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
