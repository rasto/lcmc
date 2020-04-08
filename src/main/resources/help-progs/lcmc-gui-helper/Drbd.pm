package Drbd;

our $DRBD_INFO_INTERVAL;
our $PROC_DRBD;
our $DRBD_PROXY_GET_PLUGINS;
our $DRBD_PROXY_SHOW;

sub init {
    $DRBD_INFO_INTERVAL = 10;
    $PROC_DRBD = "/proc/drbd";
    $DRBD_PROXY_GET_PLUGINS = "drbd-proxy-ctl -c 'show avail-plugins'";
    $DRBD_PROXY_SHOW = "drbd-proxy-ctl -c show";
}

sub get_drbd_events {
    my $kidpid;
    die "can't fork: $!" unless defined($kidpid = fork());
    if ($kidpid) {
        while (1) {
            do_drbd_events();
            sleep $DRBD_INFO_INTERVAL;
        }
    }
    else {
        while (1) {
            print "\n"; # reset timeout
            sleep $DRBD_INFO_INTERVAL;
        }
    }
}

sub do_drbd_events {
    if (!-e $PROC_DRBD) {
        print "--nm--\n";
        return;
    }
    my ($v1, $v2) = Host_software::get_drbd_version();
    my $command;
    if ($v1 < 7 || ($v1 == 8 && $v2 < 4)) { # < 8.4.0
        $command = "/sbin/drbdsetup /dev/drbd0 events -a -u";
    }
    else {
        $command = "/sbin/drbdsetup all events";
    }
    my $prev_drbd_info = 0;
    if (!open EVENTS, "$command|") {
        Log::print_warning("can't execute $command\n");
        return;
    }
    else {
        while (<EVENTS>) {
            if ($_ && $_ !~ /\d+\s+(ZZ|\?\?)/) {
                my $drbd_info = get_drbd_dump_xml();
                if ($drbd_info ne $prev_drbd_info) {
                    print "--drbd-info-start--"
                        . `date +%s%N`;
                    print $drbd_info;
                    print "--drbd-info-end--\n";
                    $prev_drbd_info = $drbd_info;
                }
                print "--event-info-start--" . `date +%s%N`;
                print "$_";
                print "--event-info-end--\n";
            }
        }
    }
    close EVENTS;
}

sub get_drbd_info {
    print "--drbd-info-start--" . `date +%s%N`;
    print get_drbd_dump_xml();
    print "--drbd-info-end--\n";
}

sub get_drbd_dump_xml {
    return Command::_exec("/sbin/drbdadm -d dump-xml 2>&1");
}

# get_drbd_devs
# Returns hash with drbd devices as keys and the underlying blockd evices as their
# value.
sub get_drbd_devs {
    my %drbd_devs;
    for (Command::_exec(
        qq(for f in `find /dev/drbd/by-disk/ -name '*' 2>/dev/null`;do
    			if [ -L \$f ]; then
    				echo -n "\$f ";
    				readlink -f \$f;
    			fi;
    	     	done))) {
        my ($dev, $drbd) = split;
        $dev =~ s!^/dev/drbd/by-disk/!/dev/!;
        $drbd_devs{$drbd} = $dev;
    }
    return \%drbd_devs;
}

sub get_proc_drbd {
    my %texts = (ns => "sent over network",
        nr          => "received over network",
        dw          => "written to the disk",
        dr          => "read from the disk",
        al          => "number of activity log updates",
        bm          => "number of bitmap updates",
        lo          => "local count",
        pe          => "pending",
        ua          => "unacknowledged",
        ap          => "application pending",
        ep          => "epochs",
        wo          => "write order",
        oos         => "out of sync");
    my %units = (ns => "KB",
        nr          => "KB",
        dw          => "KB",
        dr          => "KB",
        al          => "",
        bm          => "",
        lo          => "",
        pe          => "",
        ua          => "",
        ap          => "",
        ep          => "",
        wo          => "fdn",
        oos         => "KB");
    my %write_orders = (b => "barrier",
        f                 => "flush",
        d                 => "drain",
        n                 => "none");

    if (!open my $pfh, "/proc/drbd") {
        Log::print_warning("can't open /proc/drbd: $!\n");
    }
    else {
        while (<$pfh>) {
            my @infos;
            print;
            if (/ns:/ && /nr:/ && /dr:/) {
                @infos = split;
            }
            my $l = 0;
            for (values %texts) {
                $l = length $_ if length $_ > $l;
            }
            for (@infos) {
                my ($name, $value) = split /:/;
                if ($texts{$name}) {
                    print "    $texts{$name}: ";
                    print " " x ($l - length $texts{$name});
                    my $unit = $units{$name};
                    if ("fdn" eq $unit) {
                        print $write_orders{$value}
                            . "\n";
                    }
                    elsif ("KB" eq $unit) {
                        print convert_kilobytes($value)
                            . "\n";
                    }
                    else {
                        print "$value\n";
                    }
                }
                else {
                    print "$name: $value\n";
                }
            }
            if (/ns:/ && /nr:/ && /dr:/) {
                print "\n\n";
            }
        }
        close $pfh;
    }
}

sub get_drbd_xml {
    my %missing; # handlers and startup don't come from drbdsetup xml-help, so
    # we parse them out of the man page.
    my @missing;
    my $manpage = Command::_exec("zcat /usr/share/man/man5/drbd.conf.5.gz || cat /usr/share/man/man5/drbd.conf.5 || cat /usr/man/man5/drbd.conf.5 || bzcat /usr/share/man/man5/drbd.conf.5.bz2");
    if (!$manpage) {
        Log::print_error("drbd xml\n");
        exit 1;
    }
    my $from = "";

    for my $section ("global", "handlers", "startup") {
        my ($part) = $manpage =~ /^\\fB$section\\fR$(.*?)\.[TPs][Pp]/sm;
        my @options = map {s/\\-/-/g;
            $_} $part =~ /\\fB(.*?)\\fR(?!\()/g;
        push @missing, $section;
        $missing{$section} = \@options;
    }

    #$missing{"resource"} = ["protocol", "device"];
    push @missing, "resource";

    my @a = $manpage =~ /^\\fB(([^\n]*?)(?:\s+(?:\\fR\\fB)?\\fI|\\fR$).*?)\.[TP]P/msg;
    my %descs;
    while (@a) {
        if ($from && $a[1] ne "on-io-error") {
            shift @a;
            next;
        }
        $from = "";
        my $desc = shift @a;
        my $command = shift @a;
        $desc =~ s/.\\" drbd.conf:.*$//gm;
        $desc =~ s/\n(?!\n)/ /g;
        $desc =~ s/\.RS 4/\n/g;
        $desc =~ s/\.sp/\n\n/g;
        # split lines that are max 80 characters long.
        my $cols = 80;
        $desc = join "\n",
            map {
                my $line = $_;
                $_ = "";
                while (length $line >= $cols) {
                    my $r = rindex $line, " ", $cols;
                    my $next_line = substr $line,
                        $r,
                        length($line) - $r,
                        "\n";
                    $_ .= $line;
                    $line = $next_line;
                };
                $_ . $line}
                split /\n/, $desc;
        for ($desc, $command) {
            s/\\m\[blue\]//g;
            s/\\m\[\].*?s\+2//g;
            s/\\-/-/g;
            s/\\'/'/g;
            s/\\&//g;
            s/&/&amp;/g;
            s/\\fI(.*?)\\fR/&lt;u&gt;&lt;i&gt;$1&lt;\/i&gt;&lt;\/u&gt;/g; # italic
            s/\\fB(.*?)\\fR/&lt;b&gt;$1&lt;\/b&gt;/g;                     # bold
            s/\</&lt;/g;
            s/\>/&gt;/g;
            s/\\fB//g;
            s/\.fR//g;
            s/\\fR//g;
            s/\.RS 4/&lt;br&gt;/g;
            s/\.RS//g;
            s/\.RE//g;
            s/\.sp/&lt;br&gt;/g;
            s/\.[TP]P//g;
            s/\n/&lt;br&gt;/g;
        }
        $descs{$command} = "<desc>&lt;html&gt;$desc&lt;/html&gt;</desc>";
    }

    for (@missing) {
        print "<command name=\"$_\">\n";
        for my $option (@{$missing{$_}}) {
            my $desc = $descs{$option};
            my $type = "string";
            my $handlers = "";
            my $default;
            my $min;
            my $max;

            if ($desc) {
                my ($arg) = $desc =~ /^.*?&lt;i&gt;(.*?)&lt;/;
                if (!$arg || $arg eq $option) {
                    $type = "boolean";
                }
                elsif ($arg eq "count" || $arg eq "time") {
                    $type = "numeric";
                }
                my ($part) =
                    $desc =~ /valid.*?options.*?are:(.*)/si;
                if ($part) {
                    my @hs =
                        $part =~ /&lt;b&gt;(.*?)&lt;\/b&gt;/g;
                    if (@hs > 0) {
                        $type = "handler";
                        for my $h (@hs) {
                            $handlers .= "<handler>$h</handler>";
                        }
                    }
                }
                if ($type eq "numeric") {
                    ($default) = $desc =~ /default\s+.*?is\s+(\d+)/i;
                    ($min, $max) = $desc =~ /from (\d+) to (\d+)/;
                }
            }
            print "\t<option name=\"$option\" type=\"$type\">\n";
            if ($handlers) {
                print "\t\t$handlers\n";
            }
            if (defined $default) {
                print "\t\t<default>$default</default>\n";
            }
            if (defined $min) {
                print "\t\t<min>$min</min>\n";
            }
            if (defined $max) {
                print "\t\t<max>$max</max>\n";
            }
            if ($desc) {
                print "\t\t$desc\n";
            }
            print "\t</option>\n";
        }
        print "</command>\n";
    }

    my ($v1, $v2) = Host_software::get_drbd_version();
    my @sections = ("net-options", "disk-options");
    my $help_option = "xml-help";
    if ($v1 < 7 || ($v1 == 8 && $v2 < 4)) {
        # < 8.4.0
        @sections = ("net", "disk", "syncer");
        $help_option = "xml";
    }

    for (@sections) {
        my $xml = Command::_exec("/sbin/drbdsetup $help_option $_");
        if ($Command::COMMAND_ERRNO) {
            Log::print_error("can't exec drbdsetup: $Command::COMMAND_ERRNO\n");
            exit 1;
        }
        $xml =~ s/(option name="(.*?)".*?)(<\/option>)/$1 . ($descs{$2} || "not documented") . $3 /egs;
        print $xml;
    }
}

sub convert_kilobytes {
    my $value = shift;
    for my $unit ("KiBytes", "MiBytes", "GiBytes", "TiBytes") {
        if ($value < 1024) {
            return sprintf("%.2f %s", $value, $unit);
        }
        $value = $value / 1024;
    }
    return $value . " TiBytes";
}

sub available_crypto_modules {
    my @modules;
    my %module_exists;
    for (Command::_exec("cat /proc/crypto")) {
        my ($cr) = /^name\s*:\s*(\S+)/;
        next if !$cr || $cr =~ /\(/ || $module_exists{$cr};
        push @modules, $cr;
        $module_exists{$cr}++;

    }
    for (Command::_exec("ls /lib/modules/`uname -r`/kernel/crypto/*.ko", 2)) {
        my ($cr) = /([^\/]+).ko$/;
        next if $module_exists{$cr};
        if ($cr eq "sha1" || $cr eq "md5" || $cr eq "crc32c") {
            unshift @modules, $cr;
        }
        else {
            push @modules, $cr;
        }
    }
    my $out = "";
    for (@modules) {
        $out .= "$_\n";
    }
    return $out;
}

