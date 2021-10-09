package Cluster;

our $CLUSTER_INFO_INTERVAL;
our $OCF_DIR;
our $OCF_RESOURCE_DIR = $OCF_DIR;
our $STONITH_ADMIN_PROG;
our $PCMK_SERVICE_AGENTS;
our @SERVICE_CLASSES;

sub init() {
    $CLUSTER_INFO_INTERVAL = 10;
    $OCF_DIR = "/usr/lib/ocf";
    $OCF_RESOURCE_DIR = $OCF_DIR . "/resource.d";
    $STONITH_ADMIN_PROG = "/usr/sbin/stonith_admin";
    $PCMK_SERVICE_AGENTS = "crm_resource --list-agents ";

    @SERVICE_CLASSES = ("service", "systemd", "upstart");
}

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
    my $libpath = Host_software::get_hb_lib_path();
    my $hb_version = Command::_exec("$libpath/heartbeat -V 2>/dev/null") || "";
    my $info = get_cluster_info($hb_version);
    my $pcmk_path = "/usr/libexec/pacemaker:/usr/lib/heartbeat:/usr/lib64/heartbeat:/usr/lib/pacemaker:/usr/lib64/pacemaker:/usr/lib/x86_64-linux-gnu/pacemaker
";
    my $command;
    if (system("PATH=$pcmk_path /usr/bin/which cibmon") == 0) {
        $command = "PATH=$pcmk_path exec cibmon -udVVVV -m1 2>&1";
    } elsif (-e "/var/log/pacemaker.log") {
        $command = 'tail -F /var/log/pacemaker.log';
    } else {
        $command = 'tail -F /var/log/pacemaker/pacemaker.log';
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
        $prog = "$ptest_prog -s -S -VVVVV -L 2>&1";
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
    my $libpath = Host_software::get_hb_lib_path();
    my $pacemaker_controld = Host_software::get_pacemaker_controld();
    my $crmd = Command::_exec("$pacemaker_controld metadata 2>/dev/null");
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

