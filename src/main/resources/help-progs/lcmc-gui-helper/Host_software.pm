package Host_software;
our $PROC_DRBD;

sub init() {
    $PROC_DRBD = "/proc/drbd";
}

sub get_cluster_versions {
    my $libpath = get_hb_lib_path();
    my $pacemaker_controld = find_pacemaker_controld();
    my $hb_version = Command::_exec("$libpath/heartbeat -V 2>/dev/null") || "";
    if ($hb_version) {
        $hb_version =~ s/\s+.*//;
        chomp $hb_version;
    }
    my $pm_version = Command::_exec("$pacemaker_controld --version 2>/dev/null") || "";
    if (!$pm_version) {
        $pm_version = Command::_exec("$pacemaker_controld version 2>/dev/null") || "";
    }
    $pm_version =~ s/^Pacemaker\s+//;
    $pm_version =~ s/^CRM Version:\s+//;
    $pm_version =~ s/\s+.*//;
    chomp $pm_version;

    # there is no reliable way to find the installed corosync and openais
    # version, so it is best effort or just "ok" if it is installed
    # after that only the package managers will be asked.
    my $cs_prog = "/usr/sbin/corosync";
    my $cs_version = "";
    my $cs_script = "corosync";
    my $corosync_1_2_5_file = "/tmp/corosync-1.2.5-beware";
    if (-e $cs_prog) {
        if (-e $corosync_1_2_5_file) {
            $cs_version = "1.2.5!";
        }
        else {
            my ($cs_version_string) = Command::_exec("$cs_prog -v") =~ /('.*?')/;
            # workaround for opensuse
            $cs_version_string =~ s/'UNKNOWN'/'2.3.1'/;
            ($cs_version) = $cs_version_string =~ /'(\d+\.\d+\.\d+)'/;
            if ($cs_version && "1.2.5" eq $cs_version) {
                # workaround so that corosync 1.2.5 does not fill up
                # shared momory.
                if (open TMP, ">$corosync_1_2_5_file") {
                    close TMP;
                }
            }
            else {
                unlink $corosync_1_2_5_file;
            }
        }
        if (!$cs_version) {
            $cs_version = "ok";
        }
    }
    my $ais_prog = "/usr/sbin/aisexec";
    my $ais_script = "openais";
    if (!-e "/etc/init.d/openais" && -e "/etc/init.d/openais-legacy") {
        $ais_script = "openais-legacy";
    }
    my $ais_version = "";
    if (-e $ais_prog) {
        if (!(Command::_system("/usr/bin/file $ais_prog 2>/dev/null"
            . "|grep 'shell script' > /dev/null") >> 8)
            && -e "/etc/init.d/openais") {
            $ais_version = "wrapper";
        }
        if (!$ais_version) {
            $ais_version =
                Command::_exec("grep -a -o 'subrev [0-9]* version [0-9.]*' /usr/sbin/aisexec|sed 's/.* //'");
            chomp $ais_version;
        }
        if (!$ais_version) {
            $ais_version = "ok";
        }
    }
    my $pcmk_prog = "/usr/sbin/pacemakerd";
    my $pcmk_script = "pacemaker";
    my $drbdp_script = "drbdproxy";
    my $drbdp_prog = "/sbin/drbd-proxy";
    my $hb_init = is_init("heartbeat");
    chomp $hb_init;
    my $cs_init = is_init($cs_script);
    chomp $cs_init;
    my $ais_init = is_init($ais_script);
    chomp $ais_init;
    my $pcmk_init = is_init($pcmk_script);
    chomp $pcmk_init;
    my $hb_isrc_cmd = is_script_rc("heartbeat");
    my $cs_isrc_cmd = is_script_rc($cs_script);
    my $ais_isrc_cmd = is_script_rc($ais_script);
    my $pcmk_isrc_cmd = is_script_rc($pcmk_script);
    my $hb_isrc = Command::_exec("$hb_isrc_cmd") || "off";
    my $cs_isrc = Command::_exec("$cs_isrc_cmd") || "off";
    my $ais_isrc = Command::_exec("$ais_isrc_cmd") || "off";
    my $pcmk_isrc = Command::_exec("$pcmk_isrc_cmd") || "off";
    chomp $hb_isrc;
    chomp $cs_isrc;
    chomp $ais_isrc;
    chomp $pcmk_isrc;

    my $hb_running_cmd = "$libpath/heartbeat -s";
    my $ais_running = "";
    if (!$cs_version || "wrapper" eq $ais_version) {
        my $ais_running_cmd = is_running($ais_script, $ais_prog);
        $ais_running =
            Command::_system("$ais_running_cmd >/dev/null 2>&1") >> 8 || "on";
    }
    my $cs_running_cmd = is_running($cs_script, $cs_prog);
    my $cs_running = Command::_system("$cs_running_cmd >/dev/null 2>&1") >> 8 || "on";
    my $hb_running = Command::_system("$hb_running_cmd >/dev/null 2>&1") >> 8 || "on";
    my $pcmk_running_cmd = is_running($pcmk_script, $pcmk_prog);
    my $pcmk_running =
        Command::_system("$pcmk_running_cmd >/dev/null 2>&1") >> 8 || "on";
    my $drbd_loaded = (!-e $PROC_DRBD) || "on";
    my $hb_conf = Command::_system("ls /etc/ha.d/ha.cf >/dev/null 2>&1") >> 8 || "on";
    my $drbdp_running_cmd = is_running($drbdp_script, $drbdp_prog);
    my $drbdp_running =
        Command::_system("$drbdp_running_cmd >/dev/null 2>&1") >> 8 || "on";
    my $cs_ais_conf;
    if ($cs_version) {
        $cs_ais_conf =
            Command::_system("ls /etc/corosync/corosync.conf >/dev/null 2>&1") >> 8
                || "on";
    }
    else {
        $cs_ais_conf =
            Command::_system("ls /etc/ais/openais.conf >/dev/null 2>&1") >> 8
                || "on";
    }
    chomp $hb_running;
    chomp $ais_running;
    chomp $cs_running;
    chomp $pcmk_running;
    chomp $drbdp_running;
    my $service = Command::_exec("(/usr/sbin/corosync-cmapctl service || /usr/sbin/corosync-objctl|grep '^service\.') 2>/dev/null");
    my $pcmk_svc_ver = "no";
    if ($service && $service =~ /^service\.ver=(\d+)/m) {
        $pcmk_svc_ver = $1;
    }
    # drbd version
    my ($drbd_version) =
        Command::_exec("echo|/sbin/drbdadm help 2>/dev/null") =~ /Version:\s+(\S+)/;
    $drbd_version = "" if !$drbd_version;
    my $drbd_mod_version = Command::_exec("(/sbin/modinfo -F version drbd 2>/dev/null|grep . || /sbin/modinfo -F description drbd 2>/dev/null|sed 's/.* v//')", 2) || "";
    chomp $drbd_mod_version;
    return "hb:$hb_version\n"
        . "pm:$pm_version\n"
        . "cs:$cs_version\n"
        . "ais:$ais_version\n"
        . "hb-rc:$hb_isrc\n"
        . "ais-rc:$ais_isrc\n"
        . "cs-rc:$cs_isrc\n"
        . "pcmk-rc:$pcmk_isrc\n"
        . "hb-running:$hb_running\n"
        . "cs-running:$cs_running\n"
        . "ais-running:$ais_running\n"
        . "pcmk-running:$pcmk_running\n"
        . "drbdp-running:$drbdp_running\n"
        . "hb-init:$hb_init\n"
        . "cs-init:$cs_init\n"
        . "ais-init:$ais_init\n"
        . "pcmk-init:$pcmk_init\n"
        . "pcmk-svc-ver:$pcmk_svc_ver\n"
        . "hb-conf:$hb_conf\n"
        . "cs-ais-conf:$cs_ais_conf\n"
        . "drbd:$drbd_version\n"
        . "drbd-mod:$drbd_mod_version\n"
        . "drbd-loaded:$drbd_loaded\n"
        . "hb-lib-path:$libpath\n"
}

# return -1 if ver1 is smaller than ver2 etc. 1.0.9.1 and 1.0.9 are considered
# equal and return 0.
sub compare_versions {
    my $ver1 = shift;
    my $ver2 = shift;
    my @ver1 = split /\./, $ver1;
    my @ver2 = split /\./, $ver2;
    while (@ver1 > 0 && @ver2 > 0) {
        my $v1 = shift @ver1;
        my $v2 = shift @ver2;
        if ($v1 < $v2) {
            return -1;
        }
        elsif ($v1 > $v2) {
            return 1;
        }
    }
    return 0;
}

sub pcmk_version_smaller_than {
    my $version = shift;
    my ($pcmk_version) = (get_cluster_versions() =~ /pm:([\d.]*)/);
    return compare_versions($pcmk_version, $version) < 0;
}

sub pcmk_version_greater_than {
    my $version = shift;
    my ($pcmk_version) = (get_cluster_versions() =~ /pm:([\d.]*)/);
    return compare_versions($pcmk_version, $version) > 0;
}

sub get_drbd_version {
    my (@version) = Command::_exec("echo|/sbin/drbdadm help 2>/dev/null")
        =~ /Version:\s+(\d+)\.(\d+)\.\d+/;
    return @version;
}

#
# Returns whether it is an init script.
sub is_init {
    my $script = shift;
    if (-e "/usr/lib/systemd/system/$script.service"
        || -e "/etc/init.d/$script") {
        return "on";
    }
    return "";
}

#
# Returns a portable command that determines if the init script is in rc?.d
# directories.
sub is_script_rc {
    my $script = shift;
    return
        "(/bin/systemctl is-enabled $script.service|grep enabled"
            . " || /usr/sbin/update-rc.d -n -f $script remove 2>&1|grep '/rc[0-9]\.d.*$script\\>'"
            . " || /sbin/chkconfig --list $script 2>/dev/null"
            . "|grep ':on') 2>/dev/null"
            . "|sed s/.*/on/|uniq";
}

#
# Returns a portable command that determines if the init script is running.
sub is_running {
    my $script = shift;
    my $prog = shift;
    return <<STATUS;
if (/etc/init.d/$script status 2>&1|grep 'Usage:' >/dev/null); then \
	PROG=$prog; \
	for PID in `pidof \$PROG`; do \
		if [ "\$(readlink -f /proc/\$PID/exe)" = "\$PROG" ]; then \
			exit 0; \
		fi; \
	done; \
	exit 3; \
else \
	if [ -e /etc/init.d/$script ]; then \
		out=`/etc/init.d/$script status 2>&1`; \
	else \
		out=`service $script status 2>&1`; \
	fi; \
	ret=\$?; \
	if [ -z "\$out" ]; then exit 111; else exit \$ret; fi; \
fi
STATUS
}

#
# Return heartbeat lib path. It can be /usr/lib/heartbeat or
# /usr/lib64/heartbeat
#
sub get_hb_lib_path {
    my $arch = Command::_exec("uname -m", 2);
    chomp $arch;
    if ($arch eq "x86_64" && -e "/usr/lib64") {
        return "/usr/lib64/heartbeat";
    }
    return "/usr/lib/heartbeat";
}

sub find_pacemaker_controld {
    my $hb_lib_path = get_hb_lib_path();
    for ("/usr/lib64/pacemaker",
        "/usr/libexec/pacemaker",
        "/usr/lib/x86_64-linux-gnu/pacemaker",
        "/usr/lib/pacemaker",
        $hb_lib_path) {
        if (-e "$_/pacemaker-controld") {
            return "$_/pacemaker-controld";
        } 
		if (-e "$_/crmd") {
            return "$_/crmd";
        }
		if (-e "$_/pengine") {
            return "$_/pengine";
        }
    }
}

sub get_installation_info {
    my $out = get_cluster_versions();
    my $hn = Command::_exec("hostname");
    chomp $hn;
    $out .= "hn:$hn\n";
    return $out;
}

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
