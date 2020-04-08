#!/usr/bin/perl

# This file is part of Linux Cluster Management Console by Rasto Levrinc.
#
# Copyright (C) 2011 - 2012, Rastislav Levrinc.
# Copyright (C) 2009 - 2011, LINBIT HA-Solutions GmbH.
#
# DRBD Management Console is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as published
# by the Free Software Foundation; either version 2, or (at your option)
# any later version.
#
# DRBD Management Console is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with drbd; see the file COPYING.  If not, write to
# the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.

use strict;
use warnings;
$| = 1;

use Fcntl qw(F_GETFL F_SETFL O_NONBLOCK);
use POSIX qw(:errno_h); # EAGAIN
use Digest::MD5;

use Socket;

$ENV{LANG} = "C";
$ENV{LANGUAGE} = "C";
$ENV{LC_CTYPE} = "C";
$ENV{PATH} = "/sbin:/usr/sbin:/usr/local/sbin:/root/bin:/usr/local/bin"
    . ":/usr/bin:/bin:/usr/bin";
for (keys %ENV) {
    $ENV{$_} = "C" if /^LC_/;
}

{
    package Main;

    # options
    our $CMD_LOG_OP = "--cmd-log";
    our $LOG_TIME_OP = "--log-time";
    our $CMD_LOG_DEFAULT = 0;
    our $LOG_TIME_DEFAULT = 300;

    our $HW_INFO_INTERVAL = 10;

    our $NO_LVM_CACHE = 0;

    start(\@ARGV);

    sub start {
        my $argv = shift // die;
        my ($helper_options, $action_options) = Options::parse($argv);
        my $do_log = $$helper_options{$CMD_LOG_OP} || $CMD_LOG_DEFAULT;
        my $log_time = $$helper_options{$LOG_TIME_OP} || $LOG_TIME_DEFAULT;
        Log::init($do_log, $log_time);
        Disk::init();
        Drbd::init();
        Drbd_proxy::init();
        Host_software::init();
        VM::init();
        Cluster::init();
        my $action = shift @$action_options || die;
        if ($action eq "all") {
            Disk::clear_lvm_cache();
            my $drbd_devs = Drbd::get_drbd_devs();
            print "net-info\n";
            print Network::get_net_info();
            print "disk-info\n";
            print Disk::get_disk_info($NO_LVM_CACHE, $drbd_devs);
            print "disk-space\n";
            print Disk::disk_space();
            print "vg-info\n";
            print Disk::get_vg_info($NO_LVM_CACHE);
            print "filesystems-info\n";
            print Disk::get_filesystems_info();
            print "crypto-info\n";
            print Drbd::available_crypto_modules();
            print "qemu-keymaps-info\n";
            print VM::get_qemu_keymaps_info();
            print VM::get_cpu_map_info();
            print "mount-points-info\n";
            print Disk::get_mount_points_info();
            print "drbd-proxy-info\n";
            print Drbd_proxy::get_drbd_proxy_info();
            print "gui-info\n";
            print Gui_config::get_gui_info();
            print "installation-info\n";
            print Host_software::get_installation_info();
            print "gui-options-info\n";
            print Gui_config::get_gui_options_info();
            print "version-info\n";
            print Host_software::get_version_info();
        }
        elsif ($action eq "hw-info-daemon") {
            start_hw_info_daemon();
        }
        elsif ($action eq "hw-info") {
            print get_hw_info();
        }
        elsif ($action eq "hw-info-lvm") {
            Disk::clear_lvm_cache();
            my $drbd_devs = Drbd::get_drbd_devs();
            print get_hw_info();
            print "vg-info\n";
            print Disk::get_vg_info($NO_LVM_CACHE);
            print "disk-info\n";
            print Disk::get_disk_info($NO_LVM_CACHE, $drbd_devs);
            print "disk-space\n";
            print Disk::disk_space();
        }
        elsif ($action eq "hw-info-lazy") {
            print get_hw_info_lazy();
        }
        elsif ($action eq "installation-info") {
            print Host_software::get_installation_info();
        }
        elsif ($action eq "get-net-info") {
            print Network::get_net_info();
        }
        elsif ($action eq "get-disk-info") {
            my $drbd_devs = Drbd::get_drbd_devs();
            print Disk::get_disk_info($NO_LVM_CACHE, $drbd_devs);
        }
        elsif ($action eq "get-vg-info") {
            print Disk::get_vg_info($NO_LVM_CACHE);
        }
        elsif ($action eq "get-filesystems-info") {
            print Disk::get_filesystems_info();
        }
        elsif ($action eq "get-crypto-info") {
            print Drbd::available_crypto_modules();
        }
        elsif ($action eq "get-qemu-keymaps-info") {
            print VM::get_qemu_keymaps_info();
        }
        elsif ($action eq "get-cpu-map-info") {
            print VM::get_cpu_map_info();
        }
        elsif ($action eq "get-drbd-proxy-info") {
            print Drbd_proxy::get_drbd_proxy_info();
        }
        elsif ($action eq "get-gui-info") {
            print Gui_config::get_gui_info();
        }
        elsif ($action eq "get-mount-point-info") {
            print Disk::get_mount_points_info();
        }
        elsif ($action eq "get-drbd-info") {
            print Drbd::get_drbd_info();
        }
        elsif ($action eq "get-drbd-events") {
            Drbd::get_drbd_events();
        }
        elsif ($action eq "get-resource-agents") {
            Cluster::get_resource_agents(@$action_options);
        }
        elsif ($action eq "get-old-style-resources") {
            Cluster::get_old_style_resources(@$action_options);
        }
        elsif ($action eq "get-lsb-resources") {
            Cluster::get_lsb_resources();
        }
        elsif ($action eq "get-stonith-devices") {
            Cluster::get_stonith_devices(@$action_options);
        }
        elsif ($action eq "get-drbd-xml") {
            Drbd_proxy::get_drbd_proxy_xml();
            Drbd::get_drbd_xml();
        }
        elsif ($action eq "get-cluster-events") {
            my $ret = Cluster::get_cluster_events();
            if ($ret) {
                print "---start---\n";
                print "$ret\n";
                print "---done---\n";
                exit 1;
            }
        }
        elsif ($action eq "get-cluster-metadata") {
            Cluster::get_cluster_metadata();
        }
        elsif ($action eq "get-cluster-versions") {
            print Host_software::get_cluster_versions();
        }
        elsif ($action eq "get-vm-info") {
            print VM::get_vm_info();
        }
        elsif ($action eq "gui-test") {
            Gui_Test::gui_pcmk_config_test(@$action_options);
            Gui_Test::gui_pcmk_status_test(@$action_options);
        }
        elsif ($action eq "gui-drbd-test") {
            Gui_Test::gui_drbd_test(@$action_options);
        }
        elsif ($action eq "gui-vm-test") {
            Gui_Test::gui_vm_test(@$action_options);
        }
        elsif ($action eq "proc-drbd") {
            Drbd::get_proc_drbd();
        }
        elsif ($action eq "processed-log") {
            Log::processed_log();
        }
        elsif ($action eq "raw-log") {
            Log::raw_log();
        }
        elsif ($action eq "clear-log") {
            Log::clear_log();
        }
        else {
            die "unknown command: $action";
        }
    }

    # periodic stuff
    sub start_hw_info_daemon {
        my $prev_hw_info = 0;
        my $prev_hw_info_lazy = 0;
        my $prev_vm_info = 0;
        my $prev_drbd_info = 0;
        my $count = 0;
        my $use_lvm_cache = 0;
        while (1) {
            print "\n";
            if (Disk::noLvmCache()) {
                $use_lvm_cache = 0;
                Disk::useLvmCache();
            }
            my $drbd_devs = Drbd::get_drbd_devs();
            if ($count % 5 == 0) {
                my $hw_info = get_hw_info();
                $hw_info .= "vg-info\n";
                $hw_info .= Disk::get_vg_info($use_lvm_cache);
                $hw_info .= "disk-info\n";
                $hw_info .= Disk::get_disk_info($use_lvm_cache, $drbd_devs);
                if ($hw_info ne $prev_hw_info) {
                    print "--hw-info-start--" . `date +%s%N`;
                    print $hw_info;
                    $prev_hw_info = $hw_info;
                    print "--hw-info-end--\n";
                }
                $count = 0;
            }
            else {
                my $hw_info_lazy = get_hw_info_lazy();
                $hw_info_lazy .= "vg-info\n";
                $hw_info_lazy .= Disk::get_vg_info($use_lvm_cache);
                $hw_info_lazy .= "disk-info\n";
                $hw_info_lazy .= Disk::get_disk_info($use_lvm_cache, $drbd_devs);
                if ($hw_info_lazy ne $prev_hw_info_lazy) {
                    print "--hw-info-start--" . `date +%s%N`;
                    print $hw_info_lazy;
                    print "--hw-info-end--\n";
                    $prev_hw_info_lazy = $hw_info_lazy;
                }
            }
            $use_lvm_cache = 1;
            my $vm_info = VM::get_vm_info();
            if ($vm_info ne $prev_vm_info) {
                print "--vm-info-start--" . `date +%s%N`;
                print $vm_info;
                print "--vm-info-end--\n";
                $prev_vm_info = $vm_info;
            }
            my $drbd_info = Drbd::get_drbd_dump_xml();
            if ($drbd_info ne $prev_drbd_info) {
                print "--drbd-info-start--" . `date +%s%N`;
                print $drbd_info;
                print "--drbd-info-end--\n";
                $prev_drbd_info = $drbd_info;
            }
            sleep $HW_INFO_INTERVAL;
            $count++;
        }
    }

    sub get_hw_info {
        my $out = "net-info\n";
        $out .= Network::get_net_info();
        $out .= "filesystems-info\n";
        $out .= Disk::get_filesystems_info();
        $out .= "disk-space\n";
        $out .= Disk::disk_space();
        $out .= "crypto-info\n";
        $out .= Drbd::available_crypto_modules();
        $out .= "qemu-keymaps-info\n";
        $out .= VM::get_qemu_keymaps_info();
        $out .= VM::get_cpu_map_info();
        $out .= "mount-points-info\n";
        $out .= Disk::get_mount_points_info();
        $out .= "drbd-proxy-info\n";
        $out .= Drbd_proxy::get_drbd_proxy_info();
        $out .= "installation-info\n";
        $out .= Host_software::get_installation_info();
        $out .= "version-info\n";
        $out .= Host_software::get_version_info();
        return $out;
    }

    sub get_hw_info_lazy {
        my $out = "net-info\n";
        $out .= Network::get_net_info();
        $out .= "filesystems-info\n";
        $out .= Disk::get_filesystems_info();
        $out .= "disk-space\n";
        $out .= Disk::disk_space();
        $out .= "mount-points-info\n";
        $out .= Disk::get_mount_points_info();
        $out .= "installation-info\n";
        $out .= Host_software::get_installation_info();
        $out .= "drbd-proxy-info\n";
        $out .= Drbd_proxy::get_drbd_proxy_info();
        return $out;
    }
}
