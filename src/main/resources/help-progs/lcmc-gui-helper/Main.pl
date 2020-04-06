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
    our $CLUSTER_INFO_INTERVAL = 10;
    our $OCF_DIR = "/usr/lib/ocf";
    our $OCF_RESOURCE_DIR = $OCF_DIR . "/resource.d";
    our $STONITH_ADMIN_PROG = "/usr/sbin/stonith_admin";
    our $VIRSH_COMMAND = "virsh -r";
    # --secure-info and -r don't work together
    our $VIRSH_COMMAND_NO_RO = "virsh";
    our $PCMK_SERVICE_AGENTS = "crm_resource --list-agents ";

    our @SERVICE_CLASSES = ("service", "systemd", "upstart");
    our @VM_OPTIONS = ("",
        "-c 'xen:///'",
        "-c lxc:///",
        "-c openvz:///system",
        "-c vbox:///session",
        "-c uml:///system");

    our $LVM_CACHE_FILE = "/tmp/lcmc.lvm.$$";
    our $LVM_ALL_CACHE_FILES = "/tmp/lcmc.lvm.*";
    our $NO_LVM_CACHE = 0;

    our %DISABLE_VM_OPTIONS; # it'll be populated for options that give an error

    start(\@ARGV);

    sub start {
        my $argv = shift // die;
        my ($helper_options, $action_options) = Options::parse($argv);
        my $do_log = $$helper_options{$CMD_LOG_OP} || $CMD_LOG_DEFAULT;
        my $log_time = $$helper_options{$LOG_TIME_OP} || $LOG_TIME_DEFAULT;
        Log::init($do_log, $log_time);
        Drbd::init();
        Drbd_proxy::init();
        Cluster_software::init();
        my $action = shift @$action_options || die;
        if ($action eq "all") {
            clear_lvm_cache();
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
            print get_qemu_keymaps_info();
            print get_cpu_map_info();
            print "mount-points-info\n";
            print Disk::get_mount_points_info();
            print "drbd-proxy-info\n";
            print Drbd_proxy::get_drbd_proxy_info();
            print "gui-info\n";
            print get_gui_info();
            print "installation-info\n";
            print get_installation_info();
            print "gui-options-info\n";
            print get_gui_options_info();
            print "version-info\n";
            print get_version_info();
        }
        elsif ($action eq "hw-info-daemon") {
            start_hw_info_daemon();
        }
        elsif ($action eq "hw-info") {
            print get_hw_info();
        }
        elsif ($action eq "hw-info-lvm") {
            clear_lvm_cache();
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
            print get_installation_info();
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
            print get_qemu_keymaps_info();
        }
        elsif ($action eq "get-cpu-map-info") {
            print get_cpu_map_info();
        }
        elsif ($action eq "get-drbd-proxy-info") {
            print Drbd_proxy::get_drbd_proxy_info();
        }
        elsif ($action eq "get-gui-info") {
            print get_gui_info();
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
            get_resource_agents(@$action_options);
        }
        elsif ($action eq "get-old-style-resources") {
            get_old_style_resources(@$action_options);
        }
        elsif ($action eq "get-lsb-resources") {
            get_lsb_resources();
        }
        elsif ($action eq "get-stonith-devices") {
            get_stonith_devices(@$action_options);
        }
        elsif ($action eq "get-drbd-xml") {
            Drbd_proxy::get_drbd_proxy_xml();
            Drbd::get_drbd_xml();
        }
        elsif ($action eq "get-cluster-events") {
            my $ret = get_cluster_events();
            if ($ret) {
                print "---start---\n";
                print "$ret\n";
                print "---done---\n";
                exit 1;
            }
        }
        elsif ($action eq "get-cluster-metadata") {
            get_cluster_metadata();
        }
        elsif ($action eq "get-cluster-versions") {
            print Cluster_software::get_cluster_versions();
        }
        elsif ($action eq "get-vm-info") {
            print get_vm_info();
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
            if (!-e $LVM_CACHE_FILE) {
                $use_lvm_cache = 0;
                Command::_exec("touch $LVM_CACHE_FILE");
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
            my $vm_info = get_vm_info();
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
        $out .= get_qemu_keymaps_info();
        $out .= get_cpu_map_info();
        $out .= "mount-points-info\n";
        $out .= Disk::get_mount_points_info();
        $out .= "drbd-proxy-info\n";
        $out .= Drbd_proxy::get_drbd_proxy_info();
        #$out .= "gui-info\n";
        #$out .= get_gui_info();
        $out .= "installation-info\n";
        $out .= get_installation_info();
        $out .= "version-info\n";
        $out .= get_version_info();
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
        $out .= get_installation_info();
        $out .= "drbd-proxy-info\n";
        $out .= Drbd_proxy::get_drbd_proxy_info();
        return $out;
    }

    # prints available qemu keymaps.
    sub get_qemu_keymaps_info {
        my $out = "";
        for (Command::_exec("ls /usr/share/qemu*/keymaps/ 2>/dev/null")) {
            $out .= $_;
        }
        return $out;
    }

    sub get_cpu_map_info {
        my @models;
        my %vendors;
        if (open my $cpu_map_fh, "/usr/share/libvirt/cpu_map.xml") {
            while (<$cpu_map_fh>) {
                my ($model) = /<model\s+name=\'(.*)'>/;
                push @models, $model if $model;
                my ($vendor) = /<vendor>(.*)</
                    || /<vendor\s+name=\'(.*?)'.*\/>/;
                $vendors{$vendor} = 1 if $vendor;
            }
        }
        my $out = "";
        $out .= "cpu-map-model-info\n";
        for (@models) {
            $out .= "$_\n";
        }
        $out .= "cpu-map-vendor-info\n";
        for (sort keys %vendors) {
            $out .= "$_\n";
        }
        return $out;
    }

    # get_gui_info()
    #
    sub get_gui_info {
        my $out = "";
        if (open FH, "/var/lib/heartbeat/drbdgui.cf") {
            while (<FH>) {
                $out .= "$_";
            }
            close FH;
        }
        return $out;
    }

    # get_installation_info()
    #
    sub get_installation_info {
        my $out = Cluster_software::get_cluster_versions();
        my $hn = Command::_exec("hostname");
        chomp $hn;
        $out .= "hn:$hn\n";
        return $out;
    }

    #
    # get_gui_options_info
    #
    sub get_gui_options_info {
        my $out = "o:vm.filesystem.source.dir.lxc\n";
        $out .= "/var/lib/lxc\n";
        $out .= Command::_exec("ls -1d /var/lib/lxc/*/rootfs 2>/dev/null");
        return $out;
    }

    #
    # get_version_info()
    #
    sub get_version_info {
        my $cmd =
            'uname; uname -m; uname -r; '
                . 'for d in redhat debian gentoo SuSE SUSE distro; do '
                . 'v=`head -1 -q /etc/"$d"_version /etc/"$d"-release /etc/"$d"-brand 2>/dev/null`; '
                . 'if [ ! -z "$v" ]; then echo "$v"; echo "$d"; fi; '
                . 'done |head -2'
                . '| sed "s/distro/openfiler/";'
                . 'lsb_release -i -r 2>/dev/null '
                . '| sed "s/centos/redhat/I"|sed "s/SUSE LINUX/suse/" '
                . '| sed "s/openSUSE project/suse/" '
                . '| sed "s/openSUSE$/suse/" '
                . '| sed "s/enterprise_linux\|ScientificSL/redhatenterpriseserver/" '
                . '| perl -lne "print lc((split /:\s*/)[1])"'
                . '| sed "s/oracleserver/redhat/"; '
                . 'cut -d ":" /etc/system-release-cpe -f 4,5 2>/dev/null|sed "s/:/\n/"'
                . '| sed "s/enterprise_linux/redhatenterpriseserver/" '
                . '| sed "s/centos/redhat/" ';
        return Command::_exec("$cmd");
    }

    sub cib_message {
        my $socket = shift;
        my $msg = shift;
        $msg = ">>>\n$msg<<<\n";
        printf $socket pack "L", length $msg;
        printf $socket pack "L", 0xabcd;
        print $socket $msg;
    }

    sub get_message {
        my $socket = shift;
        my $msg = "";
        while (<$socket>) {
            if ($_ eq "<<<\n") {
                return $msg;
            }
            if ($_ !~ />>>/) {
                $msg .= $_;
            }
        }
        die;
    }

    #
    # Prints cib info.
    #
    sub get_cluster_events {
        my $kidpid;
        die "can't fork: $!" unless defined($kidpid = fork());
        if ($kidpid) {
            # parent
            do_cluster_events();
            kill 1, $kidpid;
        }
        else {
            # kid
            while (1) {
                print "---reset---\n"; # reset timeout
                sleep $CLUSTER_INFO_INTERVAL;
            }
        }
    }

    sub do_cluster_events {
        my $libpath = Cluster_software::get_hb_lib_path();
        my $hb_version = Command::_exec("$libpath/heartbeat -V 2>/dev/null") || "";
        my $info = get_cluster_info($hb_version);
        my $pcmk_path = "/usr/libexec/pacemaker:/usr/lib/heartbeat:/usr/lib64/heartbeat:/usr/lib/pacemaker:/usr/lib64/pacemaker:/usr/lib/x86_64-linux-gnu/pacemaker";
        my $command =
            "PATH=$pcmk_path exec cibmon -udVVVV -m1 2>&1";
        if ($hb_version && (Cluster_software::compare_versions($hb_version, "2.1.4") <= 0)) {
            $command =
                " PATH=$pcmk_path exec cibmon -dV -m1 2>&1";
        }
        if ($info) {
            print "---start---\n";
            print $info;
            print "---done---\n";
            my $prev_info = 0;
            if (!open EVENTS, "$command|") {
                Log::print_warning("can't execute $command\n");
                return;
            }
            else {
                while (<EVENTS>) {
                    # pcmk 1.1.8, it's an error, but
                    # still indicates an event
                    if (/signon to CIB failed/i) {
                        print "ERROR: signon to CIB failed";
                        return;
                    }
                    elsif (/error:/
                        || /Diff: ---/
                        || /Local-only Change:/) {
                        my $cluster_info = get_cluster_info($hb_version);
                        if ($cluster_info ne $prev_info) {
                            print "---start---\n";
                            print $cluster_info;
                            print "---done---\n";
                            $prev_info = $cluster_info;
                        }
                    }
                }
            }
        }
        else {
            print "ERROR: cib connection error";
        }
    }

    #
    # Get info from ptest and make xml from it. This is used only to find out
    # if a resource is running, not running and/or unmanaged
    # unmanaged etc.
    sub get_resource_status {
        my $hb_version = shift;
        my %role;
        my %unmanaged;
        my %resources;
        my @fenced_nodes;
        my $crm_simulate_prog = "/usr/sbin/crm_simulate";
        my $prog = "crm_simulate -s -S -VVVVV -L 2>&1";
        my $errors = ""; # TODO: error handling
        my $ptest_prog = "/usr/sbin/ptest";
        if (!-e $crm_simulate_prog && -e $ptest_prog) {
            if ($hb_version
                && (Cluster_software::compare_versions($hb_version, "2.1.4") <= 0)) {
                $prog = "$ptest_prog -S -VVVVV -L 2>&1";
            }
            else {
                $prog = "$ptest_prog -s -S -VVVVV -L 2>&1";
            }
            # default, because crm_simulate
            # crashes sometimes
            # older ptest versions don't have -s option
        }
        elsif (!-e $ptest_prog) {
            $errors .= "could not find $prog\n";
        }
        my %allocation_scores;
        for my $line (Command::_exec("$prog")) {
            my $what;
            my $on;
            my $res;
            if ($line =~ /pe_fence_node:\s+Node\s+(\S+)/) {
                push @fenced_nodes, $1;
            }
            elsif ($line =~
                /Leave\s+(?:resource\s+)?(\S+)\s+\((.*?)\)/) {
                # managed: Started, Master, Slave, Stopped
                $res = $1;
                my $state = $2;
                if ($res =~ /(.*):\d+$/) {
                    $res = $1;
                }
                if ($state =~ /\s+unmanaged$/) {
                    $unmanaged{$res}++;
                }
                else {
                    if ($state =~ /^(Stopped)/) {
                    }
                    else {
                        if ($state =~ /\s/) {
                            ($what, $on) =
                                split /\s+/, $state;
                        }
                        else {
                            $what = "started";
                            $on = $state;
                        }

                    }
                }
            }
            elsif ($line
                =~ /Stop\s+resource\s+(\S+)\s+\((.*)\)/) {
                # Stop, is still slave or started
                $res = $1;
                $on = $2;
                if ($res =~ /(.*):\d+$/) {
                    $res = $1;
                    $what = "slave";
                }
                else {
                    $what = "started";
                }
            }
            elsif ($line =~
                /Demote\s+(\S+)\s+\(Master -> \S+\s+(.*)\)/) {
                # Demote master -> *, still master
                $res = $1;
                $on = $2;
                if ($res =~ /(.*):\d+$/) {
                    $res = $1;
                    $what = "master";
                }
            }
            elsif ($line =~
                /Promote\s+(\S+)\s+\((\S+) -> \S+\s+(.*)\)/) {
                # Promote from something, still that something
                $res = $1;
                $what = $2;
                $on = $3;
                if ($res =~ /(.*):\d+$/) {
                    $res = $1;
                }
            }
            elsif ($line =~
                /Move\s+(\S+)\s+\((\S+)\s+(\S+)/) {
                # Promote from something, still that something
                $res = $1;
                $what = $2;
                $on = $3;
            }
            elsif ($line =~
                /native_print:\s+(\S+).*:\s+(.*)\s+\(unmanaged\)$/) {
                # unmanaged
                $res = $1;
                my $state = $2;
                $state =~ s/\(.*\)\s+//;
                if ($res =~ /(.*):\d+$/) {
                    $res = $1;
                }
                if ($state =~ /^(Stopped)/) {
                }
                else {
                    ($what, $on) = split /\s+/, $state;
                }
                $unmanaged{$res}++;
            }
            elsif ($line =~ /native_color:\s+(\S+)\s+allocation score on ([^:]+):\s+(\S+)/) {
                my $r = $1;
                my $host = $2;
                my $score = $3;
                $allocation_scores{$r}{$host} = $score;
            }
            if ($res) {
                $resources{$res}++;
                if ($what && $on) {
                    $role{$res}{$on} = $what if !$role{$res}{$on};
                }
            }
        }
        my $fenced_nodes_ret = "";
        if (@fenced_nodes > 0) {
            $fenced_nodes_ret .= "<fenced>\n";
            for my $n (@fenced_nodes) {
                $fenced_nodes_ret .= " <node>$n</node>\n"
            }
            $fenced_nodes_ret .= "</fenced>\n";
        }
        my $info = "";
        for my $res (sort keys %resources) {
            my $running = "running";
            if (keys %{$role{$res}} == 0) {
                $running = "stopped";
            }
            my $managed = "managed";
            if ($unmanaged{$res}) {
                $managed = "unmanaged";
            }
            $info .= "  <resource id=\"$res\""
                . " running=\"$running\""
                . " managed=\"$managed\">\n";
            for my $on (sort keys %{$role{$res}}) {
                my $tag = $role{$res}{$on};
                $info .= "    <$tag>$on</$tag>\n";
            }
            if ($allocation_scores{$res}) {
                $info .= "    <scores>\n";
                for my $host (keys %{$allocation_scores{$res}}) {
                    my $score = $allocation_scores{$res}{$host};
                    $info .= "      <score host=\"$host\" score=\"$score\"/>\n";
                }
                $info .= "    </scores>\n";
            }
            $info .= "  </resource>\n";
        }
        if ($info) {
            return("<resource_status>\n$info</resource_status>\n",
                $fenced_nodes_ret);
        }
        return("", $fenced_nodes_ret);
    }

    sub get_cluster_info {
        my $hb_version = shift;
        my ($info, $fenced_nodes) = get_resource_status($hb_version);
        # TODO: use cib.xml if cibadmin can't connect
        my $cibinfo = Command::_exec("/usr/sbin/cibadmin -Ql || cat /var/lib/pacemaker/cib/cib.xml /var/lib/heartbeat/crm/cib.xml 2>/dev/null");
        if ($cibinfo) {
            my $res_status = "res_status";
            my $cibquery = "cibadmin";
            return "$res_status\nok\n$info\n>>>$res_status\n"
                . "$cibquery\nok\n<pcmk>\n$fenced_nodes$cibinfo</pcmk>\n"
                . ">>>$cibquery\n";
        }
        return "\n";
    }

    sub get_cluster_metadata {
        print "<metadata>\n";
        my $libpath = Cluster_software::get_hb_lib_path();
        my $crmd_libpath = Cluster_software::get_crmd_lib_path();
        # pengine moved in pcmk 1.1.7
        my $pengine = Command::_exec("$crmd_libpath/pengine metadata 2>/dev/null || $libpath/pengine metadata 2>/dev/null");
        if ($pengine) {
            # remove first line
            substr $pengine, 0, index($pengine, "\n") + 1, '';
            print $pengine;
        }
        my $crmd = Command::_exec("$crmd_libpath/crmd metadata 2>/dev/null");
        if ($crmd) {
            # remove first line
            substr $crmd, 0, index($crmd, "\n") + 1, '';
            print $crmd;
        }
        print "</metadata>\n";
    }

    sub get_existing_resources {
        my $list = Command::_exec("crm_resource -L");
        my %existing_rscs;
        while ($list =~ /\((.*)::(.*):(.*)\)/g) {
            $existing_rscs{$1}{$2}{$3} = 1;
        }
        return \%existing_rscs;
    }

    sub get_resource_agents {
        my $type = shift // "";
        my $existing_rscs_ocf;
        my $existing_rscs_stonith;
        if ("configured" eq $type) {
            my $existing_rscs = get_existing_resources();
            $existing_rscs_ocf = $$existing_rscs{"ocf"};
            $existing_rscs_stonith = $$existing_rscs{"stonith"};
        }
        print "class:ocf\n";
        get_ocf_resources($type, $existing_rscs_ocf);
        print "provider:heartbeat\n";
        print "master:\n";
        print "class:stonith\n";
        get_stonith_devices($type, $existing_rscs_stonith);
        if ("quick" eq $type) {
            print "class:heartbeat\n";
            get_old_style_resources($type);
            for my $service (@SERVICE_CLASSES) {
                get_service_resources($service);
            }
            # lsb is subset of service, but this is needed for
            # services already configured as lsb in pcmk config.
            print "class:lsb\n";
            get_lsb_resources();
        }
    }

    sub get_ocf_resources {
        my $type = shift // "";
        my $existing_rscs = shift;
        my $quick = 0;
        if ("quick" eq $type) {
            $quick = 1;
        }
        if ("configured" eq $type) {
            for my $prov (keys %{$existing_rscs}) {
                print "provider:$prov\n";
                for my $s (keys %{$$existing_rscs{$prov}}) {
                    get_ocf_resource($prov, $s, $quick);
                }
            }
        }
        else {
            opendir my $dfh, "$OCF_RESOURCE_DIR" or return;
            for my $prov (sort grep {/^[^.]/} readdir $dfh) {
                print "provider:$prov\n";
                opendir my $d2fh, "$OCF_RESOURCE_DIR/$prov" or next;
                for my $s (sort grep {/^[^.]/ && !/\.metadata$/} readdir $d2fh) {
                    get_ocf_resource($prov, $s, $quick);
                }
            }
        }
    }

    sub get_ocf_resource {
        my $prov = shift;
        my $s = shift;
        my $quick = shift;
        if ($quick) {
            $s =~ s/\.sh$//;
            print "ra:$s\n";
        }
        else {
            my $ra_name = $s;
            $ra_name =~ s/\.sh$//;
            print "ra-name:$ra_name\n";
            print "master:";
            print Command::_exec("grep -wl crm_master $OCF_RESOURCE_DIR/$prov/$s;echo;") . "\n";
            print Command::_exec("OCF_RESKEY_vmxpath=a OCF_ROOT=$OCF_DIR $OCF_RESOURCE_DIR/$prov/$s meta-data 2>/dev/null");
        }
    }

    sub get_old_style_resources {
        my $type = shift // "";
        my $quick = 0;
        if ("quick" eq $type) {
            $quick = 1;
        }
        my $dir = "/etc/ha.d/resource.d/";
        for (Command::_exec("ls $dir 2>/dev/null")) {
            print "ra:$_";
        }
    }

    # service, upstart, systemd.
    # would work for lsb, but it's not backwards compatible
    sub get_service_resources {
        my $service = shift;
        print "class:$service\n";
        for (Command::_exec("$PCMK_SERVICE_AGENTS $service 2>/dev/null")) {
            next if /@/; # skip some weird stuff
            print "ra:$_";
        }
    }

    sub get_lsb_resources {
        # old style init scripts (lsb)
        my $dir = "/etc/init.d/";
        for (Command::_exec("find $dir -perm -u=x -xtype f -printf \"%f\n\"")) {
            print "ra:$_";
        }
    }

    sub get_stonith_devices {
        my $type = shift // "";
        my $existing_rscs = shift;
        if (!-e $STONITH_ADMIN_PROG) {
            get_stonith_devices_old($type, $existing_rscs);
        }
        my $quick = 0;
        my $configured = 0;
        my %configured_devs;
        if ("quick" eq $type) {
            $quick = 1;
        }
        elsif ("configured" eq $type) {
            $configured = 1;
            for my $p (keys %$existing_rscs) {
                for my $s (keys %{$$existing_rscs{$p}}) {
                    $configured_devs{$s}++
                }
            }

        }

        for my $name (Command::_exec("$STONITH_ADMIN_PROG -I")) {
            chomp $name;
            if ($quick) {
                print "ra:$name\n";
                next;
            }
            if ($configured && !$configured_devs{$name}) {
                next;
            }
            my $metadata = Command::_exec("$STONITH_ADMIN_PROG -M -a $name");
            $metadata =~ s/(<resource-agent.*?)\>/$1 class="stonith">/;
            if (!$metadata) {
                next;
            }
            print "ra-name:$name\n";
            print $metadata;
        }
    }

    # squeeze, natty
    sub get_stonith_devices_old {
        my $type = shift // "";
        my $existing_rscs = shift;
        my $quick = 0;
        my $configured = 0;
        my %configured_devs;
        if ("quick" eq $type) {
            $quick = 1;
        }
        elsif ("configured" eq $type) {
            $configured = 1;
            for my $p (keys %$existing_rscs) {
                for my $s (keys %{$$existing_rscs{$p}}) {
                    $configured_devs{$s}++
                }
            }

        }
        my $libdir = "/usr/lib/stonith/plugins";
        my $arch = Command::_exec("uname -m", 2);
        chomp $arch;
        if ($arch eq "x86_64") {
            my $libdir64 = "/usr/lib64/stonith/plugins";
            if (-e $libdir64) {
                $libdir = $libdir64;
            }
        }
        for my $subtype ("external") {
            my $dir = "$libdir/$subtype/";
            for (Command::_exec("find $dir -perm -a=x -type f -printf \"%f\n\"")) {
                if ($quick) {
                    print "ra:$subtype/$_";
                }
                else {
                    chomp;
                    if ($configured && !$configured_devs{$_}) {
                        next;
                    }
                    my $path = "PATH=\$PATH:/usr/share/cluster-glue";
                    print get_ocf_like_stonith_devices(
                        "$subtype/$_",
                        scalar Command::_exec("$path $dir/$_ getinfo-devid"),
                        scalar Command::_exec("$path $dir/$_ getinfo-devdescr")
                            . Command::_exec("$path $dir/$_ getinfo-devurl"),
                        scalar Command::_exec("$path $dir/$_ getinfo-xml"));
                }
            }
        }

        for my $subtype ("stonith2") {
            my $dir = "$libdir/$subtype/";
            for (Command::_exec("find $dir -type f -name *.so -printf \"%f\n\"")) {
                chomp;
                my $name = $_;
                $name =~ s/\.so$//;
                if ($quick) {
                    print "ra:$name\n";
                    next;
                }
                if ($configured && !$configured_devs{$name}) {
                    next;
                }
                my $info = Command::_exec("/usr/sbin/stonith -t $name -h");
                if (!$info) {
                    next;
                }
                my ($shortdesc, $longdesc) = $info
                    =~ /^STONITH Device:\s+(.*?)$(.*?)List of valid/ms;
                my $content;
                open my $fh, "$dir/$_" or next;
                {
                    local $/;
                    $content = <$fh>;
                }
                close $fh;
                if (!$content) {
                    next;
                }
                my ($parameters) =
                    $content =~ /(<parameters>.*?<\/parameters>)/s;
                print get_ocf_like_stonith_devices($name,
                    $shortdesc,
                    $longdesc,
                    $parameters);
            }
        }
    }

    sub get_ocf_like_stonith_devices {
        my $device = shift;
        my $shortdesc = shift;
        my $longdesc = shift;
        my $parameters = shift;
        my $class = "stonith";

        return <<XML;
<?xml version="1.0"?>
<resource-agent name="$device" class="$class">
<version>1.0</version>

<shortdesc lang="en">$shortdesc</shortdesc>
<longdesc lang="en">$longdesc</longdesc>
$parameters
<actions>
<action name="monitor" timeout="60" interval="60" />
<action name="start"   timeout="60" />
<action name="stop"    timeout="60" />
</actions>
</resource-agent>
XML
    }

    sub get_vm_networks {
        my %autostart;
        for (Command::_exec("ls /etc/libvirt/qemu/networks/autostart/*.xml 2>/dev/null")) {
            my ($name) = /([^\/]+).xml/;
            next if !$name;
            $autostart{$name}++;
        }
        my $out = "";
        for (Command::_exec("ls /etc/libvirt/qemu/networks/*.xml 2>/dev/null")) {
            my ($name) = /([^\/]+).xml/;
            next if !$name;
            chomp;
            my $config = Command::_exec("$VIRSH_COMMAND net-dumpxml $name 2>/dev/null")
                || "";
            if ($config) {
                $out .= "<net name=\"$name\" config=\"$_\"";
                if ($autostart{$name}) {
                    $out .= ' autostart="True"';
                }
                else {
                    $out .= ' autostart="False"';
                }
                $out .= ">\n";
                $out .= $config;
                $out .= "</net>\n";
            }
        }
        return $out;
    }

    sub get_vm_info {
        my $networks = get_vm_networks();
        my %autostart;
        for (Command::_exec("ls /etc/libvirt/qemu/autostart/*.xml 2>/dev/null; ls /etc/xen/auto/ 2>/dev/null")) {
            my ($name) = /([^\/]+).xml/;
            next if !$name;
            $autostart{$name}++;
        }
        my $libvirt_version = "";
        if (Command::_exec("$VIRSH_COMMAND version 2>/dev/null") =~ /libvirt\s+([0-9\.]+)/) {
            $libvirt_version = $1;
        }
        my $out = "<version>$libvirt_version</version>\n";
        OPTIONS:
        for my $options (@VM_OPTIONS) {
            if ($DISABLE_VM_OPTIONS{$options}) {
                next;
            }
            my $header = 1;
            for (Command::_exec("$VIRSH_COMMAND $options list --all 2>&1")) {
                if ($header) {
                    if (/^-{5}/) {
                        $header = 0;
                    }
                    elsif (/^error:/) {
                        # disable the ones that give an
                        # error
                        $DISABLE_VM_OPTIONS{$options}++;
                        next OPTIONS;
                    }
                    next;
                }
                my ($name) = /^\s*\S+\s+(\S+)/;
                next if !$name;
                chomp;
                my $info =
                    Command::_exec("$VIRSH_COMMAND $options dominfo $name 2>/dev/null|grep -v 'CPU time'")
                        || "";
                next if !$info;
                my $vncdisplay =
                    Command::_exec("$VIRSH_COMMAND $options vncdisplay $name 2>/dev/null") || "";
                my $config_in_etc;
                #if (open CONFIG, $_) {
                #	local $/;
                #	$config_in_etc = <CONFIG>;
                #	close CONFIG;
                #}
                my $config;
                $config =
                    Command::_exec("$VIRSH_COMMAND_NO_RO $options dumpxml --security-info $name 2>/dev/null") || "";
                $out .= "<vm name=\"$name\"";
                if ($autostart{$name}) {
                    $out .= ' autostart="True"';
                }
                else {
                    $out .= ' autostart="False"';
                }
                if ($options) {
                    $out .= ' virsh-options="' . $options . '"';
                }
                $out .= ">\n";
                $out .= "<info>\n";
                $out .= $info;
                $out .= "</info>\n";
                $out .= "<vncdisplay>$vncdisplay</vncdisplay>\n";
                if ($config) {
                    $out .= "<config>\n";
                    $out .= $config;
                    $out .= "</config>\n";
                }
                if ($config_in_etc) {
                    $out .= "<config-in-etc>\n";
                    $out .= "<![CDATA[$config_in_etc]]>";
                    $out .= "</config-in-etc>\n";
                }
                $out .= "</vm>\n";
            }
        }
        if ($networks) {
            $out .= $networks;
        }
        my $md5 = Digest::MD5::md5_hex($out);
        my $ret = "<vms md5=\"$md5\">\n";
        $ret .= $out;
        $ret .= "</vms>\n";
        return $ret;
    }

    # force daemon to reread the lvm information
    sub clear_lvm_cache {
        unlink glob $LVM_ALL_CACHE_FILES;
    }
}
