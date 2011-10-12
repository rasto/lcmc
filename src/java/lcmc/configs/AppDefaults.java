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

package lcmc.configs;

import java.awt.Color;
import java.util.Arrays;

/**
 * Here are default values for application.
 */
public final class AppDefaults extends java.util.ListResourceBundle {
    /** Darker background color. */
    public static final Color BACKGROUND_DARKER = new Color(63, 155, 241);

    /** Dark background color. */
    public static final Color BACKGROUND_DARK = new Color(120, 120, 120);

    /** Background color. */
    public static final Color BACKGROUND = new Color(168, 168, 168);

    /** Light background color. */
    public static final Color BACKGROUND_LIGHT = new Color(227, 227, 227);


    /** Get contents. */
    @Override protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        /*
         * defaults for development
         */
        {"DownloadLogin.User",      ""},
        {"DownloadLogin.Password",  ""},
        {"SSH.Host",                ""},
        {"SSH.SecHost",             ""},
        {"SSH.PublicKey",           ""},

        /*
         * error and warning messages handling.
         */
        /* show application errors in a dialog and stderr */
        {"AppError",         "y"},
        {"AppWarning",       "y"}, /* shows application warnings in stderr */
        {"DebugLevel",       0}, /* level -1 - no messages, 2 all messages */

        /*
         * dimensions of gui
         */
        {"DrbdMC.width",                         1120},
        {"DrbdMC.height",                        768},

        {"MainPanel.TerminalPanelHeight",        150},

        {"ConfigDialog.width",                   880},
        {"ConfigDialog.height",                  400},

        {"ConfirmDialog.width",                  400},
        {"ConfirmDialog.height",                 200},

        {"ExecCommandDialog.width",              600},
        {"ExecCommandDialog.height",             300},

        {"DialogButton.Width",                   40}, //!!!!! doesn't work
        {"DialogButton.Height",                  20},

        //{"ClusterBrowser.FieldWidth",            180}, // ??
        {"ClusterBrowser.ServiceLabelWidth",     150},
        {"ClusterBrowser.ServiceFieldWidth",     150},

        {"ClusterBrowser.DrbdResLabelWidth",     150},
        {"ClusterBrowser.DrbdResFieldWidth",     150},
        //{"GuiComboBox.width",                    200},
        //{"GuiComboBox.height",                   30},
        {"HostBrowser.DrbdDevLabelWidth",        150},
        {"HostBrowser.DrbdDevFieldWidth",        150},

        {"HostBrowser.ResourceInfoArea.Width",   300},
        {"HostBrowser.ResourceInfoArea.Height",  150},
        {"Browser.InfoPanelMinimalWidth",        440},
        {"Browser.LabelFieldHeight",             25},
        {"Browser.FieldHeight",                  30},

        {"Dialog.DrbdConfig.Resource.LabelWidth", 150},
        {"Dialog.DrbdConfig.Resource.FieldWidth", 150},

        {"Dialog.DrbdConfig.BlockDev.LabelWidth", 150},
        {"Dialog.DrbdConfig.BlockDev.FieldWidth", 150},

        {"Dialog.vm.Resource.LabelWidth", 150},
        {"Dialog.vm.Resource.FieldWidth", 350},

        {"MainMenu.DrbdGuiFiles.Extension",     "lcmc"},
        {"MainMenu.DrbdGuiFiles.Default",       System.getProperty("user.home")
                                                + "/"
                                                + "lcmc-conf.lcmc"},
        {"MainMenu.DrbdGuiFiles.Old",       System.getProperty("user.home")
                                                + "/"
                                                + "drbd-gui.drbdg"},

        /*
         * Colors
         */
        {"DrbdMC.TableHeader",               BACKGROUND},
        {"DefaultButton.Background",         BACKGROUND },
        {"ViewPanel.Background",             BACKGROUND_LIGHT },
        {"ViewPanel.ButtonPanel.Background", BACKGROUND },
        {"ViewPanel.Status.Background",      BACKGROUND_DARKER },
        {"ViewPanel.Foreground",             Color.BLACK },
        {"ViewPanel.Status.Foreground",      Color.WHITE },
        {"EmptyViewPanel.Help.Background",   Color.WHITE },
        {"ClustersPanel.Background",         Color.WHITE },
        {"HostsTab.Background",              Color.GRAY },
        {"TerminalPanel.Background",         Color.BLACK },
        {"TerminalPanel.Command", new Color(255, 187, 86) }, /* orange */
        {"TerminalPanel.Output",             Color.WHITE },
        {"TerminalPanel.Error",              Color.RED },
        {"TerminalPanel.Prompt",             Color.GREEN },

        {"TerminalPanel.TerminalBlack",      Color.BLACK},
        {"TerminalPanel.TerminalWhite",      Color.WHITE},
        {"TerminalPanel.TerminalDarkGray",   Color.GRAY},
        {"TerminalPanel.TerminalRed",        Color.RED},
        {"TerminalPanel.TerminalGreen",      Color.GREEN},
        {"TerminalPanel.TerminalYellow",     Color.YELLOW},
        {"TerminalPanel.TerminalBlue",       Color.BLUE},
        {"TerminalPanel.TerminalPurple",     new Color(128, 0, 128)},
        {"TerminalPanel.TerminalCyan",       Color.CYAN},

        {"ConfigDialog.Background",          Color.WHITE },
        {"ConfigDialog.Background.Dark",     BACKGROUND },
        {"ConfigDialog.Background.Light",    BACKGROUND_LIGHT },
        {"ConfigDialog.AnswerPane",          Color.BLACK },
        {"ConfigDialog.AnswerPane.Error",    Color.RED },
        {"ProgressBar.Background",           Color.WHITE },
        {"ProgressBar.Foreground",           BACKGROUND_DARK },

        {"ProgressBar.DefaultWidth",         300},
        {"ProgressBar.DefaultHeight",        30},

        {"ClusterTab.Background",            BACKGROUND },

        {"ResourceGraph.Background",         Color.WHITE },
        {"ResourceGraph.DrawPaint",          Color.BLACK },
        {"ResourceGraph.FillPaint",          Color.GREEN },
        {"ResourceGraph.FillPaintPicked",    Color.WHITE },
        {"ResourceGraph.PickedPaint",        Color.RED },

        {"ResourceGraph.EdgeDrawPaint", new Color(1, 1, 1) }, /* almost black */
        {"ResourceGraph.EdgeDrawPaintNew",        new Color(200, 200, 200) },
        {"ResourceGraph.EdgeDrawPaintBrighter",   new Color(128, 128, 128) },
        {"ResourceGraph.EdgePickedPaint",         new Color(160, 160, 255) },
        {"ResourceGraph.EdgePickedPaintNew",      new Color(200, 200, 200) },
        {"ResourceGraph.EdgePickedPaintBrighter", new Color(160, 160, 255) },
        {"ResourceGraph.EdgeDrawPaintRemoved",    new Color(128, 64, 64) },
        {"ResourceGraph.EdgePickedPaintRemoved",  new Color(255, 64, 64) },

        {"HeartbeatGraph.FillPaintRemoved",       Color.LIGHT_GRAY },
        {"HeartbeatGraph.FillPaintFailed",        Color.RED },
        {"HeartbeatGraph.FillPaintUnconfigured",  new Color(238, 238, 238)},
        {"HeartbeatGraph.FillPaintUnknown",       Color.LIGHT_GRAY},
        {"HeartbeatGraph.FillPaintStopped",       new Color(238, 238, 238)},
        {"HeartbeatGraph.FillPaintPlaceHolder",   new Color(238, 238, 150)},

        {"DrbdGraph.FillPaintNotAvailable",       new Color(238, 238, 238) },
        {"DrbdGraph.FillPaintPrimary",            Color.GREEN },
        {"DrbdGraph.FillPaintSecondary",          Color.YELLOW },
        {"DrbdGraph.FillPaintUnknown",            Color.LIGHT_GRAY },

        {"DrbdGraph.EdgeDrawPaintDisconnected",   new Color(255, 50, 50) },
        {"DrbdGraph.EdgeDrawPaintDisconnectedBrighter",
                                                    new Color(200, 40, 40) },

        {"DrbdGraph.DrawPaintNotAvailable",       new Color(153, 153, 153) },
        {"Host.DefaultColor",                     Color.GREEN },
        {"Host.ErrorColor",                       Color.RED },
        {"Host.NoStatusColor",                    Color.LIGHT_GRAY },

        {"GuiComboBox.DefaultValue",              new Color(50, 50, 50) },
        {"GuiComboBox.SavedValue",                new Color(0, 120, 0) },
        {"GuiComboBox.ChangedValue",              new Color(128, 0, 128) },
        {"GuiComboBox.ErrorValue",                new Color(255, 100, 100) },

        {"ClusterBrowser.Background",             new Color(255, 255, 255) },
        {"ClusterBrowser.Test.Tooltip.Background", new Color(255, 255, 0, 160)},
        {"Browser.Background",                    new Color(255, 255, 255) },

        {"EmptyBrowser.StartPanelTitleBorder",    BACKGROUND_DARK },

        /*
         * Images
         */
        {"ConfigDialog.Icon",                     "teaser_drdb_boxes_01.png"},

        {"Dialog.Cluster.ClusterHosts.HostCheckedIcon",
         "Icons/32x32/Checked.gif"},
        {"Dialog.Cluster.ClusterHosts.HostUncheckedIcon",
         "Icons/32x32/Unchecked.gif"},
        {"Dialog.Cluster.HbConfig.DopdCheckedIcon",
         "Icons/32x32/Checked.gif"},
        {"Dialog.Cluster.HbConfig.DopdUncheckedIcon",
         "Icons/32x32/Unchecked.gif"},
        {"Dialog.Cluster.CoroConfig.DefaultMCastAddress", "226.94.1.1"},
        {"Dialog.Cluster.CoroConfig.DefaultMCastPort",    "5405"},
        {"Browser.CategoryIcon", "Icons/16x16/folder_16x16.png"},
        {"Browser.ApplyIcon",    "Icons/16x16/ok_16x16.png"},
        {"Browser.ApplyIconLarge", "Icons/32x32/ok_32x32.png"},
        {"Browser.RevertIcon",    "Icons/16x16/revert_16x16.png"},
        {"Browser.MenuIcon",  "Icons/16x16/menu_16x16.png"},

        {"HostBrowser.NetIntIcon", "Icons/16x16/netzwk_16x16.png"},

        {"HostBrowser.NetIntIconLarge",
         "Icons/32x32/netzwk_32x32.png"},

        {"HostBrowser.FileSystemIcon", "Icons/16x16/filesys_16x16.png"},
        {"HostBrowser.RemoveIcon", "Icons/32x32/cancel_32x32.png"},

        {"ClusterBrowser.HostIcon", "Icons/16x16/host_16x16.png"},
        {"HeartbeatGraph.ServiceRunningIcon",
                                "Icons/32x32/service_running_32x32.png"},
        {"HeartbeatGraph.ServiceRunningFailedIcon",
                               "Icons/32x32/service_running_failed_32x32.png"},
        {"HeartbeatGraph.ServiceStartedIcon",
                                "Icons/32x32/service_started_32x32.png"},
        {"HeartbeatGraph.ServiceStoppingIcon",
                                "Icons/32x32/service_stopping_32x32.png"},
        {"HeartbeatGraph.ServiceStoppedIcon",
                               "Icons/32x32/service_stopped_32x32.png"},
        {"HeartbeatGraph.ServiceStoppedFailedIcon",
                               "Icons/32x32/service_stopped_failed_32x32.png"},
        {"HeartbeatGraph.ServiceMigratedIcon",
                                "Icons/32x32/service_migrated_32x32.png"},
        {"HeartbeatGraph.HostStandbyIcon",
                                "Icons/32x32/host_standby_32x32.png"},
        {"HeartbeatGraph.HostStandbyOffIcon",
                                "Icons/32x32/host_standbyoff_32x32.png"},
        {"HeartbeatGraph.ServiceUnmanagedIcon",
                               "Icons/32x32/service_unmanaged_32x32.png"},

        {"HeartbeatGraph.HostStopCommLayerIcon",
                                       "Icons/32x32/shutdown_32x32.png"},
        {"HeartbeatGraph.HostStartCommLayerIcon",
         "Icons/32x32/resume_32x32.png"},
        {"ServiceInfo.ServiceRunningIconSmall",
                                "Icons/16x16/service_running_16x16.png"},
        {"ServiceInfo.ServiceStoppedIconSmall",
                                      "Icons/16x16/service_stopped_16x16.png"},

        {"ServiceInfo.ServiceRunningFailedIconSmall",
                               "Icons/16x16/service_running_failed_16x16.png"},
        {"ServiceInfo.ServiceStartedIconSmall",
                                "Icons/16x16/service_started_16x16.png"},
        {"ServiceInfo.ServiceStoppingIconSmall",
                                "Icons/16x16/service_stopping_16x16.png"},
        {"ServiceInfo.ServiceStoppedFailedIconSmall",
                               "Icons/16x16/service_stopped_failed_16x16.png"},
        {"ServiceInfo.ServiceMigratedIconSmall",
                                "Icons/16x16/service_migrated_16x16.png"},
        {"ClusterBrowser.HostStandbyIconSmall",
                                "Icons/16x16/host_standby_16x16.png"},
        {"ClusterBrowser.HostStandbyOffIconSmall",
                                "Icons/16x16/host_standbyoff_16x16.png"},
        {"ClusterBrowser.ServiceUnmanagedIconSmall",
                               "Icons/16x16/service_unmanaged_16x16.png"},

        {"ClusterBrowser.NetworkIcon", "Icons/16x16/netzwk_16x16.png"},
        {"ClusterBrowser.RemoveIcon",  "Icons/32x32/cancel_32x32.png"},
        {"ClusterBrowser.RemoveIconSmall",
         "Icons/16x16/cancel_16x16.png"},

        {"ClusterBrowser.ClusterIconSmall",
         "Icons/16x16/cluster_16x16.png"},

        {"ServiceInfo.ManageByCRMIcon", "Icons/32x32/pacemaker_32x32.png"},
        {"ServiceInfo.UnmanageByCRMIcon", "Icons/32x32/nopacemaker_32x32.png"},

        {"ClusterBrowser.PacemakerIconSmall",
         "Icons/16x16/pacemaker_16x16.png"},
        {"ClusterBrowser.PacemakerIcon",
         "Icons/32x32/pacemaker_32x32.png"},

        {"ClusterBrowser.DRBDIconSmall",
         "Icons/16x16/drbd_16x16.png"},

        {"HostViewPanel.HostIcon",     "Icons/32x32/host_32x32.png"},
        {"HostTab.HostIcon",           "Icons/32x32/host_32x32.png"},
        {"HostBrowser.HostIconSmall",    "Icons/16x16/host_16x16.png"},
        {"HostBrowser.HostOnIconSmall",  "Icons/16x16/host_on_16x16.png"},
        {"HostBrowser.HostOffIconSmall",
         "Icons/16x16/host_off_16x16.png"},

        {"HostBrowser.HostIcon",       "Icons/32x32/host_32x32.png"},
        {"HostBrowser.HostOnIcon",     "Icons/32x32/host_on_32x32.png"},
        {"HostBrowser.HostOffIcon",    "Icons/32x32/host_off_32x32.png"},

        {"HostBrowser.HostInClusterIconRightSmall",
         "Icons/16x16/host_in_cluster_right_16x16.png"},

        {"HostBrowser.HostInClusterIconLeftSmall",
         "Icons/16x16/host_in_cluster_left_16x16.png"},

        {"EmptyBrowser.HostIcon",      "Icons/32x32/host_32x32.png"},

        {"MainMenu.HostIcon",          "Icons/16x16/host_16x16.png"},

        {"MainPanel.HostsIcon",        "Icons/16x16/host_16x16.png"},

        {"ClustersPanel.ClustersIcon",  "Icons/32x32/clusters_32x32.png"},
        {"ClustersPanel.ClusterIcon",    "Icons/32x32/cluster_32x32.png"},
        {"ClusterViewPanel.ClusterIcon", "Icons/32x32/cluster_32x32.png"},

        {"HostsPanel.HostIcon",          "Icons/32x32/host_32x32.png"},

        {"Info.LogIcon",     "Icons/32x32/logfile.png"},

        {"BlockDevInfo.HarddiskIconLarge",
         "Icons/32x32/blockdevice_32x32.png"},

        {"BlockDevInfo.HarddiskDRBDIconLarge",
         "Icons/32x32/blockdevice_drbd_32x32.png"},

        {"BlockDevInfo.NoHarddiskIconLarge",
         "Icons/32x32/noharddisk.png"},

        {"BlockDevInfo.HarddiskIcon",
         "Icons/16x16/blockdevice_16x16.png"},

        {"HeartbeatGraph.StartIcon",     "Icons/32x32/ok_32x32.png"},
        //{"HeartbeatGraph.StopIcon",     "Icons/32x32/cancel_32x32.png"},
        {"HeartbeatGraph.MigrateIcon",
                                "Icons/32x32/service_migrated_32x32.png"},
        {"HeartbeatGraph.UnmigrateIcon",
                              "Icons/32x32/service_unmigrate_32x32.png"},
        {"HeartbeatGraph.GroupUp", "Icons/32x32/group_up_32x32.png"},
        {"HeartbeatGraph.GroupDown", "Icons/32x32/group_down_32x32.png"},

        {"ClusterViewPanel.HostIcon",    "Icons/32x32/host_32x32.png"},
        {"ClusterTab.ClusterIcon",      "Icons/32x32/cluster_32x32.png"},

        {"ProgressIndicatorPanel.CancelIcon",
                                        "Icons/32x32/cancel_32x32.png"},
        {"ProgressBar.CancelIcon", "Icons/16x16/cancel_16x16.png"},

        {"Dialog.Dialog.BackIcon",     "Icons/16x16/back2_16x16.png"},
        {"Dialog.Dialog.NextIcon",     "Icons/16x16/next2_16x16.png"},
        {"Dialog.Dialog.CancelIcon",   "Icons/16x16/cancel_16x16.png"},
        {"Dialog.Dialog.FinishIcon",   "Icons/16x16/finish2_16x16.png"},
        {"BackIcon",                   "Icons/16x16/back2_16x16.png"},
        {"BackIconLarge",              "Icons/32x32/back2_32x32.png"},
        {"VMS.VNC.IconSmall",          "Icons/16x16/vnc_16x16.png"},
        {"VMS.VNC.IconLarge",          "Icons/32x32/vnc_32x32.png"},
        {"VMS.Pause.IconLarge",        "Icons/32x32/pause_32x32.png"},
        {"VMS.Resume.IconLarge",       "Icons/32x32/resume_32x32.png"},
        {"VMS.Reboot.IconLarge",       "Icons/32x32/reboot_32x32.png"},
        {"VMS.Shutdown.IconLarge",     "Icons/32x32/shutdown_32x32.png"},
        {"VMS.Destroy.IconLarge",      "Icons/32x32/destroy_32x32.png"},

        {"Dialog.Host.CheckInstallation.CheckingIcon",
         "Icons/32x32/dialog-information.png"},

        {"Dialog.Host.CheckInstallation.NotInstalledIcon",
         "Icons/32x32/software-update-urgent.png"},

        {"Dialog.Host.CheckInstallation.InstalledIcon",
         "Icons/32x32/weather-clear.png"},

        {"Dialog.Host.CheckInstallation.UpgrAvailIcon",
         "Icons/32x32/software-update-available.png"},

        {"Dialog.Host.Finish.HostIcon",
         "Icons/32x32/host_32x32.png"},

        {"Dialog.Host.Finish.ClusterIcon",
         "Icons/32x32/cluster_32x32.png"},

        /* ssh */
        {"SSH.User",                 "root"},
        {"SSH.Port",                 "22"},
        {"SSH.ConnectTimeout",       30000}, /* milliseconds, 0 no timeout */
        {"SSH.KexTimeout",           0}, /* milliseconds, 0 no timeout */
        {"SSH.Command.Timeout.Long", 0},     /* milliseconds, 0 no timeout */
        {"SSH.Command.Timeout",      180000}, /* milliseconds */
        {"ProgressBar.Sleep",        100},   /* milliseconds */
        {"ProgressBar.Delay",        50},    /* milliseconds */

        /* score */
        {"Score.Infinity",                    100000},
        {"Score.MinusInfinity",               -100000},

        /* drbd */
       {"HostBrowser.DrbdNetInterfacePort",  7788},

        /*
         * other
         */
        {"MaxHops",                           20},

    };
}
