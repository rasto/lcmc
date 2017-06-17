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

package lcmc.configs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ListResourceBundle;

/**
 * Here are common commands for all linuxes.
 */
public final class DistResource extends ListResourceBundle {
    /** Sudo placeholder. */
    public static final String SUDO = "@DMCSUDO@";

    private static final Object[][] contents = {
        {"Support", "no"},
        {"arch:i686", "i[3-6]86"}, // convert arch to arch in the drbd download file
        {"arch:x86_64", "x86_64"}, // convert arch to arch in the drbd download file
        {"distribution", "undefined"},

        /* This is used to find out which distribution on web page corresponds to which
         * distribution */
        {"dist:sles",                 "suse"},
        {"dist:suse",                 "suse"},
        {"dist:opensuse",             "suse"},
        {"dist:centos",               "rhel"},
        {"dist:fedora",               "redhat"},
        {"dist:rhas",                 "redhat"},
        {"dist:rhel",                 "rhel"},
        {"dist:fc",                   "fedora"},
        {"dist:debian-etch",          "debian"},
        {"dist:ubuntu",               "ubuntu"},
        {"dist:ubuntu-dapper-server", "ubuntu"},
        {"dist:ubuntu-hardy-server",  "ubuntu"},
        {"dist:ubuntu-jaunty-server", "ubuntu"},
        {"dist:ubuntu-lucid",         "ubuntu"},
        {"dist:ubuntu-maverick",      "ubuntu"},

        {"kerneldir",                 "(.*)"},

        /* DrbdCheck.version has exit code != 0 if nothing is installed */
        {"DrbdCheck.version",
         "/sbin/drbdadm help 2>/dev/null | grep 'Version: '|sed 's/^Version: //'|sed 's/ .*//'|grep ."},

        {"HbCheck.version",
         SUDO + "@GUI-HELPER@ get-cluster-versions"},

        {"ProxyCheck.version",
         SUDO + "drbd-proxy-ctl -c version 2>/dev/null"
         + "|sed 's/.* \\([0-9.]\\+\\),.*/\\1/'"},

        {"DrbdAvailVersionsSource",
         "/usr/bin/wget --no-check-certificate -q http://oss.linbit.com/drbd/"
         + " -O - |"
         + "perl -ple '($_) = m!href=\"(\\d\\.\\d/drbd-8.*?\\.tar\\.gz)\"!"
         + " or goto LINE'"
         },

        {"DrbdUtilAvailVersionsSource",
         "/usr/bin/wget --no-check-certificate -q http://oss.linbit.com/drbd/"
         + " -O - |"
         + "perl -ple '($_) = m!href=\"(drbd-utils-\\d.*?\\.tar\\.gz)\"!"
         + " or goto LINE'"
         },

        {"TestCommand", "uptime"},

        {"GetHostAllInfo", SUDO + "@GUI-HELPER@ all"},
        {"HostHWInfoDaemon", SUDO + "@GUI-HELPER@ hw-info-daemon"},
        {"GetHostHWInfo", SUDO + "@GUI-HELPER@ hw-info"},
        {"GetHostHWInfoLVM", SUDO + "@GUI-HELPER@ hw-info-lvm"},
        {"GetHostHWInfoLazy", "nice -n 19 " + SUDO + "@GUI-HELPER@ hw-info-lazy"},
        {"GetNetInfo",  SUDO + "@GUI-HELPER@ get-net-info"},

        {"PingCommand", "while true; do echo; sleep 5; done"},

        /* heartbeat crm commands */
        {"CRM.cleanupResource", SUDO + "/usr/sbin/crm_resource -C -r @ID@ -H @HOST@; true"},

        /* 2.1.4 and before */
        {"CRM.2.1.4.startResource",
         SUDO + "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p target_role -v started"},

        {"CRM.2.1.4.stopResource",
         SUDO + "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p target_role -v stopped"},

        {"CRM.2.1.4.isManagedOn",
         SUDO + "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p is_managed -v true"},

        {"CRM.2.1.4.isManagedOff",
         SUDO + "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p is_managed -v false"},
        /* HB 2.99.0, pacemaker and after. */
        {"CRM.startResource",
         SUDO + "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p target-role -v started"},

        {"CRM.stopResource",
         SUDO + "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p target-role -v stopped"},

        {"CRM.isManagedOn",
         SUDO + "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p is-managed -v true"},

        {"CRM.isManagedOff",
         SUDO + "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p is-managed -v false"},

        {"CRM.migrateResource",
         SUDO + "/usr/sbin/crm_resource -r @ID@ -H @HOST@ --migrate"},
        {"CRM.forceMigrateResource",
         SUDO + "/usr/sbin/crm_resource -f -r @ID@ -H @HOST@ --migrate"},
        {"CRM.migrateFromResource",
         SUDO + "/usr/sbin/crm_resource -r @ID@ --migrate"},
        {"CRM.unmigrateResource",
         SUDO + "/usr/sbin/crm_resource -r @ID@ --un-migrate"},

        {"Heartbeat.getOCFParametersQuick",
         SUDO + "@GUI-HELPER@ get-resource-agents quick;"},

        {"Heartbeat.getOCFParametersConfigured",
         SUDO + "@GUI-HELPER@ get-resource-agents configured;"},

        {"Heartbeat.getOCFParameters",
         SUDO + "@GUI-HELPER@ get-resource-agents;"},

        /* vmxpath env is needed so that vmware meta-data does not hang */
        {"Heartbeat.getClusterMetadata",
         SUDO + "@GUI-HELPER@ get-cluster-metadata"},

        {"Heartbeat.getClStatus",
         SUDO + "@GUI-HELPER@ get-cluster-events"},

        {"Heartbeat.startHeartbeat",
         SUDO + "/etc/init.d/heartbeat start"},

        {"Heartbeat.stopHeartbeat",
         SUDO + "/etc/init.d/heartbeat stop"},

        {"Openais.startOpenais",
         SUDO + "/etc/init.d/openais start"},

        {"Openais.stopOpenais",
         SUDO + "/etc/init.d/openais stop"},

        {"Openais.stopOpenaisWithPcmk",
         SUDO + "/etc/init.d/pacemaker stop && "
         + SUDO + "/etc/init.d/openais stop"},

        {"Openais.reloadOpenais",
         "if ! " + SUDO + "/etc/init.d/openais status >/dev/null 2>&1; then "
         + SUDO + "/etc/init.d/openais start; fi"},

        {"Corosync.startCorosync",
         SUDO + "/etc/init.d/corosync start"},

        {"Corosync.startPcmk",
         SUDO + "/etc/init.d/pacemaker start"},

        {"Corosync.stopCorosync",
         SUDO + "/etc/init.d/corosync stop"},

        {"Corosync.stopCorosyncWithPcmk",
         SUDO + "/etc/init.d/pacemaker stop && "
         + SUDO + "/etc/init.d/corosync stop"},
        {"Corosync.startCorosyncWithPcmk",
         SUDO + "/etc/init.d/corosync start;;;"
         + SUDO + "/etc/init.d/pacemaker start"},
        {"Corosync.reloadCorosync",
         "if ! " + SUDO + "/etc/init.d/corosync status >/dev/null 2>&1; then "
         + SUDO + "/etc/init.d/corosync start; fi"},
        {"Heartbeat.reloadHeartbeat",
         "if ! " + SUDO + "/etc/init.d/heartbeat status >/dev/null 2>&1; then "
         + SUDO + "/etc/init.d/heartbeat start; fi"},
        {"Heartbeat.getHbConfig",
         SUDO + "cat /etc/ha.d/ha.cf"},

        {"Heartbeat.dopdWorkaround",
         "if [ ! -e /var/run/heartbeat/crm ]; then "
         + SUDO + "mkdir /var/run/heartbeat/crm;"
         + SUDO + "chown hacluster:haclient /var/run/heartbeat/crm;"
         + " fi"},
        {"Heartbeat.enableDopd",
         SUDO + "chgrp haclient /sbin/drbdsetup;"
         + SUDO + "chmod o-x /sbin/drbdsetup;"
         + SUDO + "chmod u+s /sbin/drbdsetup;"
         + SUDO + "chgrp haclient /sbin/drbdmeta;"
         + SUDO + "chmod o-x /sbin/drbdmeta;"
         + SUDO + "chmod u+s /sbin/drbdmeta;"},

        {"CRM.standByOn",
         SUDO + "/usr/sbin/crm_attribute -N @HOST@ -n standby -v on -l forever"},
        {"CRM.standByOff",
         SUDO + "/usr/sbin/crm_attribute -N @HOST@ -n standby -v off -l forever"},
        {"CRM.2.1.4.standByOn",
         SUDO + "/usr/sbin/crm_standby -U @HOST@ -v true"},
        {"CRM.2.1.4.standByOff",
         SUDO + "/usr/sbin/crm_standby -U @HOST@ -v false"},

        {"CRM.erase",
         SUDO
         + "/usr/sbin/cibadmin -o configuration -R -X '<constraints/>'"
         + " && " + SUDO
         + "/usr/sbin/cibadmin -o configuration -R -X '<resources/>'"},

        {"CRM.configureCommit",
         "EDITOR=\"echo '@CONFIG@'|cat>\" " + SUDO + "crm configure edit"},

        {"OpenAIS.getAisConfig",
         SUDO + "cat /etc/ais/openais.conf"},
        {"Corosync.getAisConfig",
         SUDO + "cat /etc/corosync/corosync.conf"},

        {"Cluster.Init.getInstallationInfo",
         SUDO + "@GUI-HELPER@ installation-info"},


        /* drbd commands */
        {"Drbd.getParameters", SUDO + "@GUI-HELPER@ get-drbd-xml"},
        {"Drbd.getConfig",     SUDO + "@GUI-HELPER@ get-drbd-info"},

        {"DRBD.get-gi",        "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ get-gi @RES-VOL@"},
        {"DRBD.attach",        "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ attach @RES-VOL@"},
        {"DRBD.detach",        "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ detach @RES-VOL@"},
        {"DRBD.connect",       "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ connect @RES-VOL@"},
        {"DRBD.disconnect",    "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ disconnect @RES-VOL@"},
        {"DRBD.pauseSync",     "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ pause-sync @RES-VOL@"},
        {"DRBD.resumeSync",    "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ resume-sync @RES-VOL@"},
        {"DRBD.setPrimary",    "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ primary @RES-VOL@"},
        {"DRBD.setSecondary",  "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ secondary @RES-VOL@"},
        {"DRBD.createMDDestroyData",
         SUDO + "dd if=/dev/zero of=@DEVICE@ bs=1024 count=8;"
         + " echo -e \"yes\\nyes\"|" + SUDO + "/sbin/drbdadm @DRYRUN@ create-md @RES-VOL@"},
        {"DRBD.createMD",
         "echo -e \"yes\\nyes\"|" + SUDO + "/sbin/drbdadm @DRYRUN@ create-md @RES-VOL@"},

        {"DRBD.forcePrimary",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ primary @RES-VOL@ --force"},
        {"DRBD.forcePrimary.8.3",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ -- --force primary @RES-VOL@"},

        {"DRBD.forcePrimary.8.3.7",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ -- --overwrite-data-of-peer primary @RES-VOL@"},

        {"DRBD.skipInitSync",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ new-current-uuid @RES-VOL@ --clear-bitmap"},
        {"DRBD.skipInitSync.8.3",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ -- --clear-bitmap new-current-uuid @RES-VOL@"},

        {"DRBD.skipInitSync.8.3.2",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ -- --clear-bitmap new-current-uuid @RES-VOL@"},


        {"DRBD.invalidate",    SUDO + "/sbin/drbdadm @DRYRUN@ invalidate @RES-VOL@"},
        {"DRBD.discardData",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ -- --discard-my-data connect @RES-VOL@"},

        {"DRBD.resize",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ resize @RES-VOL@"},

        {"DRBD.verify",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ verify @RES-VOL@"},

        {"DRBD.getDrbdStatus",
         SUDO + "@GUI-HELPER@ get-drbd-events"},

        {"DRBD.proxyUp",
         SUDO + "/sbin/drbdadm @DRYRUN@ proxy-up @RES-VOL@"},

        {"DRBD.proxyDown",
         SUDO + "/sbin/drbdadm @DRYRUN@ proxy-down @RES-VOL@"},

        {"DRBD.startProxy",
         SUDO + "/etc/init.d/drbdproxy start"},

        {"DRBD.stopProxy",
         SUDO + "/etc/init.d/drbdproxy stop"},

        {"DRBD.adjust",
         "if [ -e /proc/drbd ]; then echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ adjust @RES-VOL@; fi"},
        {"DRBD.adjust.apply",
         "if [ -e /proc/drbd ]; then echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ @DRYRUNCONF@ adjust @RES-VOL@; fi"},
        {"DRBD.down",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ down @RES-VOL@"},
        {"DRBD.up",
         "echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ up @RES-VOL@"},
        {"DRBD.makeFilesystem",
         SUDO + "/sbin/mkfs.@FILESYSTEM@ @DRBDDEV@"},
        {"DRBD.delMinor",
         SUDO + "/sbin/drbdsetup @DRYRUN@ @DRBDDEV@ del-minor"},

        {"DRBD.resDelConnection",
         SUDO + "/sbin/drbdsetup @DRYRUN@ @RES-VOL@ del-resource"},

        {"DRBD.getProcDrbd",
         SUDO + "@GUI-HELPER@ proc-drbd;"
         + "if [ -f /sbin/drbd-proxy-ctl ]; then " + SUDO + " /sbin/drbd-proxy-ctl -c 'show hconnections'"
              + " -c 'show hsubconnections' -c 'show memusage'"
              + " -c 'print statistics' 2>/dev/null;fi;:"},
        {"DRBD.getProcesses",  "ps aux|grep drbd"},
        {"DRBD.showProxyInfo",
         SUDO + "/sbin/drbd-proxy-ctl -c 'show hconnections'"
              + " -c 'show hsubconnections' -c 'show memusage'"
              + " -c 'print statistics';:"},
        {"DRBD.start",
         SUDO + "/etc/init.d/drbd start"},
        {"DRBD.load",
         SUDO + "/sbin/modprobe drbd"},

        {"HostBrowser.getHostInfo",
         "echo 'cluster members:';"
         + "( " + SUDO + "/usr/sbin/corosync-cmapctl "
         + " || " + SUDO + "/usr/sbin/corosync-objctl ) 2>/dev/null"
         + "|grep members|sed 's/.*= *//'"
         + "|awk '{printf(\"%s \", $0); if (NR%3==0) printf(\"\\n\")}'"
         + ";echo;" + SUDO + "/usr/sbin/corosync-cfgtool -s 2>/dev/null"
         + ";echo;" + SUDO + "/usr/sbin/corosync-quorumtool -l 2>/dev/null"

         + ";echo -----------------------------------------------------------;"
         + "echo -n 'crm verify:';"
         + SUDO + "/usr/sbin/crm_verify -VL 2>&1 "
         + "&& echo \" config Ok.\"|grep -v -e -V;"

         + "echo -----------------------------------------------------------;"
         + "echo 'crm mon:';"
         + SUDO + "/usr/sbin/crm_mon -1Arfn 2>/dev/null"
         + " || " + SUDO + "/usr/sbin/crm_mon -1rfn 2>/dev/null"
         + " || " + SUDO + "/usr/sbin/crm_mon -1rn"

         + ";echo -----------------------------------------------------------;"
         + "echo 'Interfaces:';"
         + "/sbin/ip -o -f inet a;"},

        {"HostBrowser.getHostInfoHeartbeat",
         "echo 'cluster members:';"
         + SUDO + "/usr/bin/cl_status listnodes 2>/dev/null"

         + ";echo -----------------------------------------------------------;"
         + "echo -n 'crm verify:';"
         + SUDO + "/usr/sbin/crm_verify -VL 2>&1 "
         + "&& echo \" config Ok.\"|grep -v -e -V;"

         + "echo -----------------------------------------------------------;"
         + "echo 'crm mon:';"
         + SUDO + "/usr/sbin/crm_mon -1Arfn 2>/dev/null"
         + " || " + SUDO + "/usr/sbin/crm_mon -1rfn 2>/dev/null"
         + " || " + SUDO + "/usr/sbin/crm_mon -1rn"

         + ";echo -----------------------------------------------------------;"
         + "echo 'Interfaces:';"
         + "/sbin/ip -o -f inet a;"},


        {"HostBrowser.getCrmConfigureShow",
         SUDO + "PAGER=cat /usr/sbin/crm configure show"},

        {"Logs.hbLog",
         "(grep @GREPPATTERN@ /var/log/ha.log 2>/dev/null"
         + " || grep @GREPPATTERN@ /var/log/syslog 2>/dev/null"
         + " || grep @GREPPATTERN@ /var/log/messages)|tail -500"},

        {"DrbdLog.log",
         "(grep @GREPPATTERN@ /var/log/kern.log 2>/dev/null"
         +  " || grep @GREPPATTERN@ /var/log/messages)| tail -500"},

        {"Pacemaker.Service.Ver", "0"},

        {"MakeKernelPanic", SUDO + "bash -c 'echo c > /proc/sysrq-trigger'"},
        {"MakeKernelReboot", SUDO + "bash -c 'echo b > /proc/sysrq-trigger'"},

        {"VMSXML.GetData",
         SUDO + "@GUI-HELPER@ get-vm-info"},

        {"VIRSH.Autostart",
         SUDO + "/usr/bin/virsh @OPTIONS@ autostart @VALUE@ @DOMAIN@ 2>/dev/null"},

        {"VIRSH.Start",
         SUDO + "/usr/bin/virsh @OPTIONS@ start @DOMAIN@"},

        {"VIRSH.Shutdown",
         SUDO + "/usr/bin/virsh @OPTIONS@ shutdown @DOMAIN@"},

        {"VIRSH.Destroy",
         SUDO + "/usr/bin/virsh @OPTIONS@ destroy @DOMAIN@"},

        {"VIRSH.Reboot",
         SUDO + "/usr/bin/virsh @OPTIONS@ reboot @DOMAIN@"},

        {"VIRSH.Suspend",
         SUDO + "/usr/bin/virsh @OPTIONS@ suspend @DOMAIN@"},

        {"VIRSH.Resume",
         SUDO + "/usr/bin/virsh @OPTIONS@ resume @DOMAIN@"},

        {"VIRSH.Define",
         SUDO + "/usr/bin/virsh @OPTIONS@ define @CONFIG@"},

        {"VIRSH.Undefine",
         SUDO + "/usr/bin/virsh @OPTIONS@ undefine @DOMAIN@"},

        {"Host.getConnectionStatus",
         "true"},

        {"LVM.pvcreate",
         SUDO + "pvcreate @DEVICE@"},

        {"LVM.pvremove",
         SUDO + "pvremove @DEVICE@"},

        {"LVM.vgcreate",
         SUDO + "vgcreate @VGNAME@ @PVNAMES@"},

        {"LVM.resize",
         SUDO + "lvresize -L@SIZE@ @DEVICE@"},

        {"LVM.lvcreate",
         SUDO + "lvcreate -n@LVNAME@ -L@SIZE@ @VGNAME@"},

        {"LVM.vgremove",
         SUDO + "vgremove @VGNAME@"},

        {"LVM.lvremove",
         SUDO + "lvremove -f @DEVICE@"},

        {"LVM.lvsnapshot",
         SUDO + "lvcreate -s -n@LVNAME@ -L@SIZE@ @DEVICE@"},

        {"CmdLog.Raw", "@GUI-HELPER-PROG@ @OPTIONS@ raw-log"},
        {"CmdLog.Processed", "@GUI-HELPER-PROG@ @OPTIONS@ processed-log"},
        {"CmdLog.Clear", "@GUI-HELPER-PROG@ clear-log"},

        {"libvirt.lxc.libpath", "/usr/lib/libvirt"},
        {"libvirt.xen.libpath", "/usr/lib/xen-default"},

        /* config files */
        {"ocf:heartbeat:apache.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "envfiles")))},

        {"ocf:heartbeat:LVM.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/lvm/lvm.conf")))},

        {"ocf:heartbeat:Dummy.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "state")))},

        {"ocf:heartbeat:CTDB.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/sysconfig/ctdb",
              "/etc/ctdb/nodes",
              "/etc/ctdb/public_addresses",
              "/etc/exports"
              )))},

        {"ocf:heartbeat:CTDB.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "smb_conf")))},

        {"ocf:heartbeat:drbd.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "drbdconf")))},

        {"ocf:linbit:drbd.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
                 "drbdconf")))},

        {"ocf:heartbeat:eDir88.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "eDir_config_file")))},

        {"ocf:heartbeat:exportfs.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/exports")))},

        {"ocf:heartbeat:Filesystem.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/fstab")))},

        {"ocf:heartbeat:iSCSITarget.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/initiators.deny",
              "/etc/initiators.allow")))},

        {"ocf:heartbeat:mysql-proxy.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "defaults_file")))},

        {"ocf:heartbeat:ManageRAID.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/conf.d/HB-ManageRAID")))},

        {"ocf:heartbeat:nfsserver.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/exports")))},

        {"ocf:lsb:nfs-common.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/exports")))},

        {"ocf:lsb:nfs-kernel-server.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/exports")))},

        {"ocf:heartbeat:oracle.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/oratab")))},

        {"ocf:heartbeat:oralsnr.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/oratab")))},

        {"ocf:heartbeat:portblock.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/csync2/csync2.cfg")))},

        {"ocf:heartbeat:postfix.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/postfix/main.cf")))},

        {"ocf:heartbeat:Raid1.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "raidconf")))},

        {"ocf:heartbeat:SAPDatabase.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/opt/sdb")))},

        {"ocf:heartbeat:SAPInstance.files",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "/etc/opt/sdb")))},

        {"ocf:heartbeat:Squid.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "squid_conf")))},

        {"ocf:heartbeat:vmware.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "vmxpath")))},

        {"ocf:heartbeat:Xen.params",
         Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
              "xmfile")))},

    };
    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
