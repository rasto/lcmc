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

import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Here are common commands for all linuxes.
 */
public final class DistResource extends java.util.ListResourceBundle {
    /** Sudo placeholder. */
    public static final String SUDO = "@DMCSUDO@";
    /** Get contents. */
    @Override protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
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
         DistResource.SUDO + "@GUI-HELPER@ get-cluster-versions"},
        /* DrbdAvailableVersions returns available versions of drbd in the download area. One
         * version per line.
         *
         * example output:
         * -----
         * drbd-0.7.17
         * drbd-0.7.18
         * drbd-0.7.19
         * drbd-0.7.20
         * ------
         */
        {"DrbdAvailVersions",
         "/usr/bin/wget --no-check-certificate -q "
         + "http://www.linbit.com/@SUPPORTDIR@/ -O - "
         + "|perl -ple '($_) = /href=\"@DRBDDIR@-(\\d[.rc0-9]*?)\\/\"/ or goto LINE'"},

        {"DrbdAvailVersionsSource",
         "/usr/bin/wget --no-check-certificate -q http://oss.linbit.com/drbd/"
         + " -O - |"
         + "perl -ple '($_) = m!href=\"(\\d\\.\\d/drbd-[89].*?\\.tar\\.gz)\"!"
         + " or goto LINE'"
         },

        {"DrbdAvailDistributions",
         "/usr/bin/wget --no-check-certificate -q"
         + " http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/ -O - "
         + "|perl -ple '($_) = m!href=\"([^\"/]+)/\"! or goto LINE'"},

        {"DrbdAvailKernels",
         "/usr/bin/wget --no-check-certificate -q"
         + " http://www.linbit.com/@SUPPORTDIR@"
         + "/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/ -O -"
         + " |perl -ple '($_) = m!href=\"([^\"/]+)/\"! or goto LINE'"
        },

        {"DrbdAvailArchs",
         "/usr/bin/wget --no-check-certificate -q"
         + " http://www.linbit.com/@SUPPORTDIR@/"
         + "@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/ -O -"
         + " |perl -ple '($_) = m!href=\"drbd8?-(?:plus8?-)?(?:km|module)-.+?(i386|x86_64|amd64|i486|i686|k7)\\.(?:rpm|deb)\"! or goto LINE'"
        },

        {"DrbdAvailBuilds",
         "/usr/bin/wget --no-check-certificate -q"
         + " http://www.linbit.com/@SUPPORTDIR@/"
         + "@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/ -O -"
         + " |perl -ple '($_) = m!href=\"drbd8?-(?:plus8?-)?(?:km|module)-(.*?)[-_]@DRBDVERSION@.+?[._]@ARCH@\\..+?\"! or goto LINE'"
        },

        {"DrbdAvailVersionsForDist",
         "/usr/bin/wget --no-check-certificate -q"
         + " http://www.linbit.com/@SUPPORTDIR@/"
         + "@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/ -O -"
         + " |perl -ple '($_) = m!href=\"drbd8?-(?:plus8?-)?(?:utils_)?(\\d.*?)-\\d+(?:\\.el5)[._]@ARCH@\\..+?\"! or goto LINE'"
        },

        {"DrbdAvailFiles",
         "/usr/bin/wget --no-check-certificate -q http://www.linbit.com/"
         + "@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@"
         + "/@KERNELVERSIONDIR@/"
         + " -O - |perl -ple '($_) = m!href=\"(drbd8?-(?:plus8?-)?(?:utils)?"
         + "(?:bash-completion)?"
         + "(?:heartbeat)?"
         + "(?:pacemaker)?"
         + "(?:udev)?"
         + "(?:xen)?"
         + "(?:(?:km|module|utils|bash-completion|heartbeat|pacemaker|udev|xen)"
         + "[_-]@BUILD@)?[-_]?@DRBDVERSION@.*?[._]@ARCH@"
         + "\\.(?:rpm|deb))\"! or goto LINE'"
        },

        {"TestCommand", "uptime"},

        /* donwload and installation */
        {"DrbdInst.test",
         "/bin/ls /tmp/drbdinst/@DRBDPACKAGES@"},

        {"DrbdInst.mkdir",   "/bin/mkdir -p /tmp/drbdinst/"},

        {"DrbdInst.wget",
         "/usr/bin/wget --no-check-certificate --http-user='@USER@'"
         + " --http-passwd='@PASSWORD@' --directory-prefix=/tmp/drbdinst/ "
         + "http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@"
         + "/@DISTRIBUTION@/@KERNELVERSIONDIR@/@DRBDPACKAGES@"},
         //+ "http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@"
         //+ "/@DISTRIBUTION@/@KERNELVERSIONDIR@/@DRBDPACKAGE@ "
         //+ "http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@"
         //+ "/@DISTRIBUTION@/@KERNELVERSIONDIR@/@DRBDMODULEPACKAGE@"},
        {"DrbdInst.start",   SUDO + "/etc/init.d/drbd start"},

        {"installGuiHelper", "installGuiHelper"}, // is treated specially by ssh class.

        {"GetHostAllInfo", SUDO + "@GUI-HELPER@ all"},
        {"GetHostHWInfo", SUDO + "@GUI-HELPER@ hw-info"},
        {"GetHostHWInfoLazy", "nice -n 19 " + SUDO + "@GUI-HELPER@ hw-info-lazy"},
        {"GetNetInfo",  SUDO + "@GUI-HELPER@ get-net-info"},

        /* heartbeat crm commands */
        {"CRM.cleanupResource", SUDO + "/usr/sbin/crm_resource -C -r @ID@ -H @HOST@"},

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

        /* gets all ocf resources and theirs meta-data */
        /* TODO: buggy xml in heartbeat 2.0.8 in ftp and mysql */
        /* TODO: implement version overwrite */
        {"Heartbeat.2.0.8.getOCFParameters",
         "export OCF_ROOT=/usr/lib/ocf;"
         + "for s in `ls -1 /usr/lib/ocf/resource.d/heartbeat/"
         + " | grep -v Pure-FTPd|grep -v mysql`;"
         + " do /usr/lib/ocf/resource.d/heartbeat/$s meta-data 2>/dev/null;"
         + "done"},

        {"Heartbeat.getOCFParameters",
         "export OCF_RESKEY_vmxpath=a;export OCF_ROOT=/usr/lib/ocf;"
         + "for prov in `ls -1 /usr/lib/ocf/resource.d/`; do "
         +  "for s in `ls -1 /usr/lib/ocf/resource.d/$prov/ `; do "
         +  "echo -n 'provider:'; echo $prov;"
         +  "echo -n 'master:';"
         +  "grep -wl crm_master /usr/lib/ocf/resource.d/$prov/$s;echo;"
         +   "/usr/lib/ocf/resource.d/$prov/$s meta-data 2>/dev/null; done;"
         + "done;"
         + "echo 'provider:heartbeat';"
         + "echo 'master:';"
         + SUDO + "@GUI-HELPER@ get-stonith-devices;"
         + SUDO + "@GUI-HELPER@ get-old-style-resources;"
         + SUDO + "@GUI-HELPER@ get-lsb-resources"},
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
         + "/usr/sbin/cibadmin --obj_type configuration -R -X '<constraints/>'"
         + " && " + SUDO
         + "/usr/sbin/cibadmin --obj_type configuration -R -X '<resources/>'"},

        {"CRM.configureCommit",
         "EDITOR=\"echo '@CONFIG@'|cat>\" " + SUDO + "crm configure edit"},

        {"OpenAIS.getAisConfig",
         DistResource.SUDO + "cat /etc/ais/openais.conf"},
        {"Corosync.getAisConfig",
         DistResource.SUDO + "cat /etc/corosync/corosync.conf"},

        {"Cluster.Init.getInstallationInfo",
         SUDO + "@GUI-HELPER@ installation-info"},


        /* drbd commands */
        {"Drbd.getParameters", SUDO + "@GUI-HELPER@ get-drbd-xml"},
        {"Drbd.getConfig",     "echo|" + SUDO + "/sbin/drbdadm dump-xml"},

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

        {"DRBD.adjust",
         "if [ -e /proc/drbd ]; then echo|" + SUDO + "/sbin/drbdadm @DRYRUN@ adjust @RES-VOL@; fi"},

        {"DRBD.adjust.dryrun",
         "echo|" + SUDO + "/sbin/drbdadm -d adjust @RES-VOL@"},
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

        {"DRBD.getProcDrbd",   SUDO + "@GUI-HELPER@ proc-drbd"},
        {"DRBD.getProcesses",  "ps aux|grep drbd"},
        {"DRBD.start",
         SUDO + "/etc/init.d/drbd start"},
        {"DRBD.load",
         SUDO + "/sbin/modprobe drbd"},

        {"HostBrowser.getCrmMon",
         SUDO + "/usr/sbin/crm_mon -1"},
        {"HostBrowser.getCrmConfigureShow",
         SUDO + "PAGER=cat /usr/sbin/crm configure show"},

        {"Logs.hbLog",
         "(grep @GREPPATTERN@ /var/log/ha.log 2>/dev/null"
         + " || grep @GREPPATTERN@ /var/log/syslog 2>/dev/null"
         + " || grep @GREPPATTERN@ /var/log/messages)|tail -500"},

        {"DrbdLog.log",
         "(grep @GREPPATTERN@ /var/log/kern.log 2>/dev/null"
         +  " || grep @GREPPATTERN@ /var/log/messages)| tail -500"},

        /* DrbdINst.install.x is automatically in 'sudo bash -c ...' */
        {"DrbdInst.install.text.6", "packages from LINBIT"},
        {"DrbdInst.install.6",
                        " packages from www.linbit.com for LINBIT customers"},
        {"DrbdInst.install.method.6",       "linbit"},

        {"Pacemaker.Service.Ver", "0"},

        {"MakeKernelPanic", SUDO + "bash -c 'echo c > /proc/sysrq-trigger'"},
        {"MakeKernelReboot", SUDO + "bash -c 'echo b > /proc/sysrq-trigger'"},

        {"VMSXML.GetData",
         SUDO + "@GUI-HELPER@ get-vm-info"},

        {"VIRSH.Start",
         SUDO + "virsh start @DOMAIN@"},

        {"VIRSH.Shutdown",
         SUDO + "virsh shutdown @DOMAIN@"},

        {"VIRSH.Destroy",
         SUDO + "virsh destroy @DOMAIN@"},

        {"VIRSH.Reboot",
         SUDO + "virsh reboot @DOMAIN@"},

        {"VIRSH.Suspend",
         SUDO + "virsh suspend @DOMAIN@"},

        {"VIRSH.Resume",
         SUDO + "virsh resume @DOMAIN@"},

        {"VIRSH.Define",
         SUDO + "virsh define @CONFIG@"},

        {"VIRSH.Undefine",
         SUDO + "virsh undefine @DOMAIN@"},

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
}
