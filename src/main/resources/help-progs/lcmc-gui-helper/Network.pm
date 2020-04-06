package Network;

# get_net_info()
#
# parses ifconfig output and prints out interface, ip and mac address one
# interface per line.
sub get_net_info {
    my $bridges = get_bridges();
    my $out = "";
    for (Command::_exec("ip -o -f inet a; ip -o -f inet6 a")) {
        next if /scope link/;
        my ($dev, $type, $addr, $mask) =
            /^\d+:\s*(\S+)\s+(\S+)\s+(\S+)\/(\S+)/;
        next if !$dev;

        my $af;
        if ("inet" eq $type) {
            $af = "ipv4";
        }
        elsif ("inet6" eq $type) {
            $af = "ipv6";
        }
        else {
            next;
        }
        if ("lo" eq $dev) {
            $out = "$dev $af $addr $mask\n" . $out;
        }
        else {
            $out .= "$dev $af $addr $mask";
            if ($$bridges{$dev}) {
                $out .= " bridge\n";
            }
            else {
                $out .= "\n";
            }
        }
    }
    $out .= "bridge-info\n";
    for (keys %$bridges) {
        $out .= "$_\n";
    }
    return $out;
}

# Returns all bridges as an array.
sub get_bridges {
    my %bridges;
    my $brctl = get_brctl_path();
    for (Command::_exec("$brctl show 2>/dev/null")) {
        next if /^\s*bridge\s+name/;
        next if /^\s/;
        $bridges{(split)[0]}++;
    }
    return \%bridges;
}

sub get_brctl_path {
    for my $p ("/usr/sbin/brctl", "/sbin/brctl", "/usr/local/sbin/brctl") {
        if (-e $p) {
            return $p;
        }
    }
    return "/usr/sbin/brctl";
}
