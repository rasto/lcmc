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
        {"DrbdMC.Title",
         "DRBD Management Console (Beta)"},

        /* Main Menu */
        {"MainMenu.Session",
         "Session"},

        {"MainMenu.New",
         "New"},

        {"MainMenu.Load",
         "Load"},

        {"MainMenu.Save",
         "Save"},

        {"MainMenu.SaveAs",
         "Save As"},

        {"MainMenu.Host",
         "Host"},

        {"MainMenu.Cluster",
         "Cluster"},

        {"MainMenu.RemoveEverything",
         "Remove Everything"},

        {"MainMenu.Exit",
         "Exit"},

        {"MainMenu.Settings",
         "Settings"},

        {"MainMenu.LookAndFeel",
         "Look And Feel"},

        {"MainMenu.Help",
         "Help"},

        {"MainMenu.About",
         "About"},

        {"MainMenu.DrbdGuiFiles",
         "DRBD Management Console files"},

        /* Main panel */
        {"MainPanel.Clusters",
         "Clusters"},

        {"MainPanel.ClustersAlt",
         "Click here for clusters view"},

        {"MainPanel.Hosts",
         "Hosts"},

        {"MainPanel.HostsAlt",
         "Click here for hosts view"},

        {"MainPanel.UpgradeCheck",
         "checking for available upgrades..."},

        {"MainPanel.UpgradeCheckFailed",
         "check for upgrade failed"},

        {"MainPanel.UpgradeAvailable",
         "<font color=black>there&nbsp;is&nbsp;a&nbsp;new&nbsp;DRBD-MC&nbsp;@LATEST@&nbsp;"
         + "available</font><br><a href=\"http://oss.linbit.com/drbd-mc/"
         + "DMC-@LATEST@.jar?from-drbd-mc\">Get it here</a>!"},

        {"MainPanel.NoUpgradeAvailable",
         ""},

        /** Clusters panel */
        {"ClustersPanel.NewTabTip",
         "New Cluster"},

        /** Hosts panel */
        {"HostsPanel.NewTabTip",
         "New Host"},

        /** Tools */
        {"Tools.ExecutingCommand",
         "Executing command..."},

        {"Tools.CommandDone",
         "[done]"},

        {"Tools.CommandFailed",
         "[failed]"},

        {"Tools.Loading",
         "Loading..."},

        {"Tools.Saving",
         "Saving \"@FILENAME@\"..."},

        {"Tools.Warning.Title",
         "Warning: "},

        {"Tools.sshError.command",
         "Command:"},

        {"Tools.sshError.returned",
         "returned exit code"},

        /* Cluster tab */
        {"ClusterTab.AddNewCluster",
         "Cluster Wizard"},

        {"ClusterTab.AddNewHost",
         "Host Wizard"},

        /* Cluster view panel */
        {"ClusterViewPanel.ClusterButtons",
         "Cluster"},

        {"ClusterViewPanel.ClusterWizard",
         "Cluster Wizard"},

        /* Progress bar */
        {"ProgressBar.Cancel",
         "Cancel"},

        /* Dialogs */
        {"Dialog.Dialog.Next",
         "Next"},

        {"Dialog.Dialog.Back",
         "Back"},

        {"Dialog.Dialog.Cancel",
         "Cancel"},

        {"Dialog.Dialog.Finish",
         "Finish"},

        {"Dialog.Dialog.Retry",
         "Retry"},

        {"Dialog.Dialog.PrintErrorAndRetry",
         "Command failed."},

        {"Dialog.Dialog.Ok",
         "OK"},

        {"Dialog.Host.NewHost.Title",
         "Host Wizard"},

        {"Dialog.Host.NewHost.Description",
         "Enter the <b>hostname/IP</b> and <b>username</b> of the server. "
         + "Host can be entered either as hostname or IP address. Enter a "
         + "hostname only if it is resolvable by DNS. Username will be used "
         + "for SSH connections and command executions. Normally it should "
         + "be the <b>root</b> user. It is recommended not to change it."
         + "<br><br>You may enter more hosts delimited with \",\", if the "
         + "server is reachable not directly but via several <b>hops</b>. "
         + "If this is the case you have to enter the same number of "
         + "usernames and hostnames/IPs as they are hops."},

        {"Dialog.Host.NewHost.EnterHost",
         "Host:"},

        {"Dialog.Host.NewHost.EnterUsername",
         "Username:"},

        {"Dialog.Host.NewHost.SSHPort",
         "SSH Port:"},

        {"Dialog.Host.NewHost.EnterPassword",
         "Password:"},

        {"Dialog.Host.Configuration.Title",
         "Host configuration"},

        {"Dialog.Host.Configuration.Description",
         "Trying to do a DNS lookup of the host. If DNS lookup failed, "
         + "go back and enter an IP of the host if the host is not "
         + "resolvable by DNS or make the hostname resolvable."},

        {"Dialog.Host.Configuration.Name",
         "nodename:"}, // TODO: this is not necessary anymore

        {"Dialog.Host.Configuration.Hostname",
         "hostname:"},

        {"Dialog.Host.Configuration.Ip",
         "IP:"},

        {"Dialog.Host.Configuration.DNSLookup",
         "DNS lookup"},

        {"Dialog.Host.Configuration.DNSLookupOk",
         "DNS lookup done."},

        {"Dialog.Host.Configuration.DNSLookupError",
         "DNS lookup failed."},

        {"Dialog.Host.SSH.Title",
         "Create SSH Connection"},

        {"Dialog.Host.SSH.Description",
         "Trying to connect to host via ssh. You can either enter "
         + "a RSA or DSA key or enter a password in the pop up dialog. "
         + "You can switch between passphrase and password "
         + "authentication by pressing enter without entering "
         + "anything."},

        {"Dialog.Host.SSH.Connecting",
         "Connecting..."},

        {"Dialog.Host.SSH.Connected",
         "Connection established."},

        {"Dialog.Host.SSH.NotConnected",
         "Connection failed."},

        {"Dialog.Host.Devices.Title",
         "Host devices"},

        {"Dialog.Host.Devices.Description",
         "Trying to retrieve information about block, network devices "
         + "and installation information of the host."},

        {"Dialog.Host.Devices.Executing",
         "Executing..."},

        {"Dialog.Host.Devices.CheckError",
         "Failed."},

        {"Dialog.Host.DrbdLinbitAvailPackages.Title",
         "Available Packages"},

        {"Dialog.Host.DrbdLinbitAvailPackages.Description",
         "Trying to match distribution, kernel package and architecture of "
         + "the server to the available binary DRBD packages. If none is "
         + "selected, most likely there is no DRBD package available for "
         + "your system. If you use stock kernel of your distribution, the "
         + "package will be provided to you by LINBIT support (not free). "
         + "After that you may retry this step again."},

        {"Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Dist",
         "There are no DRBD packages available for your distribution."},

        {"Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Kernel",
         "There are no DRBD packages available for your kernel version."},

        {"Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Arch",
         "There are no DRBD packages available at www.linbit.com for your "
         + "kernel architecture."},

        {"Dialog.Host.DrbdLinbitAvailPackages.AvailablePackages",
         "Available packages at www.linbit.com: "},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoDist",
         "DRBD package not found."},

        {"Dialog.Host.DrbdLinbitAvailPackages.NotALinux",
         "Cannot determine an operating system."},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoArch",
         "Cannot determine the architecture"},

        {"Dialog.Host.DrbdLinbitAvailPackages.Executing",
         "Looking for available packages..."},

        {"Dialog.Host.DrbdLinbitAvailPackages.AvailVersions",
         "Available versions: "},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoKernels",
         "Not available for this kernel"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoVersions",
         "Could not find any DRBD versions"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoDistributions",
         "No distributions found"},

        {"Dialog.Host.DrbdLinbitAvailPackages.NoArchs",
         "No archs found"},

        {"Dialog.Host.DistDetection.Title",
         "Distribution Detection"},

        {"Dialog.Host.DistDetection.Description",
         "Trying to detect the Linux distribution of the host. It is Linux, "
         + "right? If none is detected, it means that the distribution is "
         + "not supported. You may then choose a distribution that is "
         + "similar, which may or may not work for you."},

        {"Dialog.Host.DistDetection.Executing",
         "Executing..."},

        {"Dialog.Host.CheckInstallation.Title",
         "Installation Check"},

        {"Dialog.Host.CheckInstallation.Description",
         "Checking if DRBD, Pacemaker and other important "
         + "components are already installed. If not, you can press "
         + "one of the 'Install' buttons to install them. You can check "
         + "for DRBD upgrade as well if installed DRBD was detected. "
         + "Installing or upgrading of DRBD via binary packages is "
         + "possible only if you have support contract with LINBIT.<br>"
         + "You can also choose a <b>Pacemaker</b> installation method. "
         + "The newest packages for many different distributions are in "
         + "Clusterlabs and Opensuse repositories, yes even if your "
         + "distribution is not an Opensuse."},

        {"Dialog.Host.CheckInstallation.Drbd.NotInstalled",
         "DRBD is not installed. Click 'Install' button to install a new "
         + "shiny DRBD."},

        {"Dialog.Host.CheckInstallation.Heartbeat.AlreadyInstalled",
         "is already installed."},

        {"Dialog.Host.CheckInstallation.Heartbeat.NotInstalled",
         "Heartbeat is not installed or is installed improperly. "
         + "Press 'Next' button in order to install the Heartbeat packages."},

        {"Dialog.Host.CheckInstallation.Heartbeat.CheckError",
         "Check failed."},

        {"Dialog.Host.CheckInstallation.Checking",
         "Checking..."},

        {"Dialog.Host.CheckInstallation.CheckError",
         "Check failed."},

        {"Dialog.Host.CheckInstallation.AllOk",
         "All required components are installed."},

        {"Dialog.Host.CheckInstallation.SomeFailed",
         "Some of the required components are not installed."},

        {"Dialog.Host.CheckInstallation.DrbdNotInstalled",
         "not installed"},

        {"Dialog.Host.CheckInstallation.PmNotInstalled",
         "not installed"},

        {"Dialog.Host.CheckInstallation.HbPmNotInstalled",
         "not installed"},

        {"Dialog.Host.CheckInstallation.DrbdUpgradeButton",
         "Upgrade"},

        {"Dialog.Host.CheckInstallation.DrbdCheckForUpgradeButton",
         "Check for Upgrade"},

        {"Dialog.Host.CheckInstallation.DrbdInstallButton",
         "Install"},

        {"Dialog.Host.CheckInstallation.PmInstallButton",
         "Install"},

        {"Dialog.Host.CheckInstallation.HbPmInstallButton",
         "Install"},

        {"Dialog.Host.CheckInstallation.CheckingPm",
         "checking..."},

        {"Dialog.Host.CheckInstallation.CheckingHbPm",
         "checking..."},

        {"Dialog.Host.CheckInstallation.CheckingDrbd",
         "checking..."},

        {"Dialog.Host.CheckInstallation.PmInstallMethod",
         "Installation method: "},

        {"Dialog.Host.CheckInstallation.HbPmInstallMethod",
         "Installation method: "},

        {"Dialog.Host.CheckInstallation.DrbdInstallMethod",
         "Installation method: "},

        {"Dialog.Host.LinbitLogin.Title",
         "Log in"},

        {"Dialog.Host.LinbitLogin.Description",
         "You need to log in to the http://www.linbit.com/support download "
         + "area to get a package for your distribution. Please provide "
         + "your username and password. Contact LINBIT support to obtain "
         + "one. Alternatively you can install/upgrade the DRBD yourself "
         + "and go back to the previous dialog in order to continue.<br><br>"
         + "If you enter a wrong username or password you will find out "
         + "about it, only in the next step, when you will be unable to "
         + "download anything."},

        {"Dialog.Host.LinbitLogin.EnterUser",
         "Username"},

        {"Dialog.Host.LinbitLogin.EnterPassword",
         "Password"},

        {"Dialog.Host.LinbitLogin.Save",
         "Save"},

        {"Dialog.Host.DrbdAvailFiles.Title",
         "Available DRBD Packages"},

        {"Dialog.Host.DrbdAvailFiles.Description",
         "Trying to detect available packages. There should be one module "
         + "and one util package if you use stock distribution kernel. "
         + "If no package was auto-detected, you may choose appropriate "
         + "kernel version in the pull down menu. It is also possible "
         + "that packages were not build for your system at all. In that "
         + "case contact LINBIT support and packages will be provided "
         + "to you as soon as possible."},

        {"Dialog.Host.DrbdAvailFiles.Executing",
         "Executing..."},

        {"Dialog.Host.DrbdAvailFiles.NoFiles",
         "No packages found."},

        {"Dialog.Host.DrbdAvailFiles.NoBuilds",
         "No builds found."},

        {"Dialog.Host.DrbdAvailSourceFiles.Title",
         "Available DRBD Source Tarballs"},

        {"Dialog.Host.DrbdAvailSourceFiles.Description",
         "Trying to parse available source tarballs from the LINBIT website. "
         + "If you don't know which DRBD version should be installed, take "
         + "the already selected one, this is also the newest one."},

        {"Dialog.Host.DrbdAvailSourceFiles.Executing",
         "Executing..."},

        {"Dialog.Host.DrbdAvailSourceFiles.NoBuilds",
         "Could not find any builds"},

        {"Dialog.Host.DrbdLinbitInst.Title",
         "DRBD Installation"},

        {"Dialog.Host.DrbdLinbitInst.Description",
         "DRBD is being installed. If it fails with authorization errors, "
         + "you entered a wrong username or password, going back, entering "
         + "it correctly, would fix it. Other possibility is that wrong "
         + "distribution was selected and therefore the installation did "
         + "not work. Yet another remote possibility is that LINBIT "
         + "servers are down, but this is not very likely, since LINBIT "
         + "servers are fault-tolerant using DRBD."},

        {"Dialog.Host.DrbdLinbitInst.CheckingFile",
         "Checking installed file..."},

        {"Dialog.Host.DrbdLinbitInst.FileExists",
         "File already exists."},

        {"Dialog.Host.DrbdLinbitInst.Downloading",
         "Downloading..."},

        {"Dialog.Host.DrbdLinbitInst.Installing",
         "DRBD is being installed..."},

        {"Dialog.Host.DrbdLinbitInst.InstallationDone",
         "Installation done."},

        {"Dialog.Host.DrbdLinbitInst.InstallationFailed",
         "Installation failed."},

        {"Dialog.Host.DrbdLinbitInst.Executing",
         "Executing..."},

        {"Dialog.Host.DrbdLinbitInst.Starting",
         "Starting DRBD..."},

        {"Dialog.Host.DrbdLinbitInst.MkdirError",
         "Could not create a directory"},

        {"Dialog.Host.DrbdLinbitInst.WgetError",
         "Could not get DRBD packages."},

        {"Dialog.Host.HeartbeatInst.Title",
         "Heartbeat Install"},

        {"Dialog.Host.HeartbeatInst.Description",
         "Heartbeat and Pacemaker packages are being installed."},

        {"Dialog.Host.HeartbeatInst.Executing",
         "Installing..."},

        {"Dialog.Host.HeartbeatInst.InstOk",
         "Heartbeat and Pacemaker were successfully installed."},

        {"Dialog.Host.HeartbeatInst.InstError",
         "Installation error: you may have to go to the command line and fix "
         + "whatever needs fixing there."},

        {"Dialog.Host.PacemakerInst.Title",
         "Corosync/OpenAIS/Pacemaker Install"},

        {"Dialog.Host.PacemakerInst.Description",
         "Pacemaker with Corosync and/or OpenAIS packages is being installed."},

        {"Dialog.Host.PacemakerInst.Executing",
         "Installing..."},

        {"Dialog.Host.PacemakerInst.InstOk",
         "Pacemaker was successfully installed."},

        {"Dialog.Host.PacemakerInst.InstError",
         "Installation error: you may have to go to the command line and fix "
         + "whatever needs fixing there."},

        {"Dialog.Host.DrbdCommandInst.Title",
         "DRBD Install"},

        {"Dialog.Host.DrbdCommandInst.Description",
         "DRBD is being installed. For some distributions, especially "
         + "older RedHats, if you use an older kernel than is currently "
         + "available, you may need to find, download and install the "
         + "<b>kernel-devel</b> package for your kernel yourself or update "
         + "the kernel. You can find out your kernel version with \"uname -r\" "
         + "command. After that you can retry this step again."},

        {"Dialog.Host.DrbdCommandInst.Executing",
         "Installing..."},

        {"Dialog.Host.DrbdCommandInst.InstOk",
         "DRBD was successfully installed."},

        {"Dialog.Host.DrbdCommandInst.InstError",
         "Installation error: you may have to go to the command line and fix "
         + "whatever needs fixing there:\n"},

        {"Dialog.Host.Finish.Title",
         "Finish"},

        {"Dialog.Host.Finish.Description",
         "Configuration of the host is now complete. You can now add another "
         + "host or configure a cluster."},

        {"Dialog.Host.Finish.AddAnotherHostButton",
         "Add Another Host"},

        {"Dialog.Host.Finish.ConfigureClusterButton",
         "Configure Cluster"},

        {"Dialog.Host.Finish.Save",
         "Save"},

        {"Dialog.ConfigDialog.NoMatch",
         "No Match"},

        {"Dialog.ConfigDialog.SkipButton",
         "Skip"},

        {"Dialog.ConnectDialog.Title",
         "SSH Connection"},

        {"Dialog.ConnectDialog.Description",
         "Trying to establish an SSH connection."},

        {"Dialog.Cluster.Name.Title",
         "Cluster Wizard"},

        {"Dialog.Cluster.Name.EnterName",
         "Name:"},

        {"Dialog.Cluster.Name.Description",
         "Enter a name for the cluster. This name can be anything as long "
         + "as it is unique. It is used only as an identification in "
         + "the GUI and can be changed later."},

        {"Dialog.Cluster.ClusterHosts.Title",
         "Select Hosts"},

        {"Dialog.Cluster.ClusterHosts.Description",
         "Select two or more hosts that are part of the "
         + "DRBD/Pacemaker cluster."},

        {"Dialog.Cluster.Connect.Title",
         "Cluster Connect"},
        {"Dialog.Cluster.Connect.Description",
         "Trying to connect to all the hosts in the cluster."},

        {"Dialog.Cluster.CommStack.Title",
         "Cluster Communication Stack"},

        {"Dialog.Cluster.CommStack.Description",
         "Now you have to choose between Corosync/OpenAIS and Heartbeat, "
         + "if you have "
         + "installed both. It is either one or another from now on, although "
         + "theoretically you may be able seamlessly switch between them at "
         + "any time. "
         + "Heartbeat is more widely used and thus better "
         + "tested at the moment, but is unmaintained and is being completely "
         + "displaced by Corosync."},

        {"Dialog.Cluster.CoroConfig.Title",
         "Corosync/OpenAIS Config File"},

        {"Dialog.Cluster.CoroConfig.Description",
         "In this step Corosync config (/etc/corosync/corosync.conf) or "
         + "OpenAIS config (/etc/ais/openais.conf) is created and "
         + "OpenAIS is started. You do not have to overwrite your old "
         + "config if you have some special options. You can modify it by "
         + "hand on every host in the cluster. You have to press the "
         + "\"Create Config\" button to save the new configuration on all "
         + "hosts. "},

        {"Dialog.Cluster.CoroConfig.CreateAisConfig",
         "Create/Overwrite Config"},

        {"Dialog.Cluster.CoroConfig.WarningAtLeastTwoInt",
         "# (specify at least two interfaces)"}, // TODO: does not work so good

        {"Dialog.Cluster.CoroConfig.WarningAtLeastTwoInt.OneMore",
         "# (specify at least two interfaces: one more to go)"},

        {"Dialog.Cluster.CoroConfig.RemoveIntButton",
         "remove"},

        {"Dialog.Cluster.CoroConfig.AddIntButton",
         "add"},

        {"Dialog.Cluster.CoroConfig.UseDopdCheckBox.ToolTip",
         "use DRBD Peer Outdater"},

        {"Dialog.Cluster.CoroConfig.UseDopdCheckBox",
         ""},

        {"Dialog.Cluster.CoroConfig.UseMgmtdCheckBox.ToolTip",
         "use mgmtd, if you want to use Pacemaker-GUI"},

        {"Dialog.Cluster.CoroConfig.UseMgmtdCheckBox",
         ""},

        {"Dialog.Cluster.CoroConfig.NoConfigFound",
         ": file not found"},

        {"Dialog.Cluster.CoroConfig.ConfigsNotTheSame",
         "configuration files are not the same on all hosts"},

        {"Dialog.Cluster.CoroConfig.Loading",
         "loading..."},

        {"Dialog.Cluster.CoroConfig.CurrentConfig",
         "current config:"},

        {"Dialog.Cluster.CoroConfig.Interfaces",
         "interfaces:"},

        {"Dialog.Cluster.CoroConfig.ais.conf.ok",
         " the same on all nodes"},

        {"Dialog.Cluster.CoroConfig.Checkbox.EditConfig",
         "edit a new config"},

        {"Dialog.Cluster.CoroConfig.Checkbox.SeeExisting",
         "see the existing configs"},

        {"Dialog.Cluster.HbConfig.Title",
         "Heartbeat Initialization"},

        {"Dialog.Cluster.HbConfig.Description",
         "In this step Heartbeat config (/etc/ha.d/ha.cf) is created and "
         + "Heartbeat is started. You do not have to overwrite your old "
         + "config if you have some special options. You can modify it by "
         + "hand on every host in the cluster. You have to press the "
         + "\"Create HB Config\" button to save the new configuration on all "
         + "hosts. By the way it is OK to have ucast address to the node's "
         + "own interface, it will be ignored, at the same time it allows to "
         + "have the same config file on all hosts."},

        {"Dialog.Cluster.HbConfig.CreateHbConfig",
         "Create/Overwrite HB Config"},

        {"Dialog.Cluster.HbConfig.WarningAtLeastTwoInt",
         "# (specify at least two interfaces)"},

        {"Dialog.Cluster.HbConfig.WarningAtLeastTwoInt.OneMore",
         "# (specify at least two interfaces: one more to go)"},

        {"Dialog.Cluster.HbConfig.RemoveIntButton",
         "remove"},

        {"Dialog.Cluster.HbConfig.AddIntButton",
         "add"},

        {"Dialog.Cluster.HbConfig.UseDopdCheckBox.ToolTip",
         "use DRBD Peer Outdater"},

        {"Dialog.Cluster.HbConfig.UseDopdCheckBox",
         ""},

        {"Dialog.Cluster.HbConfig.UseMgmtdCheckBox.ToolTip",
         "use mgmtd, if you want to use Pacemaker-GUI"},

        {"Dialog.Cluster.HbConfig.UseMgmtdCheckBox",
         ""},

        {"Dialog.Cluster.HbConfig.NoConfigFound",
         "/etc/ha.d/ha.cf: file not found"},

        {"Dialog.Cluster.HbConfig.ConfigsNotTheSame",
         "configuration files are not the same on all hosts"},

        {"Dialog.Cluster.HbConfig.Loading",
         "loading..."},

        {"Dialog.Cluster.HbConfig.CurrentConfig",
         "current config:"},

        {"Dialog.Cluster.HbConfig.Interfaces",
         "interfaces:"},

        {"Dialog.Cluster.HbConfig.ha.cf.ok",
         "/etc/ha.d/ha.cf the same on all nodes"},

        {"Dialog.Cluster.HbConfig.Checkbox.EditConfig",
         "edit a new config"},

        {"Dialog.Cluster.HbConfig.Checkbox.SeeExisting",
         "see the existing configs"},

        {"Dialog.Cluster.Init.Title",
         "Cluster/DRBD Initialization"},

        {"Dialog.Cluster.Init.Description",
         "Cluster/DRBD Initialization. Load the DRBD and start the "
         + "Corosync(OpenAIS) or Heartbeat, if you wish at this point."},

        {"Dialog.Cluster.Init.CheckingDrbd",
         "checking..."},

        {"Dialog.Cluster.Init.LoadDrbdButton",
         "Load"},

        {"Dialog.Cluster.Init.CheckingPm",
         "checking..."},

        {"Dialog.Cluster.Init.StartCsAisButton",
         "Start"},

        {"Dialog.Cluster.Init.CsAisButtonRc",
         "Run at system start-up"},

        {"Dialog.Cluster.Init.CsAisButtonSwitch",
         "Switch to Corosync"},

        {"Dialog.Cluster.Init.CsAisIsRunning",
         " is running"},

        {"Dialog.Cluster.Init.CsAisIsRc",
         " is running at system start-up"},

        {"Dialog.Cluster.Init.CsAisIsStopped",
         " is stopped"},

        {"Dialog.Cluster.Init.CsAisIsNotInstalled",
         " is not installed"},

        {"Dialog.Cluster.Init.CsAisIsNotConfigured",
         " is not configured"},

        {"Dialog.Cluster.Init.CheckingHb",
         "checking..."},

        {"Dialog.Cluster.Init.StartHbButton",
         "Start"},

        {"Dialog.Cluster.Init.HbButtonRc",
         "Run at system start-up"},

        {"Dialog.Cluster.Init.HbButtonSwitch",
         "Switch to HB"},

        {"Dialog.Cluster.Init.HbIsRunning",
         "Heartbeat is running"},

        {"Dialog.Cluster.Init.HbIsRc",
         "Heartbeat is running at system start-up"},

        {"Dialog.Cluster.Init.HbIsStopped",
         "Heartbeat is stopped"},

        {"Dialog.Cluster.Init.HbIsNotInstalled",
         "Heartbeat is not installed"},

        {"Dialog.Cluster.Init.HbIsNotConfigured",
         "Heartbeat is not configured"},

        {"Dialog.Cluster.Init.DrbdIsLoaded",
         "DRBD is loaded"},

        {"Dialog.Cluster.Init.DrbdIsNotLoaded",
         "DRBD is not loaded"},

        {"Dialog.About.Title",
         "DRBD Management Console (Beta). Release: "},

        {"Dialog.About.Description",
         "<b>DRBD Management Console by Rasto Levrinc "
         + "(rasto@linbit.com).</b><br>"
         + "(C)opyright 2002-2009 by LINBIT HA-Solutions GmbH.<br>"
         + "Please visit the website "
         + "http://www.drbd.org/mc/management-console/<br>"
         + "Mailing list: http://lists.linbit.com/listinfo/drbd-mc<br>" },


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
+ "* tightvnc, released under the terms of the GPL License\n"
+ "* ultravnc, released under the terms of the GPL License\n"
+ "* realvnc, released under the terms of the GPL License\n"
         },


        {"Dialog.Cluster.Finish.Title",
         "Finish"},

        {"Dialog.Cluster.Finish.Description",
         "Configuration of the cluster is now complete. You can now "
         + "configure DRBD and Pacemaker services from the menu in "
         + "the cluster view."},

        {"Dialog.Cluster.Finish.Save",
         "Save"},

        {"Dialog.DrbdConfig.Resource.Title",
         "Configure DRBD Resource"},

        {"Dialog.DrbdConfig.Resource.Description",
         "Configure the new DRBD resource. Enter the <b>name</b> of the "
         + "resource. "
         + "You can call it whatever you want as long it is unique. "
         + "The same applies for DRBD device. "
         + "This <b>device</b> should be in the form /dev/drbdX. "
         + "Choose a <b>protocol</b> that the DRBD should use for replication. "
         + "You can learn more about protocols -- a.k.a replication "
         + "modes -- in <a href=\"http://www.drbd.org/docs/introduction/\">"
         + "DRBD User's Guide: Introduction to DRBD</a>. After you changed "
         + "the fields, or you are satisfied with the defaults, press "
         + "<b>Next</b> to continue."},

        {"Dialog.DrbdConfig.BlockDev.Title",
         "Configure DRBD Block Device"},

        {"Dialog.DrbdConfig.BlockDev.Description",
         "Enter the information about the DRBD block device. Choose a "
         + "DRBD net interface that will be used for DRBD communication "
         + "and a port. The port must not be used by anything else and must "
         + "not be used by another DRBD block device. The net interface "
         + "should be different than the one that is used by Pacemaker. "
         + "Choose where DRBD meta data should be written. You can choose "
         + "internal to make it simple or learn external DRBD meta disk to "
         + "make it faster."},

        {"Dialog.DrbdConfig.CreateFS.Title",
         "Initialize DRBD block devices."},

        {"Dialog.DrbdConfig.CreateFS.Description",
         "In this step you can initialize and start the DRBD cluster. "
         + "You can choose one host as a primary host. You can create a "
         + "filesystem on it, but in this case you have to choose one host "
         + "as a primary, choose the file system and press "
         + "\"Create File System\" button."},

        {"Dialog.DrbdConfig.CreateFS.NoHostString",
         "none"},

        {"Dialog.DrbdConfig.CreateFS.ChooseHost",
         "host (primary)"},

        {"Dialog.DrbdConfig.CreateFS.Filesystem",
         "file system"},

        {"Dialog.DrbdConfig.CreateFS.SelectFilesystem",
         "Use existing data"},

        {"Dialog.DrbdConfig.CreateFS.CreateFsButton",
         "Create File System"},

        {"Dialog.DrbdConfig.CreateMD.Title",
         "Create DRBD Meta-Data"},

        {"Dialog.DrbdConfig.CreateMD.Description",
         "In this step you can create new meta-data, overwrite them "
         + "or use the old ones, if they are already there."},

        {"Dialog.DrbdConfig.CreateMD.Metadata",
         "meta-data"},

        {"Dialog.DrbdConfig.CreateMD.UseExistingMetadata",
         "Use existing meta-data"},

        {"Dialog.DrbdConfig.CreateMD.CreateNewMetadata",
         "Create new meta-data"},

        {"Dialog.DrbdConfig.CreateMD.CreateNewMetadataDestroyData",
         "Create new meta-data & destroy data"},

        {"Dialog.DrbdConfig.CreateMD.OverwriteMetadata",
         "Overwrite meta-data"},

        {"Dialog.DrbdConfig.CreateMD.CreateMDButton",
         "Create Meta-Data"},

        {"Dialog.DrbdConfig.CreateMD.OverwriteMDButton",
         "Overwrite Meta-Data"},

        {"Dialog.DrbdConfig.CreateMD.CreateMD.Done",
         "Meta-data on @HOST@ have been created."  },

        {"Dialog.DrbdConfig.CreateMD.CreateMD.Failed",
         "Could not create meta-data on @HOST@."  },

        {"Dialog.DrbdConfig.CreateMD.CreateMD.Failed.40",
         "Could not create meta-data on @HOST@, because there is no room "
         + "for it. You can either destroy the file\n"
         + "system from here by choosing 'Create new meta-data & destroy data'"
         + " from the pull-down menu, resize\nthe file system manually, or "
         + "use external meta-data."},

        {"Dialog.Drbd.SplitBrain.Title",
         "Resolve DRBD Split-Brain"},

        {"Dialog.Drbd.SplitBrain.Description",
         "A split-brain condition was detected, a condition where two or "
         + "more nodes have written to the same DRBD block device without "
         + "knowing about each other. Choose the host which you think "
         + "have newer and/or more correct data. Be warned though, the "
         + "data from other host on this block device will be discarded."},

        {"Dialog.Drbd.SplitBrain.ChooseHost",
         "host"},

        {"Dialog.Drbd.SplitBrain.ResolveButton",
         "Resolve"},

        {"Dialog.HostLogs.Title",
         "Log Viewer"},

        {"Dialog.ClusterLogs.Title",
         "Log Viewer"},

        {"AppError.Title",
         "Application Error"},

        {"AppError.Text",
         "An error in application was detected. Please "
         + "send us the following info, so that we can fix it.\n"},

        {"Clusters.DefaultName",
         "Cluster "},

        {"ConfirmDialog.Title",
         "Confirmation Dialog"},

        {"ConfirmDialog.Description",
         "Are you sure?"},

        {"ConfirmDialog.Yes",
         "Yes"},

        {"ConfirmDialog.No",
         "No"},

        {"EmptyBrowser.LoadMarkedClusters",
         "Load Marked Clusters"},

        {"EmptyBrowser.LoadMarkedClusters.ToolTip",
         "Load marked clusters in the GUI."},

        {"EmptyBrowser.UnloadMarkedClusters",
         "Unload Marked Clusters"},

        {"EmptyBrowser.UnloadMarkedClusters.ToolTip",
         "Unload marked clusters in the GUI."},

        {"EmptyBrowser.RemoveMarkedClusters",
         "Remove Marked Clusters"},

        {"EmptyBrowser.RemoveMarkedClusters.ToolTip",
         "Remove marked clusters from the GUI."},

        {"EmptyBrowser.LoadClusterButton",
         "Load"},

        {"EmptyBrowser.NewHostWizard",
         "New Host Wizard"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Title",
         "Remove Marked Clusters"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Desc",
         "Are you sure you want to remove marked clusters?<br>@CLUSTERS@"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Yes",
         "Remove"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.No",
         "Cancel"},

        {"Browser.ActionsMenu",
         "Actions"},

        {"Browser.Resources",
         "Resources"},

        {"Browser.ParamDefault",
         "Default value: "},

        {"Browser.ParamType",
         "Type: "},

        {"Browser.ExpertMode",
         "Advanced Mode"},

        {"Browser.ApplyResource",
         "Apply"},

        {"ClusterBrowser.Host.Disconnected",
         "Disconnected"},

        {"ClusterBrowser.Host.Offline",
         "Offline"},

        {"ClusterBrowser.confirmRemoveAllServices.Title",
         "Remove All Services"},

        {"ClusterBrowser.confirmRemoveAllServices.Description",
         "All services and constraints will be removed. Are you sure?"},

        {"ClusterBrowser.confirmRemoveAllServices.Yes",
         "Remove"},

        {"ClusterBrowser.confirmRemoveAllServices.No",
         "Cancel"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Title",
         "Remove DRBD Resource"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Description",
         "DRBD Resource @RESOURCE@ will be removed. Are you sure?"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Yes",
         "Remove"},

        {"ClusterBrowser.confirmRemoveDrbdResource.No",
         "Cancel"},

        {"ClusterBrowser.confirmRemoveService.Title",
         "Remove Service"},

        {"ClusterBrowser.confirmRemoveService.Description",
         "Service @SERVICE@ will be removed. Are you sure?"},

        {"ClusterBrowser.confirmRemoveService.Yes",
         "Remove"},

        {"ClusterBrowser.confirmRemoveService.No",
         "Cancel"},

        {"ClusterBrowser.confirmRemoveGroup.Title",
         "Remove Group"},

        {"ClusterBrowser.confirmRemoveGroup.Description",
         "Group @GROUP@ and its services @SERVICES@ will be removed. "
         + "Are you sure?"},

        {"ClusterBrowser.confirmRemoveGroup.Yes",
         "Remove"},

        {"ClusterBrowser.confirmRemoveGroup.No",
         "Cancel"},

        {"ClusterBrowser.confirmLinbitDrbd.Title",
         "Create Linbit:DRBD Service"},

        {"ClusterBrowser.confirmLinbitDrbd.Description",
         "<b>This is not a good idea!</b><br>"
         + " Your Heartbeat @VERSION@(!) is too old for Linbit:DRBD resource"
         + " agent to work properly."
         + " You should upgrade to the Pacemaker or use the <b>drbddisk</b>"
         + " resource agent.<br>"
         + "<b>Are you sure?!<b>"},

        {"ClusterBrowser.confirmLinbitDrbd.Yes",
         "Yes (Don't click)"},

        {"ClusterBrowser.confirmLinbitDrbd.No",
         "Cancel"},

        {"ClusterBrowser.CreateDir.Title",
         "@DIR@ does not exist"},

        {"ClusterBrowser.CreateDir.Description",
         "Mount point @DIR@ does not exist on @HOST@. Create?"},

        {"ClusterBrowser.CreateDir.Yes",
         "Create"},

        {"ClusterBrowser.CreateDir.No",
         "Cancel"},

        /* Cluster Resource View */
        {"ClusterBrowser.AllHosts",
         "All Hosts"},

        {"ClusterBrowser.ClusterHosts",
         "Cluster Hosts"},

        {"ClusterBrowser.Networks",
         "Networks"},

        {"ClusterBrowser.CommonBlockDevices",
         "Shared Disks"},

        {"ClusterBrowser.Drbd",
         "Storage (DRBD)"},

        {"ClusterBrowser.Heartbeat",
         "Cluster"},

        {"ClusterBrowser.VMs",
         "VMs"},


        {"ClusterBrowser.Services",
         "Services"},

        {"ClusterBrowser.Scores",
         "Scores"},

        {"ClusterBrowser.DrbdResUnconfigured",
         "???"},

        {"ClusterBrowser.CommonBlockDevUnconfigured",
         "???"},

        {"ClusterBrowser.ClusterBlockDevice.Unconfigured",
         "unconfigured"},

        {"ClusterBrowser.Ip.Unconfigured",
         "unconfigured"},

        {"ClusterBrowser.SelectBlockDevice",
         "Select..."},

        {"ClusterBrowser.SelectFilesystem",
         "Select..."},

        {"ClusterBrowser.SelectNetInterface",
         "Select..."},

        {"ClusterBrowser.SelectMountPoint",
         "Select..."},

        {"ClusterBrowser.None",
         "None"},

        {"ClusterBrowser.HeartbeatId",
         "ID"},

        {"ClusterBrowser.HeartbeatProvider",
         "Provider"},

        {"ClusterBrowser.ResourceClass",
         "Class"},

        {"ClusterBrowser.Group",
         "Group"},

        {"ClusterBrowser.HostLocations",
         "Host Locations"},

        {"ClusterBrowser.Operations",
         "Operations"},

        {"ClusterBrowser.AdvancedOperations",
         "Other Operations"},

        {"ClusterBrowser.availableServices",
         "Available Services"},

        {"ClusterBrowser.ClStatusFailed",
         "<h2>Waiting for cluster status...</h2>"},

        {"ClusterBrowser.Hb.RemoveAllServices",
         "Remove All Services"},

        {"ClusterBrowser.Hb.RemoveService",
         "Remove Service"},

        {"ClusterBrowser.Hb.AddService",
         "Add Service"},

        {"ClusterBrowser.Hb.AddStartBefore",
         "Start Before"},

        {"ClusterBrowser.Hb.AddDependentGroup",
         "Add New Dependent Group"},

        {"ClusterBrowser.Hb.AddDependency",
         "Add New Dependent Service"},

        {"ClusterBrowser.Hb.AddGroupService",
         "Add Group Service"},

        {"ClusterBrowser.Hb.AddGroup",
         "Add Group"},

        {"ClusterBrowser.Hb.StartResource",
         "Start"},

        {"ClusterBrowser.Hb.StopResource",
         "Stop"},

        {"ClusterBrowser.Hb.MigrateResource",
         "Migrate To"},

        {"ClusterBrowser.Hb.UnmigrateResource",
         "Unmigrate"},

        {"ClusterBrowser.Hb.ViewServiceLog",
         "View Service Log"},

        {"ClusterBrowser.Hb.RemoveEdge",
         "Remove Colocation and Order"},

        {"ClusterBrowser.Hb.RemoveEdge.ToolTip",
         "Remove order and co-location dependencies."},

        {"ClusterBrowser.Hb.RemoveOrder",
         "Remove Order"},

        {"ClusterBrowser.Hb.RemoveOrder.ToolTip",
         "Remove order dependency."},

        {"ClusterBrowser.Hb.ReverseOrder",
         "Reverse Order"},

        {"ClusterBrowser.Hb.ReverseOrder.ToolTip",
         "Reverse order of the constraint."},

        {"ClusterBrowser.Hb.RemoveColocation",
         "Remove Colocation"},

        {"ClusterBrowser.Hb.RemoveColocation.ToolTip",
         "Remove co-location dependency."},

        {"ClusterBrowser.Hb.AddOrder",
         "Add Order"},

        {"ClusterBrowser.Hb.AddOrder.ToolTip",
         "Add order dependency."},

        {"ClusterBrowser.Hb.AddColocation",
         "Add Colocation"},

        {"ClusterBrowser.Hb.AddColocation.ToolTip",
         "Add co-location dependency."},

        {"ClusterBrowser.Hb.CleanUpFailedResource",
         "Restart Failed (Clean Up)"},

        {"ClusterBrowser.Hb.CleanUpResource",
         "Reset Fail-Count (Clean Up)"},

        {"ClusterBrowser.Hb.ViewLogs",
         "View Logs"},

        {"ClusterBrowser.Hb.ResGrpMoveUp",
         "Move Up"},

        {"ClusterBrowser.Hb.ResGrpMoveDown",
         "Move Down"},

        {"ClusterBrowser.Hb.ManageResource",
         "Manage by CRM"},

        {"ClusterBrowser.Hb.UnmanageResource",
         "Do not manage by CRM"},

        {"ClusterBrowser.Hb.NoInfoAvailable",
         "no info available"},

        {"ClusterBrowser.Hb.StartingFailed",
         "starting failed"},

        {"ClusterBrowser.Hb.Starting",
         "starting..."},

        {"ClusterBrowser.Hb.Stopping",
         "stopping..."},

        {"ClusterBrowser.Hb.Migrating",
         "migrating..."},

        {"ClusterBrowser.HbUpdateResources",
         "updating cluster resources..."},

        {"ClusterBrowser.HbUpdateStatus",
         "updating cluster status..."},

        {"ClusterBrowser.Drbd.RemoveEdge",
         "Remove DRBD Resource"},

        {"ClusterBrowser.Drbd.RemoveEdge.ToolTip",
         "Remove DRBD Resource"},

        {"ClusterBrowser.Drbd.ResourceConnect",
         "Connect"},

        {"ClusterBrowser.Drbd.ResourceConnect.ToolTip",
         "Connect"},

        {"ClusterBrowser.Drbd.ResourceDisconnect",
         "Disconnect"},

        {"ClusterBrowser.Drbd.ResourceDisconnect.ToolTip",
         "Disconnect"},

        {"ClusterBrowser.Drbd.ResourceResumeSync",
         "Resume Sync"},

        {"ClusterBrowser.Drbd.ResourceResumeSync.ToolTip",
         "Resume Sync"},

        {"ClusterBrowser.Drbd.ResourcePauseSync",
         "Pause Sync"},

        {"ClusterBrowser.Drbd.ResourcePauseSync.ToolTip",
         "Pause Sync"},

        {"ClusterBrowser.Drbd.ResolveSplitBrain",
         "Resolve Split-Brain"},

        {"ClusterBrowser.Drbd.ResolveSplitBrain.ToolTip",
         "Resolve Split-Brain"},

        {"ClusterBrowser.Drbd.ViewLogs",
         "View Logs"},

        {"ClusterBrowser.DrbdUpdate",
         "updating DRBD resources..."},

        {"ClusterBrowser.DifferentHbVersionsWarning",
         "<i>warning: different hb versions</i>"},

        {"ClusterBrowser.linbitDrbdMenuName",
         "Filesystem + Linbit:DRBD"},

        {"ClusterBrowser.DrbddiskMenuName",
         "Filesystem + drbddisk (obsolete)"},

        {"ClusterBrowser.StartingPtest",
         "<html><b>What happens when you apply it:</b><br>"
         + "Starting policy engine test...</html>"},

        {"ClusterBrowser.StartingDRBDtest",
         "<html><b>What happens when you apply it:</b><br>"
         + "Starting drbdadm --dry-run test...</html>"},

        {"ClusterBrowser.OperationsSameAs",
         "Same As"},


        {"PtestData.ToolTip",
         "What happens when you apply it:"},

        {"DRBDtestData.ToolTip",
         "What happens when you apply it:"},

        {"HostBrowser.HostWizard",
         "Host Wizard"},

        {"HostBrowser.Drbd.NoInfoAvailable",
         "no info available"},

        {"HostBrowser.Drbd.AddDrbdResource",
         "Add Mirrored Disk"},

        {"HostBrowser.Drbd.RemoveDrbdResource",
         "Remove DRBD Resource"},

        {"HostBrowser.Drbd.SetPrimary",
         "Promote To Primary"},

        {"HostBrowser.Drbd.SetPrimaryOtherSecondary",
         "Promote To Primary"},

        {"HostBrowser.Drbd.Attach",
         "Attach Disk"},

        {"HostBrowser.Drbd.Attach.ToolTip",
         "Attach underlying disk"},

        {"HostBrowser.Drbd.Detach",
         "Detach Disk"},

        {"HostBrowser.Drbd.Detach.ToolTip",
         "Detach underlying disk and make this DRBD device diskless"},

        {"HostBrowser.Drbd.Connect",
         "Connect To Peer"},

        {"HostBrowser.Drbd.Disconnect",
         "Disconnect From Peer"},

        {"HostBrowser.Drbd.SetSecondary",
         "Demote To Secondary"},

        {"HostBrowser.Drbd.SetSecondary.ToolTip",
         "Demote to secondary"},

        {"HostBrowser.Drbd.ForcePrimary",
         "Force Promotion To Primary"},

        {"HostBrowser.Drbd.Invalidate",
         "Invalidate"},

        {"HostBrowser.Drbd.Invalidate.ToolTip",
         "Invalidate data on this device and start full sync from other node"},

        {"HostBrowser.Drbd.DiscardData",
         "Discard Data"},

        {"HostBrowser.Drbd.DiscardData.ToolTip",
         "Discard data and start full sync from other node"},

        {"HostBrowser.Drbd.Resize",
         "Resize"},

        {"HostBrowser.Drbd.Resize.ToolTip",
         "Resize DRBD block device, when underlying<br>"
         + "block device was resized"},

        {"HostBrowser.Drbd.ResumeSync",
         "Resume Sync"},

        {"HostBrowser.Drbd.ResumeSync.ToolTip",
         "Resume sync"},

        {"HostBrowser.Drbd.PauseSync",
         "Pause Sync"},

        {"HostBrowser.Drbd.PauseSync.ToolTip",
         "Pause sync"},

        {"HostBrowser.Drbd.ViewLogs",
         "View Logs"},

        {"HostBrowser.Drbd.AttachAll",
         "Attach All Detached"},

        {"HostBrowser.Drbd.LoadDrbd",
         "Load DRBD"},

        {"HostBrowser.Drbd.UpAll",
         "Start All DRBDs"},

        {"HostBrowser.Drbd.UpgradeDrbd",
         "Upgrade DRBD"},

        {"HostBrowser.Drbd.ChangeHostColor",
         "Change Color"},

        {"HostBrowser.Drbd.ViewDrbdLog",
         "View Log File"},

        {"HostBrowser.Drbd.ConnectAll",
         "Connect All DRBDs"},

        {"HostBrowser.Drbd.DisconnectAll",
         "Disconnect All DRBDs"},

        {"HostBrowser.Drbd.SetAllPrimary",
         "Set All DRBDs Primary"},

        {"HostBrowser.Drbd.SetAllSecondary",
         "Set All DRBDs Secondary"},

        {"HostBrowser.CRM.StandByOn",
         "Go Into Standby"},

        {"HostBrowser.CRM.StandByOff",
         "Go Out of Standby"},

        {"HostBrowser.RemoveHost",
         "Remove Host"},

        /* Host Browser */
        {"HostBrowser.idDrbdNode",
         "is DRBD node"},

        {"HostBrowser.NetInterfaces",
         "Net Interfaces"},

        {"HostBrowser.BlockDevices",
         "Block Devices"},

        {"HostBrowser.FileSystems",
         "File Systems"},

        {"HostBrowser.MetaDisk.Internal",
         "Internal"},

        {"HostBrowser.DrbdNetInterface.Select",
         "Select..."},

        {"HostBrowser.Hb.NoInfoAvailable",
         "no info available"},

        {"HostBrowser.ExpertSubmenu",
         "Expert Options"},

        {"HostBrowser.MakeKernelPanic",
         "make kernel panic on "},

        {"HostBrowser.MakeKernelReboot",
         "make instant reboot on "},

        {"CRMXML.RequiredOptions",
         "Required Options"},

        {"CRMXML.MetaAttrOptions",
         "Meta Attributes"},

        {"CRMXML.OptionalOptions",
         "Advanced Options"},

        {"CRMXML.GetOCFParameters",
         "Getting OCF parameters..."},

        {"CRMXML.TargetRole.ShortDesc",
         "Target Role"},

        {"CRMXML.TargetRole.LongDesc",
         "Select whether the service should be started or should be stopped."},

        {"CRMXML.IsManaged.ShortDesc",
         "Is Managed By HB"},

        {"CRMXML.IsManaged.LongDesc",
         "Select whether the service should be managed by CRM or not."},

        {"CRMXML.ColocationSectionParams",
         "Colocation Parameters"},
        {"CRMXML.OrderSectionParams",
         "Order Parameters"},

        {"GuiComboBox.Select",
         "Select..."},

        {"GuiComboBox.NothingSelected",
         "<<nothing selected>>"},

        {"HeartbeatGraph.ColOrd",
         "col / ord"},

        {"HeartbeatGraph.Colocation",
         "colocated"},

        {"HeartbeatGraph.NoColOrd",
         "repelled / ord"},

        {"HeartbeatGraph.NoColocation",
         "repelled"},

        {"HeartbeatGraph.Order",
         "ordered"},

        {"HeartbeatGraph.Removing",
         " removing... "},

        {"HeartbeatGraph.Unconfigured",
         "unconfigured"},

        /* Score */
        {"Score.InfinityString",
         "always"},

        {"Score.MinusInfinityString",
         "never"},

        {"Score.ZeroString",
         "don't care"},

        {"Score.PlusString",
         "preferred"},

        {"Score.MinusString",
         "better not"},

        {"Score.Unknown",
         "unknown"},

        {"SSH.Enter.password",
         "'s&nbsp;<font color=red>password</font>:"},

        {"SSH.Enter.passphrase",
         "Enter&nbsp;<font color=red>passphrase</font>&nbsp;for&nbsp;key:"},

        {"SSH.Publickey.Authentication.Failed",
         "Authentication failed."},

        {"SSH.RSA.DSA.Authentication",
         "RSA/DSA Authentication"},

        {"Boolean.True",
         "True"},

        {"Boolean.False",
         "False"},

        {"Heartbeat.2.1.3.Boolean.True",
         "true"},

        {"Heartbeat.2.1.3.Boolean.False",
         "false"},

        {"Heartbeat.Boolean.True",
         "True"},

        {"Heartbeat.Boolean.False",
         "False"},

        {"Heartbeat.getClusterMetadata",
         "getting metadata"},

        {"Heartbeat.ExecutingCommand",
         "Executing CRM command..."},

        {"DrbdNetInterface",
         "DRBD net interface"},

        {"DrbdNetInterface.Long",
         "DRBD network interface"},

        {"DrbdMetaDisk",
         "DRBD meta disk"},

        {"DrbdMetaDisk.Long",
         "DRBD meta disk"},

        {"DrbdNetInterfacePort",
         "DRBD net interface port"},

        {"DrbdNetInterfacePort.Long",
         "DRBD network interface port"},

        {"DrbdMetaDiskIndex",
         "DRBD meta disk index"},

        {"DrbdMetaDiskIndex.Long",
         "DRBD meta disk index"},

        {"ProgressIndicatorPanel.Cancel",
         "Cancel"},

        {"CIB.ExecutingCommand",
         "Executing cibadmin..."},

        {"Openais.ExecutingCommand",
         "Executing OpenAIS command..."},

        {"Corosync.ExecutingCommand",
         "Executing Corosync command..."},

        {"DRBD.ExecutingCommand",
         "Executing DRBD command..."},

        {"DrbdXML.GetConfig",
         "Getting DRBD configuration..."},

        {"DrbdXML.GetParameters",
         "Getting DRBD parameters..."},
        {"Error.Title",
         "Error"},

        {"VNCXML.GetConfig",
         "Parsing libvirt config..."},

        {"EmptyViewPanel.HelpButton",
         "I am new here"},

        {"EmptyViewPanel.HideHelpButton",
         "I am a DMC expert"},

        {"EmptyViewPanel.HelpText",
"<h3>Welcome to the DRBD Management Console</h3>"
+ "by Rasto Levrinc."
+ "<p>You can start by clicking on the \"Host Wizard\" button. When you have added at least\n"
+ "two hosts, you can combine them in a new cluster with \"Cluster Wizard\" button.\n"
+ "The same way you can also add an existing cluster, configured and running or any\n"
+ "combination of the previous, just do not press the \"Create HB Config\" button\n"
+ "in the \"Heartbeat Initialization\" dialog window and you and the cluster will\n"
+ "be fine.</p>"

+ "<p><i>And of course this is a beta software, so do not forget to double check\n"
+ "everything the GUI is doing and if you find a bug file a bug report.</i></p>"

+ "<p>You can start even if you have nothing installed on the cluster hosts, just a Linux distribution of\n"
+ "course. There is a possibility to install all required components from\n"
+ "different sources with different methods during the \"host wizard\" phase\n"
+ "and have a running cluster in under a minute or two.</p>"

+ "<p>The host and cluster wizards allow you to install, configure, start and get\n"
+ "a visual overview of the DRBD, Pacemaker (Heartbeat) clusters.\n"
+ "The configuration part of Openais or Heartbeat is still very basic and\n"
+ "so you may have to check it or write\n"
+ "the config file by yourself, till it is properly implemented.</p>"

+ "<p>Thanks.</p>"
        },

    };
}
