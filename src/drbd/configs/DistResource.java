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

package drbd.configs;

import java.util.Arrays;

/**
 * Here are common commands for all linuxes.
 */
public class DistResource extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "no"},
        {"arch:i686", "i386"}, // convert arch to arch in the drbd download file
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
        {"dist:ubuntu-jaunty-server",  "ubuntu"},

        {"kerneldir",                 "(.*)"},

        /* DrbdCheck.version has exit code != 0 if nothing is installed */
        {"DrbdCheck.version",
         "/sbin/drbdadm help 2>/dev/null | grep 'Version: '|sed 's/^Version: //'|sed 's/ .*//'|grep ."},

        {"HbCheck.version",
         "@GUI-HELPER@ get-cluster-versions"},
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
         + "|perl -ple '($_) = /href=\"@DRBDDIR@-(\\d.*?)\\/\"/ or goto LINE'"},

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
         + " |perl -ple '($_) = m!href=\"drbd8?-(?:plus8?-)?(?:utils_)?(\\d.*?)-\\d+[._]@ARCH@\\..+?\"! or goto LINE'"
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
        {"DrbdInst.start",   "/etc/init.d/drbd start"},

        {"installGuiHelper", "installGuiHelper"}, // is treated specially by ssh class.

        {"GetHostAllInfo", "@GUI-HELPER@ all"},
        {"GetHostHWInfo", "@GUI-HELPER@ hw-info"},
        {"GetNetInfo",  "@GUI-HELPER@ get-net-info"},

        /* heartbeat crm commands */
        {"CRM.cleanupResource",    "/usr/sbin/crm_resource -C -r @ID@ -H @HOST@"},

        /* 2.1.4 and before */
        {"CRM.2.1.4.startResource",
         "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p target_role -v started"},

        {"CRM.2.1.4.stopResource",
         "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p target_role -v stopped"},

        {"CRM.2.1.4.isManagedOn",
         "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p is_managed -v true"},

        {"CRM.2.1.4.isManagedOff",
         "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p is_managed -v false"},
        /* HB 2.99.0, pacemaker and after. */
        {"CRM.startResource",
         "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p target-role -v started"},

        {"CRM.stopResource",
         "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p target-role -v stopped"},

        {"CRM.isManagedOn",
         "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p is-managed -v true"},

        {"CRM.isManagedOff",
         "/usr/sbin/crm_resource --meta -t primitive -r @ID@ -p is-managed -v false"},

        {"CRM.migrateResource",   "/usr/sbin/crm_resource -r @ID@ -H @HOST@ --migrate"},
        {"CRM.forceMigrateResource",
         "/usr/sbin/crm_resource -f -r @ID@ -H @HOST@ --migrate"},
        {"CRM.migrateFromResource", "/usr/sbin/crm_resource -r @ID@ --migrate"},
        {"CRM.unmigrateResource", "/usr/sbin/crm_resource -r @ID@ --un-migrate"},

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
         + "@GUI-HELPER@ get-stonith-devices;"
         + "@GUI-HELPER@ get-old-style-resources;"
         + "@GUI-HELPER@ get-lsb-resources"},
        /* vmxpath env is needed so that vmware meta-data does not hang */
        {"Heartbeat.getClusterMetadata",
         "@GUI-HELPER@ get-cluster-metadata"},

        {"Heartbeat.getClStatus",
         "@GUI-HELPER@ get-cluster-events"},

        {"Heartbeat.startHeartbeat", "/etc/init.d/heartbeat start"},
        {"Heartbeat.stopHeartbeat",  "/etc/init.d/heartbeat stop"},
        {"Openais.startOpenais",   "/etc/init.d/openais start"},
        {"Openais.stopOpenais",   "/etc/init.d/openais stop"},
        {"Openais.reloadOpenais",  "/etc/init.d/openais reload"},
        {"Corosync.startCorosync",   "/etc/init.d/corosync start"},
        {"Corosync.stopCorosync",   "/etc/init.d/corosync start"},
        {"Corosync.reloadCorosync",  "/etc/init.d/corosync force-reload"},
        {"Heartbeat.reloadHeartbeat", "/etc/init.d/heartbeat reload"},

        {"Heartbeat.getHbConfig",    "cat /etc/ha.d/ha.cf"},

        {"Heartbeat.dopdWorkaround",
         "if [ ! -e /var/run/heartbeat/crm ]; then"
         + " mkdir /var/run/heartbeat/crm;"
         + " chown hacluster:haclient /var/run/heartbeat/crm;"
         + " fi"},
        {"Heartbeat.enableDopd",
         "chgrp haclient /sbin/drbdsetup;"
         + "chmod o-x /sbin/drbdsetup;"
         + "chmod u+s /sbin/drbdsetup;"
         + "chgrp haclient /sbin/drbdmeta;"
         + "chmod o-x /sbin/drbdmeta;"
         + "chmod u+s /sbin/drbdmeta;"},

        {"CRM.standByOn",
         "/usr/sbin/crm_attribute -N @HOST@ -n standby -v on -l forever"},
        {"CRM.standByOff",
         "/usr/sbin/crm_attribute -N @HOST@ -n standby -v off -l forever"},
        {"CRM.2.1.4.standByOn",
         "/usr/sbin/crm_standby -U @HOST@ -v true"},
        {"CRM.2.1.4.standByOff",
         "/usr/sbin/crm_standby -U @HOST@ -v false"},

        {"CRM.setGroupOrder",
         "echo group @GROUPID@ @RESOURCES@"
         + "|/usr/sbin/crm configure load update -"},

        {"OpenAIS.getAisConfig",     "cat /etc/ais/openais.conf"},
        {"Corosync.getAisConfig",    "cat /etc/corosync/corosync.conf"},

        {"Cluster.Init.getInstallationInfo",
         "@GUI-HELPER@ installation-info"},


        /* drbd commands */
        {"Drbd.getParameters", "@GUI-HELPER@ get-drbd-xml"},
        {"Drbd.getConfig",     "echo|/sbin/drbdadm dump-xml"},

        {"DRBD.attach",        "echo|/sbin/drbdadm @DRYRUN@ attach @RESOURCE@"},
        {"DRBD.detach",        "echo|/sbin/drbdadm @DRYRUN@ detach @RESOURCE@"},
        {"DRBD.connect",       "echo|/sbin/drbdadm @DRYRUN@ connect @RESOURCE@"},
        {"DRBD.disconnect",    "echo|/sbin/drbdadm @DRYRUN@ disconnect @RESOURCE@"},
        {"DRBD.pauseSync",     "echo|/sbin/drbdadm @DRYRUN@ pause-sync @RESOURCE@"},
        {"DRBD.resumeSync",    "echo|/sbin/drbdadm @DRYRUN@ resume-sync @RESOURCE@"},
        {"DRBD.setPrimary",    "echo|/sbin/drbdadm @DRYRUN@ primary @RESOURCE@"},
        {"DRBD.setSecondary",  "echo|/sbin/drbdadm @DRYRUN@ secondary @RESOURCE@"},
        {"DRBD.createMDDestroyData",
         "dd if=/dev/zero of=@DEVICE@ bs=1024 count=8;"
         + " echo -e \"yes\\nyes\"|/sbin/drbdadm @DRYRUN@ create-md @RESOURCE@"},
        {"DRBD.createMD",
         "echo -e \"yes\\nyes\"|/sbin/drbdadm @DRYRUN@ create-md @RESOURCE@"},
        {"DRBD.forcePrimary",
         "echo|/sbin/drbdadm @DRYRUN@ -- --overwrite-data-of-peer primary @RESOURCE@"},

        {"DRBD.invalidate",    "echo|/sbin/drbdadm @DRYRUN@ invalidate @RESOURCE@"},
        {"DRBD.discardData",
         "echo|/sbin/drbdadm @DRYRUN@ -- --discard-my-data connect @RESOURCE@"},

        {"DRBD.resize",        "echo|/sbin/drbdadm @DRYRUN@ resize @RESOURCE@"},

        {"DRBD.verify",        "echo|/sbin/drbdadm @DRYRUN@ verify @RESOURCE@"},

        {"DRBD.getDrbdStatus",
         "@GUI-HELPER@ get-drbd-events"},

        {"DRBD.adjust",
         "if [ -e /proc/drbd ]; then echo|/sbin/drbdadm @DRYRUN@ @DRYRUNCONF@ adjust @RESOURCE@; fi"},

        {"DRBD.adjust.dryrun", "echo|/sbin/drbdadm -d adjust @RESOURCE@"},
        {"DRBD.down",          "echo|/sbin/drbdadm @DRYRUN@ down @RESOURCE@"},
        {"DRBD.up",            "echo|/sbin/drbdadm @DRYRUN@ up @RESOURCE@"},
        {"DRBD.makeFilesystem", "/sbin/mkfs.@FILESYSTEM@ @DRBDDEV@;sync"},

        {"DRBD.getProcDrbd",   "cat /proc/drbd"},
        {"DRBD.getProcesses",  "ps aux|grep drbd"},
        {"DRBD.start",         "/etc/init.d/drbd start"},
        {"DRBD.load",          "/sbin/modprobe drbd"},

        {"HostBrowser.getCrmMon",
         "/usr/sbin/crm_mon -1"},
        {"HostBrowser.getCrmConfigureShow",
         "crm configure show"},

        {"Logs.hbLog",
         "(grep @GREPPATTERN@ /var/log/ha.log 2>/dev/null"
         + " || grep @GREPPATTERN@ /var/log/syslog 2>/dev/null"
         + " || grep @GREPPATTERN@ /var/log/messages)|tail -500"},

        {"DrbdLog.log",
         "(grep @GREPPATTERN@ /var/log/kern.log 2>/dev/null"
         +  " || grep @GREPPATTERN@ /var/log/messages)| tail -500"},

        {"DrbdInst.install.text.1", "packages from LINBIT"},
        {"DrbdInst.install.1",
                        " packages from www.linbit.com for LINBIT customers"},
        {"DrbdInst.install.method.1",       "linbit"},

        {"Pacemaker.Service.Ver", "0"},

        {"MakeKernelPanic", "echo c > /proc/sysrq-trigger"},
        {"MakeKernelReboot", "echo b > /proc/sysrq-trigger"},

        {"VMSXML.GetData",
         "@GUI-HELPER@ get-vm-info"},

        {"VIRSH.Start",
         "virsh start @DOMAIN@"},

        {"VIRSH.Shutdown",
         "virsh shutdown @DOMAIN@"},

        {"VIRSH.Destroy",
         "virsh destroy @DOMAIN@"},

        {"VIRSH.Reboot",
         "virsh reboot @DOMAIN@"},

        {"VIRSH.Suspend",
         "virsh suspend @DOMAIN@"},

        {"VIRSH.Resume",
         "virsh resume @DOMAIN@"},

        {"VIRSH.Define",
         "virsh define @CONFIG@"},

        {"VIRSH.Undefine",
         "virsh undefine @DOMAIN@"},
    };
}
