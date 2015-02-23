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
 * along with DRBD; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.configs;

import java.util.Arrays;
import java.util.ListResourceBundle;

/**
 * Here are all Italian and common texts.
 */
public final class TextResource_it_IT extends ListResourceBundle {

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static final Object[][] contents = {
        {"DrbdMC.Title",
         "Linux Cluster Management Console"},

        /* Main Menu */
        {"MainMenu.Session",
         "Sessioni"},

        {"MainMenu.New",
         "Nuovo"},

        {"MainMenu.Load",
         "Carica"},

        {"MainMenu.Save",
         "Salva"},

        {"MainMenu.SaveAs",
         "Salva come"},

        {"MainMenu.Host",
         "Host"},

        {"MainMenu.Cluster",
         "Cluster"},

        {"MainMenu.RemoveEverything",
         "Rimuovi Tutto"},

        {"MainMenu.Exit",
         "Esci"},

        {"MainMenu.Settings",
         "Impostazioni"},

        {"MainMenu.LookAndFeel",
         "Look And Feel"},

        {"MainMenu.Help",
         "Aiuto"},

        {"MainMenu.Edit",
         "Modifica"},

        {"MainMenu.Copy",
         "Copia"},

        {"MainMenu.Paste",
         "Incolla"},

        {"MainMenu.About",
         "About"},

        {"MainMenu.BugReport",
         "Bug Report"},

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
         "verifica aggiornamenti in corso..."},

        {"MainPanel.UpgradeCheckDisabled",
         "verifica aggiornamenti disabilitata"},

        {"MainPanel.UpgradeCheckFailed",
         "verifica aggiornamenti fallita"},

        {"MainPanel.UpgradeAvailable",
         "<font color=black>new&nbsp;LCMC&nbsp;@LATEST@&nbsp;"
         + "is&nbsp;available</font>&nbsp;"
         + "<a href=\"http://lcmc.sourceforge.net/"
         + "?from-lcmc\">here</a>!"},

        {"MainPanel.NoUpgradeAvailable",
         ""},

        /** Clusters panel */
        {"ClustersPanel.ClustersTab",
         "Clusters"},

        {"ClustersPanel.ClustersTabTip",
         "Clusters"},

        /** Hosts panel */
        {"HostsPanel.NewTabTip",
         "Nuovo Host"},

        /** Tools */
        {"Tools.ExecutingCommand",
         "Executing command..."},

        {"Tools.CommandDone",
         "[fatto]"},

        {"Tools.CommandFailed",
         "[fallito]"},

        {"Tools.Loading",
         "Caricamento..."},

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
         "Aggiungi Cluster / Wizard"},

        {"ClusterTab.AddNewHost",
         "Aggiungi Host / Wizard"},

        {"MainMenu.OperatingMode.ToolTip",
         "Operating Mode"},

        /* Progress bar */
        {"ProgressBar.Cancel",
         "Annulla"},

        /* Dialogs */
        {"Dialog.Dialog.Next",
         "Avanti"},

        {"Dialog.Dialog.Back",
         "Indietro"},

        {"Dialog.Dialog.Cancel",
         "Annulla"},

        {"Dialog.Dialog.Finish",
         "Termina"},

        {"Dialog.Dialog.Retry",
         "Riprova"},

        {"Dialog.Dialog.PrintErrorAndRetry",
         "Command failed."},

        {"Dialog.Dialog.Ok",
         "Chiudi"},

        {"Dialog.Dialog.Save",
         "Salva"},

        {"Dialog.Host.NewHost.Title",
         "Host Wizard"},

        {"Dialog.Dialog.SendReport",
         "Invia Report"},

        {"Dialog.Host.NewHost.Description",
         "Specifica <b>hostname/IP</b> e <b>username</b> del server. "
         + "Come Host puoi inserire un nome host o un indirizzo IP. Inserisci"
         + " un nome host solo se risolvibile dal DNS. Il nome utente verrà "
         + "utilizzato per le connessioni e i comandi via SSH. Normalmente "
         + "deve essere l'utente <b>root</b> o un utente con accesso sudo. "
         + "<br><br>Puoi inserire una lista di host delimitata da \",\", se il"
         + " server non è raggiungibile direttamente ma attraverso una serie"
         + " di <b>hops</b>. In quest'ultimo caso devi inserire lo stesso "
         + "numero di username e nomi host/IPs che deve attraversare. "},

        {"Dialog.Host.NewHost.EnterHost",
         "Host:"},

        {"Dialog.Host.NewHost.EnterUsername",
         "Username:"},

        {"Dialog.Host.NewHost.SSHPort",
         "Porta SSH:"},

        {"Dialog.Host.NewHost.UseSudo",
         "Usa sudo:"},

        {"Dialog.Host.NewHost.EnterPassword",
         "Password:"},

        {"Dialog.Host.NewProxyHost.Title",
         "Aggiungi Proxy Host"},

        {"Dialog.Host.NewProxyHost.Description",
         "Enter the <b>hostname/IP</b> and <b>username</b> of the proxy host."},

        {"Dialog.Host.Configuration.Title",
         "Configurazione Host"},

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
         "Connessione in corso..."},

        {"Dialog.Host.SSH.Connected",
         "Connessione stabilita."},

        {"Dialog.Host.SSH.NotConnected",
         "Connection fallita."},

        {"Dialog.Host.Devices.Title",
         "Host devices"},

        {"Dialog.Host.Devices.Description",
         "Trying to retrieve information about block, network devices "
         + "and installation information of the host."},

        {"Dialog.Host.Devices.Executing",
         "Executing..."},

        {"Dialog.Host.Devices.CheckError",
         "Failed."},

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
         + "for DRBD upgrade as well if installed DRBD was detected.<br>"
         + "You can also choose a <b>Pacemaker</b> installation method. "},

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

        {"Dialog.Host.CheckInstallation.InstallMethod",
         "Installation method: "},

        {"ProxyCheckInstallation.Title",
         "DRBD Proxy Installation Check"},

        {"ProxyCheckInstallation.Description",
         "Install the DRBD proxy."},

        {"ProxyCheckInstallation.CheckingProxy",
         "checking..."},

        {"ProxyCheckInstallation.ProxyInstallButton",
         "Install"},

        {"ProxyCheckInstallation.ProxyNotInstalled",
         "not installed"},

        {"ProxyCheckInstallation.ProxyCheckForUpgradeButton",
         "Check for Upgrade"},

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

        {"Dialog.Host.ProxyInst.Title",
         "DRBD Proxy Install"},

        {"Dialog.Host.ProxyInst.Description",
         "DRBD proxy package is being installed."},

        {"Dialog.Host.ProxyInst.Executing",
         "Installing..."},

        {"Dialog.Host.ProxyInst.InstOk",
         "DRBD Proxy was successfully installed."},

        {"Dialog.Host.ProxyInst.InstError",
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
         "Salva"},

        {"Dialog.ConfigDialog.NoMatch",
         "No Match"},

        {"Dialog.ConfigDialog.SkipButton",
         "Skip this dialog"},

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
         + "tested at the moment, but being in the maintenance mode only, "
         + " it is not actively developed anymore."},

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

        {"Dialog.Cluster.CoroConfig.NextButton", "Next / Keep Old Config"},

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

        {"Dialog.Cluster.HbConfig.NextButton", "Next / Keep Old Config"},

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
         "Linux Cluster Management Console, release: "},

        {"Dialog.About.Description",
         "<b>by rasto.levrinc@gmail.com.</b><br><br>"
         + "<b>(C)opyright 2011 - 2012, Rastislav Levrinc</b><br><br>"
         + "Based on DRBD Management Console<br>"
         + "(C)opyright 2007 - 2011, Linbit HA-Solutions<br>"
         + "written by Rasto Levrinc<br><br>"
         + "Please visit the website:<br><br>"
         + "http://lcmc.sourceforge.net<br>"
         + "http://github.com/rasto/lcmc<br>" },

        {"Dialog.BugReport.Title",
         "Bug Report"},

        {"Dialog.BugReport.Description",
         "Select configs you would like to send. You can edit the text"
         + " bellow if you want. You can also send it to lcmcgui@gmail.com." },

        {"Dialog.About.Licences",
"Linux Cluster Management Console is free software; you can redistribute it and/or\n"
+ "modify it under the terms of the GNU General Public License as published\n"
+ "by the Free Software Foundation; either version 2, or (at your option)\n"
+ "any later version.\n\n"

+ "Linux Cluster Management Console is distributed in the hope that it will be useful,\n"
+ "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
+ "GNU General Public License for more details.\n\n"

+ "You should have received a copy of the GNU General Public License\n"
+ "along with LCMC; see the file COPYING.  If not, write to\n"
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

        {"Dialog.EditConfig.Title",
         "Edit "},

        {"Dialog.EditConfig.Backup.Button",
         "Make Backup"},

        {"Dialog.EditConfig.Loading",
         "loading..."},

        {"Dialog.EditConfig.NewConfig",
         "<new config>"},

        {"Dialog.EditConfig.DifferentFiles",
         "WARNING: files differ across nodes"},

        {"Dialog.Cluster.Finish.Title",
         "Finish"},

        {"Dialog.Cluster.Finish.Description",
         "Configuration of the cluster is now complete. You can now "
         + "configure DRBD and Pacemaker services from the menu in "
         + "the cluster view."},

        {"Dialog.Cluster.Finish.Save",
         "Salva"},

        {"Dialog.DrbdConfig.Start.Title",
         "Configure DRBD Volumes"},

        {"Dialog.DrbdConfig.Start.Description",
         "Choose whether a new DRBD resource should be created or add a volume"
         + " to the existing DRBD resource."},

        {"Dialog.DrbdConfig.Start.DrbdResource",
         "DRBD Resource"},

        {"Dialog.DrbdConfig.Start.NewDrbdResource",
         "New DRBD Resource"},

        {"Dialog.DrbdConfig.Resource.Title",
         "Configure DRBD Resource"},

        {"Dialog.DrbdConfig.Resource.Description",
         "Configure the new DRBD resource. Enter the <b>name</b> of the "
         + "resource. "
         + "You can call it whatever you want as long it is unique. "
         + "Choose a <b>protocol</b> that the DRBD should use for replication. "
         + "You can learn more about protocols -- a.k.a replication "
         + "modes -- in <a href=\"http://www.drbd.org/docs/introduction/\">"
         + "DRBD User's Guide: Introduction to DRBD</a>. After you changed "
         + "the fields, or you are satisfied with the defaults, press "
         + "<b>Next</b> to continue."},

        {"Dialog.DrbdConfig.Resource.ProxyHosts",
         "Proxy Hosts"},

        {"Dialog.DrbdConfig.Resource.AddHost",
         "Add Host"},

        {"Dialog.DrbdConfig.Volume.Title",
         "Configure DRBD Volume"},

        {"Dialog.DrbdConfig.Volume.Description",
         "Configure a new DRBD volume. "
         + "The <b>device</b> should be in the form /dev/drbdX. "},

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
         + "\"Create File System\" button. <b>If you skip initial full sync, "
         + "make sure that you create the filesystem there</b>, now or later."},

        {"Dialog.DrbdConfig.CreateFS.NoHostString",
         "none"},

        {"Dialog.DrbdConfig.CreateFS.ChooseHost",
         "host (primary)"},

        {"Dialog.DrbdConfig.CreateFS.Filesystem",
         "file system"},

        {"Dialog.DrbdConfig.CreateFS.SkipSync",
         "skip initial full sync"},

        {"Dialog.DrbdConfig.CreateFS.SelectFilesystem",
         "Use existing data"},

        {"Dialog.DrbdConfig.CreateFS.CreateFsButton",
         "Create File System"},

        {"Dialog.DrbdConfig.CreateFS.MakeFS",
         "Creating file system..."},

        {"Dialog.DrbdConfig.CreateFS.MakeFS.Done",
         "Filesystem was created."},

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
         "host: "},

        {"Dialog.Drbd.SplitBrain.ResolveButton",
         "Resolve"},

        {"Dialog.HostLogs.Title",
         "Log Viewer"},

        {"Dialog.ClusterLogs.Title",
         "Log Viewer"},

        {"Dialog.Logs.RefreshButton",
         "Refresh"},

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

        {"Dialog.vm.Domain.Title",
         "Create a new virtual machine"},

        {"Dialog.vm.Domain.Description",
         "In this step you can create a new virtual machine..."},

        {"Dialog.vm.InstallationDisk.Title",
         "Installation Disk"},

        {"Dialog.vm.InstallationDisk.Description",
         "Choose installation disk or image..."},

        {"Dialog.vm.Storage.Title",
         "Storage"},

        {"Dialog.vm.Storage.Description",
         "Enter a storage for this virtual machine."},

        {"Dialog.vm.Filesystem.Title",
         "Filesystem"},

        {"Dialog.vm.Filesystem.Description",
         "Enter a filesystem for this virtual machine."},

        {"Dialog.vm.Network.Title",
         "Network Interface Configuration"},

        {"Dialog.vm.Network.Description",
         "Enter a network interface for this virtual machine."},

        {"Dialog.vm.Display.Title",
         "Remote Display"},

        {"Dialog.vm.Display.Description",
         "Configure remote display."},

        {"Dialog.vm.Finish.Title",
         "Start Domain"},

        {"Dialog.vm.Finish.Description",
         "Here you can start the domain and a viewer."},

        {"EmptyBrowser.LoadMarkedClusters",
         "Connect Marked Clusters"},

        {"EmptyBrowser.LoadMarkedClusters.ToolTip",
         "Connect marked clusters in the GUI."},

        {"EmptyBrowser.UnloadMarkedClusters",
         "Disconnect Marked Clusters"},

        {"EmptyBrowser.UnloadMarkedClusters.ToolTip",
         "Disconnect marked clusters in the GUI."},

        {"EmptyBrowser.RemoveMarkedClusters",
         "Remove Marked Clusters"},

        {"EmptyBrowser.RemoveMarkedClusters.ToolTip",
         "Remove marked clusters from the GUI."},

        {"EmptyBrowser.LoadClusterButton",
         "Connetti"},

        {"EmptyBrowser.NewHostWizard",
         "Aggiungi Host / Wizard"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Title",
         "Remove Marked Clusters"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Desc",
         "Sei sicuro di voler rimuovere il cluster selezionato?<br>@CLUSTERS@"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.Yes",
         "Rimuovi"},

        {"EmptyBrowser.confirmRemoveMarkedClusters.No",
         "Annulla"},

        {"Browser.ActionsMenu",
         "Menu"},

        {"Browser.Resources",
         "Risorse"},

        {"Browser.ParamDefault",
         "Default value: "},

        {"Browser.ParamType",
         "Tipo: "},

        {"Browser.AdvancedMode",
         "Advanced"},

        {"Browser.ApplyResource",
         "Apply"},

        {"Browser.ApplyGroup",
         "Apply Grp"},

        {"Browser.CommitResources",
         "Apply All"},

        {"Browser.ApplyDRBDResource",
         "Applica"},

        {"Browser.RevertResource",
         "Ripristina"},

        {"Browser.RevertResource.ToolTip",
         "Revert unapplied changes"},

        {"ClusterBrowser.Host.Disconnected",
         "Disconnesso"},

        {"ClusterBrowser.AdvancedSubmenu",
         "More Options"},

        {"ClusterBrowser.MigrateSubmenu",
         "More Migration Options"},

        {"ClusterBrowser.FilesSubmenu",
         "Edit Config Files"},

        {"ClusterBrowser.Host.Offline",
         "Offline"},

        {"ClusterBrowser.confirmRemoveAllServices.Title",
         "Rimuovi tutti i servizi"},

        {"ClusterBrowser.confirmRemoveAllServices.Description",
         "All services and constraints will be removed. Are you sure?"},

        {"ClusterBrowser.confirmRemoveAllServices.Yes",
         "Rimuovi"},

        {"ClusterBrowser.confirmRemoveAllServices.No",
         "Annulla"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Title",
         "Remove DRBD Volume"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Description",
         "DRBD Resource @RESOURCE@ will be removed. Are you sure?"},

        {"ClusterBrowser.confirmRemoveDrbdResource.Yes",
         "Rimuovi"},

        {"ClusterBrowser.confirmRemoveDrbdResource.No",
         "Annulla"},

        {"ClusterBrowser.confirmRemoveService.Title",
         "Remove Service"},

        {"ClusterBrowser.confirmRemoveService.Description",
         "Service @SERVICE@ will be removed. Are you sure?"},

        {"ClusterBrowser.confirmRemoveService.Yes",
         "Rimuovi"},

        {"ClusterBrowser.confirmRemoveService.No",
         "Annulla"},

        {"ClusterBrowser.confirmRemoveGroup.Title",
         "Rimuovi Gruppo"},

        {"ClusterBrowser.confirmRemoveGroup.Description",
         "Group @GROUP@ and its services @SERVICES@ will be removed. "
         + "Are you sure?"},

        {"ClusterBrowser.confirmRemoveGroup.Yes",
         "Rimuovi"},

        {"ClusterBrowser.confirmRemoveGroup.No",
         "Annulla"},

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
         "Annulla"},

        {"ClusterBrowser.confirmHbDrbd.Title",
         "Create Heartbeat:DRBD Service"},

        {"ClusterBrowser.confirmHbDrbd.Description",
         "<b>This is not a good idea!</b><br>"
         + " You should use Linbit:DRBD resource agent instead.<br>"
         + "<b>Are you sure?!<b>"},

        {"ClusterBrowser.confirmHbDrbd.Yes",
         "Yes (Don't click)"},

        {"ClusterBrowser.confirmHbDrbd.No",
         "Annulla"},

        {"ClusterBrowser.CreateDir.Title",
         "@DIR@ does not exist"},

        {"ClusterBrowser.CreateDir.Description",
         "Mount point @DIR@ does not exist on @HOST@. Create?"},

        {"ClusterBrowser.CreateDir.Yes",
         "Crea"},

        {"ClusterBrowser.CreateDir.No",
         "Non creare"},

        {"ClusterBrowser.UpdatingServerInfo",
         "updating server info..."},

        {"ClusterBrowser.UpdatingVMsStatus",
         "updating VMs status..."},

        {"ClusterBrowser.UpdatingDrbdStatus",
         "updating drbd status..."},

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
         "Storage (DRBD, LVM)"},

        {"ClusterBrowser.ClusterManager",
         "CRM (Pacemaker)"},

        {"ClusterBrowser.VMs",
         "VMs (KVM, Xen)"},


        {"ClusterBrowser.Services",
         "Servizi"},

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
         "Seleziona..."},

        {"ClusterBrowser.SelectFilesystem",
         "Seleziona..."},

        {"ClusterBrowser.SelectNetInterface",
         "Seleziona..."},

        {"ClusterBrowser.SelectMountPoint",
         "Seleziona..."},

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
         "<h2>Waiting for Pacemaker...</h2>"},

        {"ClusterBrowser.Hb.RemoveAllServices",
         "Remove All Services"},

        {"ClusterBrowser.Hb.StopAllServices",
         "Stop All Services"},

        {"ClusterBrowser.Hb.UnmigrateAllServices",
         "Remove All Migration Constraints"},

        {"ClusterBrowser.Hb.RemoveService",
         "Rimuovi Servizio"},

        {"ClusterBrowser.Hb.AddService",
         "Aggiungi Servizio"},

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

        {"ClusterBrowser.Hb.UpResource",
         "Move Up The Group"},

        {"ClusterBrowser.Hb.DownResource",
         "Move Down The Group"},

        {"ClusterBrowser.Hb.MigrateResource",
         "Migrate To"},

        {"ClusterBrowser.Hb.ForceMigrateResource",
         "Force Migrate To"},

        {"ClusterBrowser.Hb.MigrateFromResource",
         "Migrate FROM"},


        {"ClusterBrowser.Hb.UnmigrateResource",
         "Remove Migration Constraint"},

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
         "Mostra Logs"},

        {"ClusterBrowser.Hb.ResGrpMoveUp",
         "Move Up"},

        {"ClusterBrowser.Hb.ResGrpMoveDown",
         "Move Down"},

        {"ClusterBrowser.Hb.ManageResource",
         "Manage by CRM"},

        {"ClusterBrowser.Hb.UnmanageResource",
         "Do not manage by CRM"},

        {"ClusterBrowser.Hb.NoInfoAvailable",
         "unknown..."},

        {"ClusterBrowser.Hb.GroupStopped",
         "stopped (group)"},

        {"ClusterBrowser.Hb.StartingFailed",
         "starting failed"},

        {"ClusterBrowser.Hb.Starting",
         "starting..."},

        {"ClusterBrowser.Hb.Stopping",
         "stopping..."},

        {"ClusterBrowser.Hb.Enslaving",
         "enslaving..."},

        {"ClusterBrowser.Hb.Migrating",
         "migrating..."},

        {"ClusterBrowser.Hb.ColOnlySubmenu",
         "Colocation Only"},

        {"ClusterBrowser.Hb.OrdOnlySubmenu",
         "Order Only"},

        {"ClusterBrowser.Hb.ClusterWizard",
         "Cluster Wizard"},

        {"ClusterBrowser.HbUpdateResources",
         "updating resource agents..."},

        {"ClusterBrowser.HbUpdateStatus",
         "updating Pacemaker status..."},

        {"ClusterBrowser.Drbd.RemoveEdge",
         "Remove DRBD Volume"},

        {"ClusterBrowser.Drbd.RemoveEdge.ToolTip",
         "Remove DRBD Volume"},

        {"ClusterBrowser.Drbd.ResourceConnect",
         "Connetti"},

        {"ClusterBrowser.Drbd.ResourceConnect.ToolTip",
         "Connetti"},

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

        {"ClusterBrowser.Drbd.Verify",
         "Online Verification"},

        {"ClusterBrowser.Drbd.Verify.ToolTip",
         "Starts online verification."},

        {"ClusterBrowser.Drbd.ViewLogs",
         "Mostra Logs"},

        {"ClusterBrowser.DrbdUpdate",
         "updating DRBD resources..."},

        {"ClusterBrowser.DifferentHbVersionsWarning",
         "<i>warning: different hb versions</i>"},

        {"ClusterBrowser.linbitDrbdMenuName",
         "Filesystem + Linbit:DRBD"},

        {"ClusterBrowser.DrbddiskMenuName",
         "Filesystem + drbddisk (obsolete)"},

        {"ClusterBrowser.StartingPtest",
         "<html><b>Look what happens when you apply it:</b><br>"
         + "Starting policy engine test...</html>"},

        {"ClusterBrowser.StartingDRBDtest",
         "<html><b>Look what happens when you apply it:</b><br>"
         + "Starting drbdadm --dry-run test...</html>"},

        {"ClusterBrowser.SameAs",
         "Same As"},

        {"ClusterBrowser.AddServiceToCluster",
         "Add Service To Cluster"},

        {"ClusterBrowser.RAsOverviewButton",
         "RAs Overview"},

        {"ClusterBrowser.ClassesOverviewButton",
         "Classes Overview"},

        {"ClusterBrowser.ClusterStatusFailed",
         "Pacemaker status not available"},

        {"ServicesInfo.AddConstraintPlaceholderAnd",
          "Add Constraint Placeholder (AND)"},

        {"ServicesInfo.AddConstraintPlaceholderOr",
          "Add Constraint Placeholder (OR)"},

        {"ServicesInfo.AddConstraintPlaceholderAnd.ToolTip",
          "Add constraint placeholder to create a resource set."},

        {"ServicesInfo.AddConstraintPlaceholderOr.ToolTip",
          "Add constraint placeholder to create a resource set."},

        {"PtestData.ToolTip",
         "Look what happens when you apply it:"},

        {"PtestData.NoToolTip",
         "no actions"},

        {"DRBDtestData.ToolTip",
         "Look what happens when you apply it:"},

        {"DRBDtestData.NoToolTip",
         "no actions"},

        {"HostBrowser.HostWizard",
         "Host Wizard"},

        {"HostBrowser.ProxyHostWizard",
         "Proxy Host Wizard"},

        {"HostBrowser.Drbd.NoInfoAvailable",
         "unknown..."},

        {"HostBrowser.Drbd.AddDrbdResource",
         "Add Mirrored Disk"},

        {"HostBrowser.Drbd.RemoveDrbdResource",
         "Remove DRBD Volume"},

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
         "Connect Resource To Peer"},

        {"HostBrowser.Drbd.Disconnect",
         "Disconnect Resource From Peer"},

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
         "Mostra Logs"},

        {"HostBrowser.Drbd.AttachAll",
         "Attach All"},

        {"HostBrowser.Drbd.DetachAll",
         "Detach All"},

        {"HostBrowser.Drbd.LoadDrbd",
         "Load DRBD Module"},

        {"HostBrowser.Drbd.AdjustAllDrbd",
         "Load DRBD Config (Adjust)"},

        {"HostBrowser.Drbd.AdjustAllDrbd.ToolTip",
         "Load DRBD Config into the DRBD Module (drbdadm adjust all)"},

        {"HostBrowser.Drbd.UpAll",
         "Start All DRBDs (up)"},

        {"HostBrowser.Drbd.UpgradeDrbd",
         "Upgrade DRBD"},

        {"HostBrowser.Drbd.ChangeHostColor",
         "Imposta colore"},

        {"HostBrowser.Drbd.ViewDrbdLog",
         "Mostra File Log"},

        {"HostBrowser.Drbd.ConnectAll",
         "Connect All DRBDs"},

        {"HostBrowser.Drbd.DisconnectAll",
         "Disconnect All DRBDs"},

        {"HostBrowser.Drbd.SetAllPrimary",
         "Set All DRBDs Primary"},

        {"HostBrowser.Drbd.SetAllSecondary",
         "Set All DRBDs Secondary"},

        {"HostDrbdInfo.Drbd.StartProxy",
         "Start Proxy Daemon"},

        {"HostDrbdInfo.Drbd.StopProxy",
         "Stop Proxy Daemon"},

        {"HostDrbdInfo.Connect",
         "Connetti"},

        {"HostDrbdInfo.Disconnect",
         "Disconnetti"},

        {"HostDrbdInfo.Drbd.AllProxyUp",
         "Start All Proxy Connections"},

        {"HostDrbdInfo.Drbd.AllProxyDown",
         "Stop All Proxy Connections"},

        {"ProxyHostInfo.NameInfo",
         "proxy: "},

        {"ProxyHostInfo.NotConnectable",
         "Missing connection data. Run the host wizard."},

        {"BlockDevInfo.Drbd.ProxyUp",
         "Start Proxy Connection"},

        {"BlockDevInfo.Drbd.ProxyDown",
         "Stop Proxy Connection"},

        {"BlockDevInfo.PVRemove.Failed",
         "Removing of PV:{} failed."},

        {"BlockDevInfo.PVCreate.Failed",
         "Creating of PV:{} failed."},

        {"HostInfo.CRM.AllMigrateFrom",
         "Migrate All Resources Away"},

        {"HostInfo.StopCorosync",
         "Stop Corosync"},

        {"HostInfo.StopOpenais",
         "Stop Openais"},

        {"HostInfo.StopHeartbeat",
         "Stop Heartbeat"},

        {"HostInfo.StartCorosync",
         "Start Corosync"},

        {"HostInfo.StartPacemaker",
         "Start Pacemakerd"},

        {"HostInfo.StartOpenais",
         "Start Openais"},

        {"HostInfo.StartHeartbeat",
         "Start Heartbeat"},

        {"HostInfo.confirmCorosyncStop.Title",
         "Stop Corosync"},

        {"HostInfo.confirmCorosyncStop.Desc",
         "Are you sure you want to stop the corosync?"},

        {"HostInfo.confirmCorosyncStop.Yes",
         "Stop"},

        {"HostInfo.confirmCorosyncStop.No",
         "Annulla"},

        {"HostInfo.confirmHeartbeatStop.Title",
         "Stop Heartbeat"},

        {"HostInfo.confirmHeartbeatStop.Desc",
         "Sei sicuro di voler fermare Heartbeat?"},

        {"HostInfo.confirmHeartbeatStop.Yes",
         "Stop"},

        {"HostInfo.confirmHeartbeatStop.No",
         "Annulla"},

        {"HostInfo.crmShellInfo",
         "experimental remote CRM shell"},

        {"HostInfo.crmShellCommitButton",
         "commit"},

        {"HostInfo.crmShellStatusButton",
         "<html>node info<br>(refresh)</html>"},

        {"HostInfo.crmShellShowButton",
         "<html>crm shell<br>(config)</html>"},

        {"HostInfo.crmVerifyBtn",
         "<html>crm<br>verify</html>"},

        {"HostInfo.coroMembersBtn",
         "<html>cluster<br>members</html>"},

        {"HostInfo.crmShellLoading",
         "loading..."},

        {"HostBrowser.CRM.StandByOn",
         "Standby / Switchover"},

        {"HostBrowser.CRM.StandByOff",
         "Vai Online"},

        {"HostBrowser.RemoveHost",
         "Rimuovi Host"},

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
         "Seleziona..."},

        {"HostBrowser.Hb.NoInfoAvailable",
         "unknown..."},

        {"HostDrbdInfo.LVMMenu",
         "LVM"},

        {"HostDrbdInfo.AddToVG",
         "crea LV in VG "},

        {"HostBrowser.AdvancedSubmenu",
         "More Options"},

        {"HostBrowser.MakeKernelPanic",
         "make kernel panic on "},

        {"HostBrowser.MakeKernelReboot",
         "make instant reboot on "},

        {"HostBrowser.CmdLog",
         "Commnad Log Dialog"},

        {"CRMXML.GlobalRequiredOptions",
         "Impostazioni Globali Obbligatorie"},

        {"CRMXML.GlobalOptionalOptions",
         "Impostazioni Globali"},

        {"CRMXML.RscDefaultsSection",
         "Global Resource Defaults"},

        {"CRMXML.RequiredOptions",
         "Impostazioni obbligatorie"},

        {"CRMXML.MetaAttrOptions",
         "Meta Attributes"},

        {"CRMXML.OptionalOptions",
         "Impostazioni avanzate"},

        {"CRMXML.OtherOptions",
         "Altre impostazioni"},

        {"CRMXML.GetRAMetaData",
         "Loading resource agents..."},

        {"CRMXML.GetRAMetaData.Done",
         "Resource agent meta data loaded."},

        {"CRMXML.TargetRole.ShortDesc",
         "Target Role"},

        {"CRMXML.TargetRole.LongDesc",
         "Select whether the service should be started, stopped or promoted."},

        {"CRMXML.IsManaged.ShortDesc",
         "Is Managed By Cluster"},

        {"CRMXML.IsManaged.LongDesc",
         "Select whether the service should be managed by cluster."},

        {"CRMXML.AllowMigrate.ShortDesc",
         "Allow Migrate"},

        {"CRMXML.AllowMigrate.LongDesc",
         "Set this if you want live migration."},

        {"CRMXML.Priority.ShortDesc",
         "Priority"},

        {"CRMXML.Priority.LongDesc",
         "Priority to stay active."},

        {"CRMXML.ResourceStickiness.ShortDesc",
         "Resource Stickiness"},

        {"CRMXML.ResourceStickiness.LongDesc",
         "Score, how much the resource should stay where it is."},

        {"CRMXML.MigrationThreshold.ShortDesc",
         "Migration Threshold"},

        {"CRMXML.MigrationThreshold.LongDesc",
         "Migrate after migration-threshold failures."},

        {"CRMXML.FailureTimeout.ShortDesc",
         "Failure Timeout"},

        {"CRMXML.FailureTimeout.LongDesc",
         "How many seconds to wait to ignore failure."},

        {"CRMXML.MultipleActive.ShortDesc",
         "Multiple Active"},

        {"CRMXML.MultipleActive.LongDesc",
         "What to do if resource is wrongfully active on more than one node."},

        {"CRMXML.ColocationSectionParams",
         "Colocation Parameters"},
        {"CRMXML.OrderSectionParams",
         "Order Parameters"},

        {"CRMXML.stonith-timeout.ShortDesc",
         "Stonith Timeout"},

        {"CRMXML.stonith-timeout.LongDesc",
         "How long to wait for the STONITH action to complete."},

        {"CRMXML.stonith-priority.ShortDesc",
         "Stonith Priority"},

        {"CRMXML.stonith-priority.LongDesc",
         "The priority of the stonith resource. The lower the number, the<br>"
         + "higher the priority."},

        {"CRMXML.pcmk_host_check.ShortDesc",
         "PCMK Host Check"},

        {"CRMXML.pcmk_host_check.LongDesc",
         "How to determin which machines are controlled by the device.<br>"
         + "<br>"
         + "Allowed values: dynamic-list (query the device), static-list (check<br>"
         + "the pcmk_host_list attribute), none (assume every device can fence<br>"
         + "every machine)"},

        {"CRMXML.pcmk_host_list.ShortDesc",
         "PCMK Host List"},

        {"CRMXML.pcmk_host_list.LongDesc",
         "A list of machines controlled by this device (Optional unless<br>"
         + "pcmk_host_check=static-list)."},

        {"CRMXML.pcmk_host_map.ShortDesc",
         "PCMK Host Map"},

        {"CRMXML.pcmk_host_map.LongDesc",
         "A mapping of host names to ports numbers for devices that do not<br>"
         + "support host names.<br>"
         + "<br>"
         + "Eg. node1:1;node2:2,3 would tell the cluster to use port 1 for<br>"
         + "node1 and ports 2 and 3 for node2<br>"},

        {"Widget.Select",
         "Seleziona..."},

        {"Widget.NothingSelected",
         "<<nothing selected>>"},

        {"CRMGraph.ColOrd",
         "col / ord"},

        {"CRMGraph.Colocation",
         "colocated"},

        {"CRMGraph.NoColOrd",
         "repelled / ord"},

        {"CRMGraph.NoColocation",
         "repelled"},

        {"CRMGraph.Order",
         "ordered"},

        {"CRMGraph.Removing",
         " removing... "},

        {"CRMGraph.Unconfigured",
         "unconfigured"},

        {"CRMGraph.Simulate",
         "simulate..."},

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

        {"SSH.Enter.passphrase2",
         "(or press &lt;enter&gt; for password authentication)"},

        {"SSH.Enter.sudoPassword",
         "&nbsp;<font color=red>sudo</font>&nbsp;password:"},

        {"SSH.Publickey.Authentication.Failed",
         "Authentication failed."},

        {"SSH.KeyboardInteractive.DoesNotWork",
         "Keyboard-interactive does not work."},

        {"SSH.KeyboardInteractive.Failed",
         "Keyboard-interactive auth failed."},

        {"SSH.Password.Authentication.Failed",
         "Password authentication failed."},

        {"SSH.RSA.DSA.Authentication",
         "RSA/DSA Authentication"},

        {"SSH.PasswordAuthentication",
         "Password Authentication"},

        {"SSH.SudoAuthentication",
         "Sudo Authentication"},

        {"Heartbeat.getClusterMetadata",
         "getting metadata"},

        {"Heartbeat.ExecutingCommand",
         "Executing CRM command..."},

        {"DrbdNetInterface",
         "DRBD net interface"},

        {"DrbdNetInterface.Long",
         "DRBD network interface"},

        {"DrbdMetaDisk",
         "Meta Disk"},

        {"DrbdMetaDisk.Long",
         "DRBD meta disk"},

        {"DrbdNetInterfacePort",
         "Port"},

        {"DrbdNetInterfacePort.Long",
         "DRBD network interface port"},

        {"DrbdMetaDiskIndex",
         "DRBD meta disk index"},

        {"DrbdMetaDiskIndex.Long",
         "DRBD meta disk index"},

        {"ProgressIndicatorPanel.Cancel",
         "Annulla"},

        {"CIB.ExecutingCommand",
         "Executing CRM command..."},

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

        {"LVM.ExecutingCommand",
         "Executing LVM command..."},

        {"VIRSH.ExecutingCommand",
         "Executing virsh command..."},

        {"VMSXML.GetConfig",
         "Parsing libvirt config..."},

        {"VMSInfo.AddNewDomain",
         "Add New Virtual Machine"},

        {"VMSVirtualDomainInfo.Section.VirtualSystem",
         "Virtual System"},

        {"VMSVirtualDomainInfo.Section.Options",
         "Advanced Options"},

        {"VMSVirtualDomainInfo.Section.Features",
         "Features"},

        {"VMSVirtualDomainInfo.Section.CPUMatch",
         "CPU Match"},

        {"VMSVirtualDomainInfo.Short.Name",
         "Domain name"},

        {"VMSVirtualDomainInfo.Short.DomainType",
         "Domain Type"},

        {"VMSVirtualDomainInfo.Short.Vcpu",
         "Number of CPUs"},

        {"VMSVirtualDomainInfo.Short.Bootloader",
         "Bootloader"},

        {"VMSVirtualDomainInfo.Short.CurrentMemory",
         "Current Memory"},

        {"VMSVirtualDomainInfo.Short.Memory",
         "Max Memory"},

        {"VMSVirtualDomainInfo.Short.Os.Boot",
         "Boot Device"},

        {"VMSVirtualDomainInfo.Short.Os.Boot.2",
         "2nd Boot Device"},

        {"VMSVirtualDomainInfo.Short.Os.Loader",
         "Loader"},

        {"VMSVirtualDomainInfo.Short.VirshOptions",
         "Virsh options"},

        {"VMSVirtualDomainInfo.Short.Autostart",
         "Autostart"},

        {"VMSVirtualDomainInfo.Short.Arch",
         "Architecture"},

        {"VMSVirtualDomainInfo.Short.Machine",
         "Machine"},

        {"VMSVirtualDomainInfo.Short.Type",
         "Type"},

        {"VMSVirtualDomainInfo.Short.Init",
         "Init"},


        {"VMSVirtualDomainInfo.Short.Acpi",
         "ACPI"},

        {"VMSVirtualDomainInfo.Short.Apic",
         "APIC"},

        {"VMSVirtualDomainInfo.Short.Pae",
         "PAE"},

        {"VMSVirtualDomainInfo.Short.Hap",
         "HAP"},

        {"VMSVirtualDomainInfo.Short.Clock.Offset",
         "Clock Offset"},

        {"VMSVirtualDomainInfo.Short.CPU.Match",
         "Match"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.Model",
         "Model"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.Vendor",
         "Vendor"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.TopologySockets",
         "Topology Sockets"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.TopologyCores",
         "Topology Cores"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.TopologyThreads",
         "Topology Threads"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.Policy",
         "Policy"},

        {"VMSVirtualDomainInfo.Short.CPUMatch.Features",
         "Features"},

        {"VMSVirtualDomainInfo.Short.OnPoweroff",
         "On Poweroff"},

        {"VMSVirtualDomainInfo.Short.OnReboot",
         "On Reboot"},

        {"VMSVirtualDomainInfo.Short.OnCrash",
         "On Crash"},

        {"VMSVirtualDomainInfo.Short.Emulator",
         "Emulator"},

        {"VMSVirtualDomainInfo.StartVNCViewerOn",
         "Console (@VIEWER@ VNC) on "},

        {"VMSVirtualDomainInfo.StartOn",
         "Start on "},

        {"VMSVirtualDomainInfo.ShutdownOn",
         "Gracefully shutdown on "},

        {"VMSVirtualDomainInfo.DestroyOn",
         "Kill on "},

        {"VMSVirtualDomainInfo.RebootOn",
         "Reboot on "},

        {"VMSVirtualDomainInfo.SuspendOn",
         "Suspend on "},

        {"VMSVirtualDomainInfo.ResumeOn",
         "Resume on "},

        {"VMSVirtualDomainInfo.AddNewDisk",
         "New Disk"},

        {"VMSVirtualDomainInfo.AddNewFilesystem",
         "New Filesystem"},

        {"VMSVirtualDomainInfo.AddNewInterface",
         "New Network Interface"},

        {"VMSVirtualDomainInfo.AddNewInputDev",
         "New Input Device (Mouse/Tablet)"},

        {"VMSVirtualDomainInfo.AddNewGraphics",
         "New Graphics Device (VNC, SDL)"},

        {"VMSVirtualDomainInfo.AddNewSound",
         "New Sound Device"},

        {"VMSVirtualDomainInfo.AddNewSerial",
         "New Serial Device"},

        {"VMSVirtualDomainInfo.AddNewParallel",
         "New Parallel Device"},

        {"VMSVirtualDomainInfo.AddNewVideo",
         "New Video Device"},

        {"VMSVirtualDomainInfo.AddNewHardware",
         "Add Hardware"},

        {"VMSVirtualDomainInfo.MoreOptions",
         "Resume/Suspend"},

        {"VMSVirtualDomainInfo.RemoveDomain",
         "Remove Domain"},

        {"VMSVirtualDomainInfo.CancelDomain",
         "Cancel New Domain"},

        {"VMSVirtualDomainInfo.confirmRemove.Title",
         "Remove Virtual Domain"},

        {"VMSVirtualDomainInfo.confirmRemove.Description",
         "Virtual domain \"@DOMAIN@\" will be removed. "
         + "Are you sure?"},

        {"VMSVirtualDomainInfo.confirmRemove.Yes",
         "Rimuovi"},

        {"VMSVirtualDomainInfo.confirmRemove.No",
         "Annulla"},

        {"VMSVirtualDomainInfo.AvailableInVersion",
         "Available in libvirt version @VERSION@"},

        {"ConstraintPHInfo.ToolTip",
         "Resource set placeholder"},

        {"ConstraintPHInfo.Remove",
         "Rimuovi"},

        {"ConstraintPHInfo.confirmRemove.Title",
         "Remove Constraint Placeholder"},

        {"ConstraintPHInfo.confirmRemove.Description",
         "This placeholder with all constraints will be removed. "
         + "Are you sure?"},

        {"ConstraintPHInfo.confirmRemove.Yes",
         "Rimuovi"},

        {"ConstraintPHInfo.confirmRemove.No",
         "Annulla"},

        {"ConstraintPHInfo.And",
         "AND"},

        {"ConstraintPHInfo.Or",
         "OR"},

        {"Application.OpMode.RO",
         "Read-Only"},

        {"Application.OpMode.OP",
         "Operator"},

        {"Application.OpMode.ADMIN",
         "Administrator"},

        {"Application.OpMode.GOD",
         "Developer Level 10"},

        {"EditableInfo.MoreOptions",
         "more options are available in advanced mode..."},

        {"VMSDiskInfo.FileChooserTitle",
         "Select image on "},

        {"VMSDiskInfo.Approve",
         "Select"},

        {"VMSDiskInfo.Approve.ToolTip",
         "Select this image."},

        {"VMSDiskInfo.Section.DiskOptions",
         "Disk Options"},

        {"VMSDiskInfo.Section.Source",
         "Source"},

        {"VMSDiskInfo.Section.Authentication",
         "Authentication"},

        {"VMSDiskInfo.Param.Type",
         "Type"},

        {"VMSDiskInfo.Param.TargetDevice",
         "Target Device"},

        {"VMSDiskInfo.Param.SourceFile",
         "File"},

        {"VMSDiskInfo.Param.SourceDevice",
         "Device"},

        {"VMSDiskInfo.Param.SourceProtocol",
         "Protocol"},

        {"VMSDiskInfo.Param.SourceName",
         "Name"},

        {"VMSDiskInfo.Param.SourceHostName",
         "Host Names"},

        {"VMSDiskInfo.Param.SourceHostName.ToolTip",
         "<br>Enter multiple hosts delimited by comma."},

        {"VMSDiskInfo.Param.SourceHostPort",
         "Host Ports"},

        {"VMSDiskInfo.Param.SourceHostPort.ToolTip",
         "<br>Enter multiple ports delimited by comma."},

        {"VMSDiskInfo.Param.AuthUsername",
         "Username"},

        {"VMSDiskInfo.Param.AuthSecretType",
         "Secret Type"},

        {"VMSDiskInfo.Param.AuthSecretUuid",
         "Secred UUID"},

        {"VMSDiskInfo.Param.TargetBusType",
         "Disk Type"},

        {"VMSDiskInfo.Param.DriverName",
         "Driver Name"},

        {"VMSDiskInfo.Param.DriverType",
         "Driver Type"},

        {"VMSDiskInfo.Param.DriverCache",
         "Driver Cache"},

        {"VMSDiskInfo.Param.Readonly",
         "Readonly"},

        {"VMSDiskInfo.Param.Shareable",
         "Shareable"},

        {"VMSVideoInfo.ModelType",
         "Type"},

        {"VMSVideoInfo.ModelVRAM",
         "Video Memory (Kb)"},

        {"VMSVideoInfo.ModelHeads",
         "Number of Screens"},

        {"VMSVideoInfo.ModelVRAM.ToolTip",
         "Video Memory in kilobytes (VRAM)"},

        {"VMSVideoInfo.ModelHeads.ToolTip",
         "Number of Screens (Heads)"},

        {"VMSHardwareInfo.Menu.Remove",
         "Rimuovi"},

        {"VMSHardwareInfo.Menu.Cancel",
         "Annulla"},

        {"VMSHardwareInfo.confirmRemove.Title",
         "Remove Virtual Hardware"},

        {"VMSHardwareInfo.confirmRemove.Description",
         "Virtual \"@HW@\" will be removed. "
         + "Are you sure?"},

        {"VMSHardwareInfo.confirmRemove.Yes",
         "Rimuovi"},

        {"VMSHardwareInfo.confirmRemove.No",
         "Annulla"},

        {"ServiceInfo.PingdToolTip",
         "<html><b>pingd</b><br>"
         + "Set location according the connectivity. You have<br>"
         + "to set a ping/pingd resource, for this to work.</html>"},

        {"ServiceInfo.Filesystem.RunningOn",
         "running on"},

        {"ServiceInfo.Filesystem.NotRunning",
         "not running"},

        {"ServiceInfo.Filesystem.MoutedOn",
         "mounted on"},

        {"ServiceInfo.Filesystem.NotMounted",
         "not mounted"},

        {"ServiceInfo.NotRunningAnywhere",
         "it is not running anywhere"},

        {"ServiceInfo.AlreadyRunningOnNode",
         "already running on this node"},

        {"ServiceInfo.AlreadyStarted",
         "it is already started"},

        {"ServiceInfo.AlreadyStopped",
         "it is already stopped"},

        {"ServiceInfo.LoadingMetaData",
         "Loading meta data..."},

        {"DrbdInfo.CommonSection",
         "Common "},

        {"DrbdInfo.AddProxyHost",
         "Add Proxy Host"},

        {"DrbdInfo.RescanLvm",
         "Rescan LVM"},

        {"DrbdResourceInfo.HostAddresses",
         "Net Interface"},

        {"DrbdResourceInfo.AddressOnHost",
         "on "},

        {"DrbdResourceInfo.NetInterfacePort",
         "Port"},

        {"DrbdResourceInfo.NetInterfacePortToProxy",
         "<html><u>DRBD</u> \u2192 Proxy Port</html>"},

        {"DrbdResourceInfo.ProxyPorts",
         "Common Proxy Ports"},
        {"DrbdResourceInfo.Proxy",
         "Proxy on "},

        {"DrbdResourceInfo.ProxyInsideIp",
         "<html>DRBD \u2192 <u>Proxy</u> IP</html>"},

        {"DrbdResourceInfo.ProxyInsidePort",
         "<html>DRBD \u2192 <u>Proxy</u> Port</html>"},

        {"DrbdResourceInfo.ProxyOutsideIp",
         "<html><u>Proxy</u> \u2192 Proxy IP</html>"},

        {"DrbdResourceInfo.ProxyOutsidePort",
         "<html><u>Proxy</u> \u2192 Proxy Port</html>"},

        {"DrbdResourceInfo.ProxyInsideIp.ToolTip",
         "Proxy Inside IP"},

        {"DrbdResourceInfo.ProxyInsidePort.ToolTip",
         "Proxy Inside Port"},

        {"DrbdResourceInfo.ProxyOutsideIp.ToolTip",
         "Proxy Outside IP"},

        {"DrbdResourceInfo.ProxyOutsidePort.ToolTip",
         "Proxy Outside Port"},

        {"BlockDevice.MetaDiskSection",
         "DRBD Meta Disk"},

        {"DrbdVolumeInfo.VolumeSection",
         "DRBD Volume"},

        {"DrbdVolumeInfo.Number",
         "Number"},

        {"DrbdVolumeInfo.Device",
         "Device"},

        {"PcmkMultiSelectionInfo.Selection",
         "<h3>Selection:</h3>"},

        {"PcmkMultiSelectionInfo.StopSelectedResources",
         "Stop Selected Services"},

        {"PcmkMultiSelectionInfo.StartSelectedResources",
         "Start Selected Services"},

        {"PcmkMultiSelectionInfo.StandByOn",
         "Standby Selected Hosts"},

        {"PcmkMultiSelectionInfo.StandByOff",
         "Online Selected Hosts"},

        {"PcmkMultiSelectionInfo.StopCorosync",
         "Stop Corosync on Selected Hosts"},

        {"PcmkMultiSelectionInfo.StopOpenais",
         "Stop Openais on Selected Hosts"},

        {"PcmkMultiSelectionInfo.StopHeartbeat",
         "Stop Heartbeat on Selected Hosts"},

        {"PcmkMultiSelectionInfo.StartCorosync",
         "Start Corosync on Selected Hosts"},

        {"PcmkMultiSelectionInfo.StartPacemaker",
         "Start Pacemakerd on Selected Hosts"},

        {"PcmkMultiSelectionInfo.StartOpenais",
         "Start Openais on Selected Hosts"},

        {"PcmkMultiSelectionInfo.StartHeartbeat",
         "Start Heartbeat on Selected Hosts"},

        {"PcmkMultiSelectionInfo.ChangeHostColor",
         "Change Color of Selected Hosts"},

        {"PcmkMultiSelectionInfo.confirmRemove.Title",
         "Remove Selected Services"},

        {"PcmkMultiSelectionInfo.confirmRemove.Desc",
         "Are you sure you want to remove the selected services?"},

        {"PcmkMultiSelectionInfo.confirmRemove.Yes",
         "Rimuovi"},

        {"PcmkMultiSelectionInfo.confirmRemove.No",
         "Annulla"},

        {"PcmkMultiSelectionInfo.CleanUpFailedResource",
         "Restart Failed (Clean Up)"},

        {"PcmkMultiSelectionInfo.CleanUpResource",
         "Reset Fail-Counts (Clean Up)"},

        {"PcmkMultiSelectionInfo.ManageResource",
         "Manage by CRM"},

        {"PcmkMultiSelectionInfo.UnmanageResource",
         "Do not manage by CRM"},

        {"PcmkMultiSelectionInfo.MigrateFromResource",
         "Migrate FROM"},

        {"PcmkMultiSelectionInfo.UnmigrateResource",
         "Remove Migration Constraints"},

        {"PcmkMultiSelectionInfo.RemoveService",
         "Remove Selected Services"},

        {"DrbdMultiSelectionInfo.Selection",
         "<h3>Selection:</h3>"},

        {"DrbdMultiSelectionInfo.ChangeHostColor",
         "Change Color of Selected Hosts"},

        {"DrbdMultiSelectionInfo.LoadDrbd",
         "Load DRBD Module on Selected Hosts"},

        {"DrbdMultiSelectionInfo.AdjustAllDrbd",
         "Load DRBD Config on Selected Hosts (Adjust)"},

        {"DrbdMultiSelectionInfo.UpAll",
         "Start All DRBDs on Selected Hosts (up)"},

        {"DrbdMultiSelectionInfo.Detach",
         "Detach Selected BDs"},

        {"DrbdMultiSelectionInfo.Attach",
         "Attach Selected BDs"},

        {"DrbdMultiSelectionInfo.Connect",
         "Connect Resources To Peers"},

        {"DrbdMultiSelectionInfo.Disconnect",
         "Disconnect Resources From Peers"},

        {"DrbdMultiSelectionInfo.SetPrimary",
         "Promote Selected BDs To Primary"},

        {"DrbdMultiSelectionInfo.SetSecondary",
         "Demote Selected BDs To Secondary"},

        {"DrbdMultiSelectionInfo.ForcePrimary",
         "Force Promotion To Primary"},

        {"DrbdMultiSelectionInfo.Invalidate",
         "Invalidate Selected BDs"},

        {"DrbdMultiSelectionInfo.ResumeSync",
         "Resume Syncs"},

        {"DrbdMultiSelectionInfo.PauseSync",
         "Pause Syncs"},

        {"DrbdMultiSelectionInfo.Resize",
         "Resize selected BDs"},

        {"DrbdMultiSelectionInfo.DiscardData",
         "Discard Data on selected BDs"},

        {"DrbdMultiSelectionInfo.ProxyDown",
         "Stop Proxies on selected BDs"},

        {"DrbdMultiSelectionInfo.ProxyUp",
         "Start Proxies on selected BDs"},

        {"DrbdMultiSelectionInfo.HostStopProxy",
         "Stop Proxy Daemon on selected hosts"},

        {"DrbdMultiSelectionInfo.HostStartProxy",
         "Start Proxy Daemon on selected hosts"},

        {"DrbdMultiSelectionInfo.PVCreate",
         "Create PVs on selected BDs"},

        {"DrbdMultiSelectionInfo.PVCreate.ToolTip",
         "Create physical volumes on selected block devices"},

        {"DrbdMultiSelectionInfo.PVRemove",
         "Remove selected PVs"},

        {"DrbdMultiSelectionInfo.PVRemove.ToolTip",
         "Remove selected physical volumes"},

        {"DrbdMultiSelectionInfo.VGCreate",
         "Create VG on selected PVs"},

        {"DrbdMultiSelectionInfo.VGCreate.ToolTip",
         "Create volume group on selected physical volumes"},

        {"DrbdMultiSelectionInfo.VGRemove",
         "Remove selected VGs"},

        {"DrbdMultiSelectionInfo.VGRemove.ToolTip",
         "Remove selected volume groups"},

        {"DrbdMultiSelectionInfo.LVCreate",
         "Create LV in VG "},

        {"DrbdMultiSelectionInfo.LVCreate.ToolTip",
         "Create a logical volume."},

        {"DrbdMultiSelectionInfo.LVRemove",
         "Remove selected LVs"},

        {"DrbdMultiSelectionInfo.LVRemove.ToolTip",
         "Remove selected logical volumes."},

        {"DrbdMultiSelectionInfo.LVRemove.Confirm.Title",
         "Remove selected LVs"},

        {"DrbdMultiSelectionInfo.LVRemove.Confirm.Desc",
         "Remove selected logical volumes and DESTROY all the data on "
         + "them?<br>{}"},

        {"DrbdMultiSelectionInfo.LVRemove.Confirm.Remove",
         "Rimuovi"},

        {"DrbdMultiSelectionInfo.LVRemove.Confirm.Cancel",
         "Annulla"},

        {"AllHostsInfo.QuickCluster",
         "add configured pacemaker cluster"},

        {"HbOrderInfo.NotAvailableForThisVersion",
          "Not available in this version"},

        {"CmdLog.Clear.Btn",
         "Pulisci"},

        {"CmdLog.Processed.Btn",
         "Processed"},

        {"CmdLog.Raw.Btn",
         "Raw"},

        {"CmdLog.Last.Label",
         "Last"},

        {"CmdLog.Description",
         "Commands executed on the server. You have to start the LCMC "
         + "with <b>--cmd-log</b> option to see the logs from the start.<br>"
         + "100 (2) cmd ... Time in 1/100 secs (how many times).<br>"
         + "100+ not finished (or interrupted)"},
    };
}
