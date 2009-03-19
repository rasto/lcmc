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
 * along with DRBD; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package drbd.configs;

import java.util.Arrays;

/**
 * Here are all English and common texts.
 */
public class TextResource extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"DrbdMC.Title",                     "DRBD Management Console (Beta)"},

        /* Main Menu */
        {"MainMenu.Session",                  "Session"},
        {"MainMenu.New",                      "New"},
        {"MainMenu.Load",                     "Load"},
        {"MainMenu.Save",                     "Save"},
        {"MainMenu.SaveAs",                   "Save As"},
        {"MainMenu.Host",                     "Host"},
        {"MainMenu.Cluster",                  "Cluster"},
        {"MainMenu.RemoveEverything",         "Remove Everything"},
        {"MainMenu.Exit",                     "Exit"},

        {"MainMenu.Settings",                 "Settings"},
        {"MainMenu.LookAndFeel",              "Look And Feel"},

        {"MainMenu.Help",                     "Help"},
        {"MainMenu.About",                    "About"},

        {"MainMenu.DrbdGuiFiles",             "DRBD Management Console files"},


        {"MainPanel.Clusters",                "Clusters"},
        {"MainPanel.ClustersAlt",             "Click here for clusters view"},
        {"MainPanel.Hosts",                   "Hosts"},
        {"MainPanel.HostsAlt",                "Click here for hosts view"},

        {"ClustersPanel.NewTabTip",            "New Cluster"},
        {"HostsPanel.NewTabTip",               "New Host"},

        {"Tools.ExecutingCommand",            "Executing command..."},
        {"Tools.CommandDone",                 "[done]"},
        {"Tools.CommandFailed",               "[failed]"},
        {"Tools.Loading",                     "Loading..."},
        {"Tools.Saving",                      "Saving..."},
        {"Tools.sshError.command",            "Command:"},
        {"Tools.sshError.returned",           "returned exit code"},

        {"HostViewPanel.ConnectButton",       "Connect"},
        {"HostViewPanel.DisconnectButton",    "Disconnect"},
        {"HostViewPanel.WizardButton",        "Wizard"},

        {"ClusterTab.AddNewCluster",          "Cluster Wizard"},
        {"ClusterTab.AddNewHost",             "Host Wizard"},
        {"ClusterViewPanel.StatusHbButtonsTitle", "Heartbeat Status"},
        {"ClusterViewPanel.StatusHbStopButton", "Stop"},
        {"ClusterViewPanel.StatusHbPlayButton", "Play"},
        {"ClusterViewPanel.ClusterButtons",       "Cluster"},
        {"ClusterViewPanel.ClusterWizard",        "Cluster Wizard"},

        {"ClusterViewPanel.StatusDrbdButtonsTitle", "DRBD Status"},
        {"ClusterViewPanel.StatusDrbdStopButton", "Stop"},
        {"ClusterViewPanel.StatusDrbdPlayButton", "Play"},

        {"ProgressBar.Cancel",                "Cancel"},


        {"Dialog.Dialog.Next",             "Next"},
        {"Dialog.Dialog.Back",             "Back"},
        {"Dialog.Dialog.Cancel",           "Cancel"},
        {"Dialog.Dialog.Finish",           "Finish"},
        {"Dialog.Dialog.Retry",            "Retry"},
        {"Dialog.Dialog.PrintErrorAndRetry", "Command failed."},

        {"Dialog.HostNewHost.Title",            "Host Wizard"},
        {"Dialog.HostNewHost.Description", "Enter the <b>hostname/IP</b> and <b>username</b> of the server. Host can be entered either as hostname or IP address. Enter a hostname only if it is resolvable by DNS. Username will be used for SSH connections and command executions. Normally it should be the <b>root</b> user. It is recommended not to change it.<br><br>"
                                           + "You may enter more hosts delimited with \",\", if the server is reachable not directly but via several <b>hops</b>. If this is the case you have to enter the same number of usernames and hostnames/IPs as they are hops."},

        {"Dialog.HostNewHost.EnterHost",        "Host:"},
        {"Dialog.HostNewHost.EnterUsername",    "Username:"},
        {"Dialog.HostNewHost.EnterPassword",    "Password:"},

        {"Dialog.HostConfiguration.Title",          "Host configuration"},
        {"Dialog.HostConfiguration.Description",    "Trying to do a DNS lookup of the host. If DNS lookup failed, go back and enter an IP of the host if the host is not resolvable by DNS or make the hostname resolvable."},

        {"Dialog.HostConfiguration.Name",           "nodename:"}, // TODO: this is not necessary anymore
        {"Dialog.HostConfiguration.Hostname",       "hostname:"},
        {"Dialog.HostConfiguration.Ip",             "IP:"},
        {"Dialog.HostConfiguration.DNSLookup",      "DNS lookup"},
        {"Dialog.HostConfiguration.DNSLookupOk",    "DNS lookup done."},
        {"Dialog.HostConfiguration.DNSLookupError", "DNS lookup failed."},

        {"Dialog.HostSSH.Title",           "Create SSH Connection"},
        {"Dialog.HostSSH.Description",     "Trying to create a connection to the host. You can either enter a RSA or DSA key or enter a password in the pop up dialog. You can also set up passwordless authentication to omit this step."},
        {"Dialog.HostSSH.Connecting",      "Connecting..."},
        {"Dialog.HostSSH.Connected",       "Connection established."},
        {"Dialog.HostSSH.NotConnected",    "Connection failed."},

        {"Dialog.HostDevices.Title",       "Host devices"},
        {"Dialog.HostDevices.Description", "Trying to retrieve information about block, network devices and installation information of the host."},
        {"Dialog.HostDevices.Executing",   "Executing..."},
        {"Dialog.HostDevices.CheckError",  "Failed."},

        {"Dialog.HostDist.Title",          "Available Packages"},
        {"Dialog.HostDist.Description",    "Trying to match distribution, kernel package and architecture of the server to the available binary DRBD packages. If none is selected, most likely there is no DRBD package available for your system. If you use stock kernel of your distribution, the package will be provided to you by LINBIT support (not free). After that you may retry this step again."},
        {"Dialog.HostDist.DownloadNotAvailable", "DRBD package not available."},
        {"Dialog.HostDist.NoDist",         "DRBD package not found."},
        {"Dialog.HostDist.NotALinux",      "Cannot determine an operating system."},
        {"Dialog.HostDist.NoArch",         "Cannot determine the architecture"},
        {"Dialog.HostDist.Executing",      "Executing..."},
        {"Dialog.HostDist.AvailVersions",  "Available versions: "},
        {"Dialog.HostDist.NoKernels",      "Not available for this kernel"},
        {"Dialog.HostDist.NoVersions",     "Could not find any DRBD versions"},

        {"Dialog.HostDistDetection.Title", "Distribution Detection"},
        {"Dialog.HostDistDetection.Description", "Trying to detect the Linux distribution of the host. It is Linux, right? If none is detected, it means that the distribution is not supported. You may then choose a distribution that is similar, which may or may not work for you."},
        {"Dialog.HostDistDetection.Executing", "Executing..."},

        {"Dialog.HostCheckInstallation.Title",      "Installation Check"},
        {"Dialog.HostCheckInstallation.Description", "Checking if DRBD, Heartbeat and other important components are already installed. If not, you can press one of the 'Install' buttons to install them. You can check for DRBD upgrade as well if installed DRBD was detected. Installing or upgrading of DRBD via binary packages is possible only if you have support contract with LINBIT."},

        {"Dialog.HostCheckInstallation.Drbd.AlreadyInstalled", "is already installed."},
        {"Dialog.HostCheckInstallation.Drbd.NotInstalled",     "DRBD is not installed. Click 'Install' button to install a new shiny DRBD."},

        {"Dialog.HostCheckInstallation.Heartbeat.AlreadyInstalled", "is already installed."},
        {"Dialog.HostCheckInstallation.Heartbeat.NotInstalled",     "Heartbeat is not installed or is installed improperly. Press 'Next' button in order to install the heartbeat packages."},
        {"Dialog.HostCheckInstallation.Heartbeat.CheckError", "Check failed."},

        {"Dialog.HostCheckInstallation.Checking",   "Checking..."},
        {"Dialog.HostCheckInstallation.CheckError", "Check failed."},
        {"Dialog.HostCheckInstallation.AllOk",      "All required components are installed."},
        {"Dialog.HostCheckInstallation.SomeFailed", "Some of the required components are not installed."},
        {"Dialog.HostCheckInstallation.DrbdNotInstalled",  "not installed"},
        {"Dialog.HostCheckInstallation.UdevNotInstalled",  "not installed"},
        {"Dialog.HostCheckInstallation.HbNotInstalled",    "not installed"},
        {"Dialog.HostCheckInstallation.HbGUINotInstalled",    "not installed"},
        {"Dialog.HostCheckInstallation.DrbdUpgradeButton", "Upgrade"},
        {"Dialog.HostCheckInstallation.DrbdCheckForUpgradeButton", "Check for Upgrade"},

        {"Dialog.HostCheckInstallation.DrbdInstallButton", "Install"},
        {"Dialog.HostCheckInstallation.UdevInstallButton", "Install"},
        {"Dialog.HostCheckInstallation.HbInstallButton",   "Install"},
        {"Dialog.HostCheckInstallation.HbGUIInstallButton",   "Install"},

        {"Dialog.HostCheckInstallation.CheckingHb",        "checking..."},
        {"Dialog.HostCheckInstallation.CheckingHbGUI",        "checking..."},
        {"Dialog.HostCheckInstallation.CheckingDrbd",      "checking..."},
        {"Dialog.HostCheckInstallation.CheckingUdev",      "checking..."},

        {"Dialog.HostLogin.Title",          "Log in"},
        {"Dialog.HostLogin.Description",    "You need to log in to the http://www.linbit.com/support download area to get a package for your distribution. Please provide your username and password. Contact LINBIT support to obtain one. Alternatively you can install/upgrade the DRBD yourself and go back to the previous dialog in order to continue.<br><br>"
                                            + "If you enter a wrong username or password you will find out about it, only in the next step, when you will be unable to download anything."},

        {"Dialog.HostLogin.EnterUser",      "Username"},
        {"Dialog.HostLogin.EnterPassword",  "Password"},
        {"Dialog.HostLogin.Save",           "Save"},

        {"Dialog.HostDrbdAvailFiles.Title",      "Available DRBD Packages"},
        {"Dialog.HostDrbdAvailFiles.Description", "Trying to detect available packages. There should be one module and one util package if you use stock distribution kernel. If no package was auto-detected, you may choose appropriate kernel version in the pull down menu. It is also possible that packages were not build for your system at all. In that case contact LINBIT support and packages will be provided to you as soon as possible."},
        {"Dialog.HostDrbdAvailFiles.Executing",  "Executing..."},
        {"Dialog.HostDrbdAvailFiles.NoFiles",    "No packages found."},

        {"Dialog.HostDrbdInst.Title",       "DRBD Installation"},
        {"Dialog.HostDrbdInst.Description", "DRBD is being installed. If it fails with authorization errors, you entered a wrong username or password, going back, entering it correctly, would fix it. Other possibility is that wrong distribution was selected and therefore the installation did not work. Yet another remote possibility is that LINBIT servers are down, but this is not very likely, since LINBIT servers are fault-tolerant using DRBD."},
        {"Dialog.HostDrbdInst.CheckingFile", "Checking installed file..."},
        {"Dialog.HostDrbdInst.FileExists",  "File already exists."},
        {"Dialog.HostDrbdInst.Downloading", "Downloading..."},
        {"Dialog.HostDrbdInst.Installing",  "DRBD is being installed..."},
        {"Dialog.HostDrbdInst.InstallationDone", "Installation done."},
        {"Dialog.HostDrbdInst.InstallationFailed", "Installation failed."},
        {"Dialog.HostDrbdInst.Executing",   "Executing..."},
        {"Dialog.HostDrbdInst.Starting",    "Starting DRBD..."},
        {"Dialog.HostDrbdInst.WgetError",   "Could not get DRBD packages."},

        {"Dialog.HostHbInst.Title",        "Heartbeat Install"},
        {"Dialog.HostHbInst.Description",  "Heartbeat packages are being installed."},
        {"Dialog.HostHbInst.Executing",    "Installing..."},
        {"Dialog.HostHbInst.InstOk",       "Heartbeat was successfully installed."},
        {"Dialog.HostHbInst.InstError",    "Installation error, fix the error and retry."},

        {"Dialog.HostHbGUIInst.Title",        "Heartbeat GUI Install"},
        {"Dialog.HostHbGUIInst.Description",  "Heartbeat GUI package is being installed."},
        {"Dialog.HostHbGUIInst.Executing",    "Installing..."},
        {"Dialog.HostHbGUIInst.InstOk",       "Heartbeat GUI was successfully installed."},
        {"Dialog.HostHbGUIInst.InstError",    "Installation error, fix the error and retry."},

        {"Dialog.HostFinish.Title",        "Finish"},
        {"Dialog.HostFinish.Description",  "Configuration of the host is now complete. You can now add another host or configure a cluster."},
        {"Dialog.HostFinish.AddAnotherHostButton", "Add Another Host"},
        {"Dialog.HostFinish.ConfigureClusterButton", "Configure Cluster"},

        {"ExecCommandDialog.Title",           "Executing command"},
        {"ExecCommandDialog.Description",     "Executing command."},
        {"ExecCommandDialog.Executing",       "Executing..."},
        {"ExecCommandDialog.Done",            "Done."},
        {"ExecCommandDialog.ExecError",       "Execution error."},


        {"Dialog.Dialog.Ok",                   "OK"},
        {"Dialog.ConfigDialog.Ok",                   "OK"},
        {"Dialog.ConfigDialog.NoMatch",              "No Match"},
        {"Dialog.ConfigDialog.SkipButton",           "Skip"},

        {"Dialog.ConnectDialog.Title",               "SSH Connection"},
        {"Dialog.ConnectDialog.Description",         "Trying to establish an SSH connection."},

        {"Dialog.ClusterName.Title",       "Cluster Wizard"},
        {"Dialog.ClusterName.EnterName",   "Name:"},
        {"Dialog.ClusterName.Description", "Enter a name for the cluster. This name can be anything as long as it is unique. It is used only as an identification in the GUI and can be changed later."},

        {"Dialog.ClusterHosts.Title",      "Select Hosts"},
        {"Dialog.ClusterHosts.Description", "Select two or more hosts that are part of the DRBD/Heartbeat cluster."},

        {"Dialog.ClusterConnect.Title",      "Cluster Connect"},
        {"Dialog.ClusterConnect.Description", "Trying to connect to all the hosts in the cluster."},

        {"Dialog.ClusterHbInit.Title",        "Heartbeat Initialization"},
        {"Dialog.ClusterHbInit.Description",  "In this step Heartbeat config (/etc/ha.d/ha.cf) is created and Heartbeat is started. You do not have to overwrite your old config if you have some special options. You can modify it by hand on every host in the cluster. You have to press the \"Create HB Config\" button to save the new configuration on all hosts."},
        {"Dialog.ClusterHbInit.CreateHbConfig", "Create HB Config"},
        {"Dialog.ClusterHbInit.CreateHbConfig", "Create HB Config"},
        {"Dialog.ClusterHbInit.WarningAtLeastTwoInt", "(specify at least two interfaces)"},
        {"Dialog.ClusterHbInit.RemoveIntButton", "remove"},
        {"Dialog.ClusterHbInit.AddIntButton",    "add"},
        {"Dialog.ClusterHbInit.UseDopdCheckBox.ToolTip", "use DRBD Peer Outdater"},
        {"Dialog.ClusterHbInit.UseDopdCheckBox", ""},
        {"Dialog.ClusterHbInit.NoConfigFound", "no config found"},
        {"Dialog.ClusterHbInit.ConfigsNotTheSame", "configuration files are not the same on all hosts"},
        {"Dialog.ClusterHbInit.Loading", "loading..."},
        {"Dialog.ClusterHbInit.CurrentConfig", "current config:"},
        {"Dialog.ClusterHbInit.Interfaces",    "interfaces:"},

        {"Dialog.ClusterInit.Title",           "Heartbeat/DRBD Initialization"},
        {"Dialog.ClusterInit.Description",     
         "Heartbeat/DRBD Initialization. Load the DRBD and start the Heartbeat, if you wish at this point. If hb status does not work, please check the following:"
         + "<ul><li>Change the hacluster's password to \"hacluster\" on all nodes manually.</li>"
         + "<li>Restart heartbeat. You may have to kill mgmtd process if it hangs.</li></ul>"},
        {"Dialog.ClusterInit.CheckingDrbd",    "checking..."},
        {"Dialog.ClusterInit.LoadDrbdButton",  "Load"},
        {"Dialog.ClusterInit.CheckingHb",      "checking..."},
        {"Dialog.ClusterInit.CheckingHbStatus", "checking..."},
        {"Dialog.ClusterInit.StartHbButton",   "Start"},
        {"Dialog.ClusterInit.HbIsRunning",     "heartbeat is running"},
        {"Dialog.ClusterInit.HbIsStopped",     "heartbeat is stopped"},

        {"Dialog.ClusterInit.DrbdIsLoaded",     "DRBD is loaded"},
        {"Dialog.ClusterInit.DrbdIsNotLoaded",  "DRBD is not loaded"},

        {"Dialog.ClusterInit.HbStatusWorks",     "hb status works"},
        {"Dialog.ClusterInit.HbStatusDoesntWork", "hb status does not work"},
        {"Dialog.ClusterInit.HbStatusWrongPassword", "hacluster's password is unknown"},
        {"Dialog.ClusterInit.EnterPasswordButton", "Enter Password"},

        {"Dialog.ClusterDrbdConf.Title",      "DRBD Configuration"},

        {"Dialog.About.Title",                "DRBD Management Console (Beta). Release: "},
        {"Dialog.About.Description",          
         "(C)opyright 2002-2009 by LINBIT HA-Solutions GmbH.<br>"
         + "Please visit the website http://www.drbd.org/mc/management-console/<br><br>"
         + "DRBD Management Console was written by Rasto Levrinc.<br>"},


        {"Dialog.About.Licences",       
         "DRBD Management Console is free software; you can redistribute it and/or\n"
         + "modify it under the terms of the GNU General Public License as published\n"
         + "by the Free Software Foundation; either version 2, or (at your option)\n"
         + "any later version.\n\n"

         + "DRBD Management Console is distributed in the hope that it will be useful,\n"
         + "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
         + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
         + "GNU General Public License for more details.\n\n"

         + "You should have received a copy of the GNU General Public License\n"
         + "along with DRBD; see the file COPYING.  If not, write to\n"
         + "the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.\n\n"

         + "This software uses the following libraries:\n"
         + "* JUNG, which is released under the terms of the BSD License\n"
         + "* Trilead SSH for Java, released under a BSD style License\n"
         + "* colt, released partly under a CERN permissive license and the LGPL\n"
         + "* bcel, released under the terms of the Apache License\n"
         + "* commons, released under the terms of the Apache License\n"
         + "* muse, released under the terms of the Apache License\n"
         + "* xalan, released under the terms of the Apache License\n"
         + "* xml, released under the terms of the Apache License\n"
         },


        {"Dialog.ClusterFinish.Title",     "Finish"},
        {"Dialog.ClusterFinish.Description",  "Configuration of the cluster is now complete. You can now configure DRBD and Heartbeat services from the menu in the cluster view."},

        {"AppError.Title",                    "Application Error"},
        {"AppError.Text", "An error in application was detected. Please "
                          + "send us the following info, so that we can fix it.\n"},
        {"ClusterDefaultName",                "Cluster "},

        {"ConfirmDialog.Title",               "Confirmation Dialog"},
        {"ConfirmDialog.Description",         "Are you sure?"},
        {"ConfirmDialog.Yes",                 "Yes"},
        {"ConfirmDialog.No",                  "No"},

        {"ClusterBrowser.confirmRemoveAllServices.Title", "Remove All Services"},
        {"ClusterBrowser.confirmRemoveAllServices.Description", "All services and constraints will be removed. Are you sure?"},
        {"ClusterBrowser.confirmRemoveAllServices.Yes", "Remove"},
        {"ClusterBrowser.confirmRemoveAllServices.No",  "Cancel"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Title", "Remove DRBD Resource"},
        {"ClusterBrowser.confirmRemoveDrbdResource.Description", "DRBD Resource @RESOURCE@ will be removed. Are you sure?"},
        {"ClusterBrowser.confirmRemoveDrbdResource.Yes", "Remove"},
        {"ClusterBrowser.confirmRemoveDrbdResource.No",  "Cancel"},

        {"ClusterBrowser.confirmRemoveService.Title", "Remove Service"},
        {"ClusterBrowser.confirmRemoveService.Description", "Service @SERVICE@ will be removed. Are you sure?"},
        {"ClusterBrowser.confirmRemoveService.Yes", "Remove"},
        {"ClusterBrowser.confirmRemoveService.No",  "Cancel"},

        {"ClusterBrowser.confirmRemoveGroup.Title", "Remove Group"},
        {"ClusterBrowser.confirmRemoveGroup.Description", "Group @GROUP@ and its services @SERVICES@ will be removed. Are you sure?"},
        {"ClusterBrowser.confirmRemoveGroup.Yes", "Remove"},
        {"ClusterBrowser.confirmRemoveGroup.No",  "Cancel"},


        {"ClusterBrowser.confirm.Title",       "Remove DRBD Resource"},
        {"ClusterBrowser.confirm.Description", "Resource @RESOURCE@ will be removed. Are you sure?"},
        {"ClusterBrowser.confirm.Yes",         "Remove"},
        {"ClusterBrowser.confirm.No",          "Cancel"},

        {"ClusterBrowser.CreateDir.Title",    "@DIR@ does not exist"},
        {"ClusterBrowser.CreateDir.Description", "Mount point @DIR@ does not exist on @HOST@. Create?"},
        {"ClusterBrowser.CreateDir.Yes",      "Create"},
        {"ClusterBrowser.CreateDir.No",       "Cancel"},

        /* Cluster Resource View */
        {"ClusterBrowser.AllHosts",           "All Hosts"},
        {"ClusterBrowser.ClusterHosts",       "Cluster Hosts"},
        {"ClusterBrowser.Networks",           "Networks"},
        {"ClusterBrowser.CommonBlockDevices", "Shared Disks"},
        {"ClusterBrowser.Drbd",               "DRBD"},
        {"ClusterBrowser.Heartbeat",          "Heartbeat"},
        {"ClusterBrowser.Services",           "Services"},
        {"ClusterBrowser.Scores",             "Scores"},

        {"ClusterBrowser.selectInterface",    "Select interface..."},
        {"ClusterBrowser.DrbdResUnconfigured", "???"},
        {"ClusterBrowser.CommonBlockDevUnconfigured", "???"},
        {"ClusterBrowser.ClusterBlockDevice.Unconfigured", "unconfigured"},
        {"ClusterBrowser.Ip.Unconfigured",       "unconfigured"},

        {"ClusterBrowser.ChooseService",         "Service..."},
        {"ClusterBrowser.AddService",            "Add"},
        {"ClusterBrowser.ApplyResource",         "Apply"},
        {"ClusterBrowser.removeMyselfButton",    "Remove"},
        {"ClusterBrowser.SelectBlockDevice",     "Select..."},
        {"ClusterBrowser.SelectFilesystem",      "Select..."},
        {"ClusterBrowser.SelectNetInterface",    "Select..."},
        {"ClusterBrowser.SelectMountPoint",      "Select..."},
        {"ClusterBrowser.None",                  "None"},

        {"ClusterBrowser.HeartbeatUpdate",       "Getting data from Heartbeat..."},

        {"Browser.ActionsMenu",                  "Actions"},
        {"Browser.Resources",                    "Resources"},
        {"Browser.ParamDefault",                 "Default value: "},
        {"Browser.ParamType",                    "Type: "},

        {"Browser.ExpertMode",                   "Advanced Mode"},
        {"Browser.ApplyResource",                "Apply"},

//        {"Browser.ApplyDrbdResource",           "Apply"},
        {"ClusterBrowser.CreateDrbdConfig",      "Create Config"},
        {"ClusterBrowser.CreateHBConfig",        "Create HB Config"},
        {"ClusterBrowser.HeartbeatId",           "ID"},
        {"ClusterBrowser.HeartbeatClass",        "Class"},
        {"ClusterBrowser.Group",                 "Group"},
        {"ClusterBrowser.HostScores",            "Host Scores"},
        {"ClusterBrowser.Operations",            "Operations"},
        {"ClusterBrowser.AdvancedOperations",    "Other Operations"},
        {"ClusterBrowser.availableServices",     "Available Services"},
        {"HeartbeatOCF.RequiredOptions",         "Required Options"},
        {"HeartbeatOCF.OptionalOptions",         "Advanced Options"},
        {"HeartbeatOCF.GetOCFParameters",        "Getting Heartbeat OCF parameters..."},

        {"ClusterBrowser.Hb.RemoveAllServices",     "Remove All Services"},
        {"ClusterBrowser.Hb.RemoveService",         "Remove"},
        {"ClusterBrowser.Hb.AddService",            "Add Service"},
        {"ClusterBrowser.Hb.AddStartBefore",        "Start Before"},
        {"ClusterBrowser.Hb.AddDependentGroup",     "Add Dependent Group"},
        {"ClusterBrowser.Hb.AddDependency",         "Add Dependent Service"},
        {"ClusterBrowser.Hb.AddGroupService",       "Add Group Service"},
        {"ClusterBrowser.Hb.AddGroup",              "Add Group"},
        {"ClusterBrowser.Hb.StartResource",         "Start"},
        {"ClusterBrowser.Hb.StartResource.ToolTip", "Start heartbeat resource"},
        {"ClusterBrowser.Hb.StopResource",          "Stop"},
        {"ClusterBrowser.Hb.StopResource.ToolTip",  "Stop heartbeat resource"},
        {"ClusterBrowser.Hb.MigrateResource",       "Migrate To"},
        {"ClusterBrowser.Hb.MigrateResource.ToolTip", "Migrate resource to other node"},
        {"ClusterBrowser.Hb.UnmigrateResource",     "Unmigrate"},
        {"ClusterBrowser.Hb.UnmigrateResource.ToolTip", "Unmigrate"},
        {"ClusterBrowser.Hb.ViewServiceLog",        "View Service Log"},

        {"HeartbeatGraph.ColOrd",                   "col / ord"},
        {"HeartbeatGraph.Colocation",               "colocated"},
        {"HeartbeatGraph.Order",                    "ordered"},

        {"HeartbeatGraph.Removing",                 " removing... "},
        {"HeartbeatGraph.Unconfigured",             "unconfigured"},

        {"ClusterBrowser.Hb.RemoveEdge",            "Remove Colocation and Order"},
        {"ClusterBrowser.Hb.RemoveEdge.ToolTip",    "Remove order and co-location dependencies."},
        {"ClusterBrowser.Hb.RemoveOrder",           "Remove Order"},
        {"ClusterBrowser.Hb.RemoveOrder.ToolTip",   "Remove order dependency."},
        {"ClusterBrowser.Hb.ReverseOrder",          "Reverse Order"},
        {"ClusterBrowser.Hb.ReverseOrder.ToolTip",  "Reverse order of the constraint."},
        {"ClusterBrowser.Hb.RemoveColocation",      "Remove Colocation"},
        {"ClusterBrowser.Hb.RemoveColocation.ToolTip", "Remove co-location dependency."},
        {"ClusterBrowser.Hb.AddOrder",              "Add Order"},
        {"ClusterBrowser.Hb.AddOrder.ToolTip",      "Add order dependency."},
        {"ClusterBrowser.Hb.AddColocation",         "Add Colocation"},
        {"ClusterBrowser.Hb.AddColocation.ToolTip", "Add co-location dependency."},
        {"ClusterBrowser.Hb.CleanUpResource",       "Clean Up"},
        {"ClusterBrowser.Hb.ViewLogs",              "View Logs"},
        {"ClusterBrowser.Hb.ResGrpMoveUp",          "Move Up"},
        {"ClusterBrowser.Hb.ResGrpMoveDown",        "Move Down"},

        {"ClusterBrowser.Drbd.RemoveEdge",                 "Remove DRBD Resource"},
        {"ClusterBrowser.Drbd.RemoveEdge.ToolTip",         "Remove DRBD Resource"},
        {"ClusterBrowser.Drbd.ResourceConnect",            "Connect"},
        {"ClusterBrowser.Drbd.ResourceConnect.ToolTip",    "Connect"},
        {"ClusterBrowser.Drbd.ResourceDisconnect",         "Disconnect"},
        {"ClusterBrowser.Drbd.ResourceDisconnect.ToolTip", "Disconnect"},
        {"ClusterBrowser.Drbd.ResourceResumeSync",         "Resume Sync"},
        {"ClusterBrowser.Drbd.ResourceResumeSync.ToolTip", "Resume Sync"},
        {"ClusterBrowser.Drbd.ResourcePauseSync",          "Pause Sync"},
        {"ClusterBrowser.Drbd.ResourcePauseSync.ToolTip",  "Pause Sync"},
        {"ClusterBrowser.Drbd.ResolveSplitBrain",          "Resolve Split-Brain"},
        {"ClusterBrowser.Drbd.ResolveSplitBrain.ToolTip",  "Resolve Split-Brain"},
        {"ClusterBrowser.Drbd.ViewLogs",                   "View Logs"},

        {"ClusterBrowser.HbUpdateResources",               "updating heartbeat resources..."},
        {"ClusterBrowser.HbUpdateStatus",                  "updating heartbeat status..."},
        {"ClusterBrowser.DrbdUpdate",                      "updating DRBD resources..."},
        {"ClusterBrowser.DifferentHbVersionsWarning",      "<i>warning: different hb versions</i>"},

        {"ClusterBrowser.HbStatusFailed",                  "<h2>HB status failed</h2>"},
        {"ClusterBrowser.Hb.ManageResource",               "Manage by HB"},
        {"ClusterBrowser.Hb.ManageResource.ToolTip",       "Manage by HB"},
        {"ClusterBrowser.Hb.UnmanageResource",             "Do not manage by HB"},
        {"ClusterBrowser.Hb.UnmanageResource.ToolTip",     "Do not Manage by HB"},

        {"HostBrowser.Drbd.AddDrbdResource",            "Add Mirrored Disk"},
        {"HostBrowser.Drbd.RemoveDrbdResource",         "Remove DRBD Resource"},
        {"HostBrowser.Drbd.SetPrimary",                 "Set Primary"},
        {"HostBrowser.Drbd.SetPrimaryOtherSecondary",   "Change To Primary"},
        {"HostBrowser.Drbd.Attach",                     "Attach Disk"},
        {"HostBrowser.Drbd.Attach.ToolTip",             "Attach underlying disk"},
        {"HostBrowser.Drbd.Detach",                     "Detach Disk"},
        {"HostBrowser.Drbd.Detach.ToolTip",             "Detach underlying disk and make this DRBD device diskless"},
        {"HostBrowser.Drbd.Connect",                    "Connect To Peer"},
        {"HostBrowser.Drbd.Disconnect",                 "Disconnect From Peer"},
        {"HostBrowser.Drbd.SetSecondary",               "Set Secondary"},
        {"HostBrowser.Drbd.SetSecondary.ToolTip",       "Set secondary"},
        {"HostBrowser.Drbd.ForcePrimary",               "Force Primary"},
        {"HostBrowser.Drbd.Invalidate",                 "Invalidate"},
        {"HostBrowser.Drbd.Invalidate.ToolTip",         "Invalidate data on this device and start full sync from other node"},
        {"HostBrowser.Drbd.DiscardData",                "Discard Data"},
        {"HostBrowser.Drbd.DiscardData.ToolTip",        "Discard data and start full sync from other node"},
        {"HostBrowser.Drbd.Resize",                     "Resize"},
        {"HostBrowser.Drbd.Resize.ToolTip",             "Resize DRBD block device, when underlying<br>block device was resized"},
        {"HostBrowser.Drbd.ResumeSync",                 "Resume Sync"},
        {"HostBrowser.Drbd.ResumeSync.ToolTip",         "Resume sync"},
        {"HostBrowser.Drbd.PauseSync",                  "Pause Sync"},
        {"HostBrowser.Drbd.PauseSync.ToolTip",          "Pause sync"},
        {"HostBrowser.Drbd.ViewLogs",                   "View Logs"},
        {"HostBrowser.Drbd.AttachAll",                  "Attach All Detached"},

        {"HostBrowser.HostWizard",                      "Host Wizard"},
        {"HostBrowser.Drbd.LoadDrbd",                   "Load DRBD"},
        {"HostBrowser.Drbd.UpAll",                      "Start All DRBDs"},
        {"HostBrowser.Drbd.UpgradeDrbd",                "Upgrade DRBD"},
        {"HostBrowser.Drbd.ChangeHostColor",            "Change Color"},
        {"HostBrowser.Drbd.ViewLogs",                   "View Logs"},
        {"HostBrowser.Drbd.ViewDrbdLog",                "View Log File"},
        {"HostBrowser.Drbd.ConnectAll",                 "Connect All DRBDs"},
        {"HostBrowser.Drbd.DisconnectAll",              "Disconnect All DRBDs"},
        {"HostBrowser.Drbd.SetAllPrimary",              "Set All DRBDs Primary"},
        {"HostBrowser.Drbd.SetAllSecondary",            "Set All DRBDs Secondary"},
        {"HostBrowser.Heartbeat.StandByOn",             "Heartbeat Standby On"},
        {"HostBrowser.Heartbeat.StandByOff",            "Heartbeat Standby Off"},

        /* Host Browser */
        {"HostBrowser.idDrbdNode",            "is DRBD node"},
        {"HostBrowser.NetInterfaces",         "Net Interfaces"},
        {"HostBrowser.BlockDevices",          "Block Devices"},
        {"HostBrowser.FileSystems",           "File Systems"},
        {"HostBrowser.MetaDisk.Internal",     "Internal"},
        {"HostBrowser.DrbdNetInterface.Select", "Select..."},
        {"HostBrowser.NoInfoAvailable",        "no info available"},

        {"ClusterBrowser.Host.Offline",       "Offline"},
        {"ClusterBrowser.AddNewHost",         "New Host / Wizard"},
        {"GuiComboBox.Select",                "Select..."},

        /* Score */
        {"Score.InfinityString",              "always"},
        {"Score.MinusInfinityString",         "never"},
        {"Score.ZeroString",                  "don't care"},
        {"Score.PlusString",                  "preferred"},
        {"Score.MinusString",                 "better not"},
        {"Score.Unknown",                     "unknown"},

        {"SSH.Enter.passphrase",                "Enter passphrase for key: "},
        {"SSH.Publickey.Authentication.Failed", "Authentication failed."},
        {"SSH.RSA.DSA.Authentication",          "RSA/DSA Authentication"},

        {"Boolean.True",                      "True"},
        {"Boolean.False",                     "False"},

        {"Heartbeat.2.1.3.Boolean.True",      "true"},
        {"Heartbeat.2.1.3.Boolean.False",     "false"},

        {"Heartbeat.Boolean.True",            "True"},
        {"Heartbeat.Boolean.False",           "False"},

        {"DrbdNetInterface",                  "DRBD net interface"},
        {"DrbdNetInterface.Long",             "DRBD network interface"},

        {"DrbdMetaDisk",                      "DRBD meta disk"},
        {"DrbdMetaDisk.Long",                 "DRBD meta disk"},

        {"DrbdNetInterfacePort",              "DRBD net interface port"},
        {"DrbdNetInterfacePort.Long",         "DRBD network interface port"},

        {"DrbdMetaDiskIndex",                 "DRBD meta disk index"},
        {"DrbdMetaDiskIndex.Long",            "DRBD meta disk index"},

        {"ProgressIndicatorPanel.Cancel",     "Cancel"},

        {"Heartbeat.ExecutingCommand",        "Executing Heartbeat command..."},
        {"DRBD.ExecutingCommand",             "Executing DRBD command..."},
        {"DrbdXML.GetConfig",                 "Getting DRBD configuration..."},
        {"DrbdXML.GetParameters",             "Getting DRBD parameters..."},

        {"Dialog.DrbdConfigResource.Title",      "Configure DRBD Resource"},
        {"Dialog.DrbdConfigResource.Description", "Configure the new DRBD resource. Enter the <b>name</b> of the resource. "
                                                  + "You can call it whatever you want as long it is unique. "
                                                  + "The same applies for DRBD device. "
                                                  + "This <b>device</b> should be in the form /dev/drbdX. "
                                                  + "Choose a <b>protocol</b> that the DRBD should use for replication. "
                                                  + "You can learn more about protocols -- a.k.a replication modes -- in <a href=\"http://www.drbd.org/docs/introduction/\">DRBD User's Guide: Introduction to DRBD</a>. After you changed the fields, or you are satisfied with the defaults, press <b>Next</b> to continue."},

        {"Dialog.DrbdConfigBlockDev.Title",      "Configure DRBD Block Device"},
        {"Dialog.DrbdConfigBlockDev.Description", "Enter the information about the DRBD block device. Choose a DRBD net interface that will be used for DRBD communication and a port. The port must not be used by anything else and must not be used by another DRBD block device. The net interface should be different than the one that is used by Heartbeat. Choose where DRBD meta data should be written. You can choose internal to make it simple or learn external DRBD meta disk to make it faster."},

        {"Dialog.DrbdConfigCreateFS.Title",            "Initialize DRBD block devices."},
        {"Dialog.DrbdConfigCreateFS.Description",      "In this step you can initialize and start the DRBD cluster. You can choose one host as a primary host. You can create a filesystem on it, but in this case you have to choose one host as a primary, choose the file system and press \"Create File System\" button."},
        {"Dialog.DrbdConfigCreateFS.NoHostString",     "none"},
        {"Dialog.DrbdConfigCreateFS.ChooseHost",       "host (primary)"},
        {"Dialog.DrbdConfigCreateFS.Filesystem",       "file system"},
        {"Dialog.DrbdConfigCreateFS.ChooseFilesystem", "file system"},
        {"Dialog.DrbdConfigCreateFS.SelectFilesystem", "Use existing data"},
        {"Dialog.DrbdConfigCreateFS.CreateFsButton",   "Create File System"},

        {"Dialog.DrbdConfigCreateMD.Title",           "Create DRBD Meta-Data"},
        {"Dialog.DrbdConfigCreateMD.Description",     "In this step you can create new meta-data, overwrite them or use the old ones, if they are already there."},
        {"Dialog.DrbdConfigCreateMD.Metadata", "meta-data"},
        {"Dialog.DrbdConfigCreateMD.UseExistingMetadata", "Use existing meta-data"},
        {"Dialog.DrbdConfigCreateMD.CreateNewMetadata", "Create new meta-data"},
        {"Dialog.DrbdConfigCreateMD.CreateNewMetadataDestroyData", "Create new meta-data & destroy data"},
        {"Dialog.DrbdConfigCreateMD.OverwriteMetadata", "Overwrite meta-data"},
        {"Dialog.DrbdConfigCreateMD.CreateMDButton",    "Create Meta-Data"},
        {"Dialog.DrbdConfigCreateMD.OverwriteMDButton", "Overwrite Meta-Data"},
        {"Dialog.DrbdConfigCreateMD.CreateMD.Done",     "Meta-data on @HOST@ have been created."  },
        {"Dialog.DrbdConfigCreateMD.CreateMD.Failed",            "Could not create meta-data on @HOST@."  },
        {"Dialog.DrbdConfigCreateMD.CreateMD.Failed.40",         "Could not create meta-data on @HOST@, because there is no room for it. You can either destroy the file\nsystem from here by choosing 'Create new meta-data & destroy data' from the pull-down menu, resize\nthe file system manually, or use external meta-data."  },

        {"Dialog.DrbdSplitBrain.Title",       "Resolve DRBD Split-Brain"},
        {"Dialog.DrbdSplitBrain.Description", "A split-brain condition was detected, a condition where two or more nodes have written to the same DRBD block device without knowing about each other. Choose the host which you think have newer and/or more correct data. Be warned though, the data from other host on this block device will be discarded."},
        {"Dialog.DrbdSplitBrain.ChooseHost",  "host"},
        {"Dialog.DrbdSplitBrain.ResolveButton", "Resolve"},

        {"Dialog.Logs.Title",                 "Log Viewer"},

        {"Error.Title",                       "Error"},
    };
}
