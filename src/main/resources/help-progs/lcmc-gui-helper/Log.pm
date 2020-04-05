package Log;

our $LOG_FILE;
our $LOG_FH;
our $DO_LOG;
our $LOG_TIME;

our $DEBUG = 0;

sub init {
    $DO_LOG = shift // die;
    $LOG_TIME = shift // die;
    $LOG_FILE = "/var/log/lcmc.log";
}

sub print_debug {
    print "DEBUG: $_[0]\n" if $DEBUG;
}

sub print_warning {
    print "WARNING: $_[0]\n";
}

sub print_error {
    print "ERROR: $_[0]\n";
}

sub disk_info_error {
    my $error = shift;
    print STDERR "ERROR: problem getting disk info: $error\n";
}

sub disk_info_warning {
    my $warning = shift;
    print STDERR "WARNING: $warning\n";
}

sub _log {
    my $cmd = shift;
    my $level = shift;
    if ($DO_LOG >= $level) {
        if (!$LOG_FH) {
            if (!open $LOG_FH, ">>$LOG_FILE") {
                $DO_LOG = 0;
                return;
            }
        }
        print $LOG_FH "$cmd\n";
    }
}

sub _log_time {
    my $time = substr `date +%s%N`, 0, 13;
    return $time;
}

sub processed_log {
    my $now = _log_time();
    my $from_time = $now - _convert_to_sec($LOG_TIME) * 1000;
    if (!open LOG, $LOG_FILE) {
        print "can't open $LOG_FILE\n";
        return;
    }
    my %start;
    my %cmds;
    my %cmd_times;
    my %cmd_no;
    my @cmd_log;
    while (<LOG>) {
        my ($time, $log_action, $code, $cmd) =
            /(\d+)\s+(\S+)\s+(\S+)\s+(.*)/;
        if (!$time || !$log_action || !$code || !$cmd) {
            print "ERROR: can't parse: $_\n";
            next;
        }
        if ($time < $from_time) {
            next;
        }
        if ("start" eq $log_action) {
            $start{$code} = $time;
            $cmds{$code} = $cmd;
        } elsif ("done" eq $log_action) {
            next if !$start{$code}; # ignore without start

            my $duration = $time - $start{$code};
            delete $start{$code};
            compute_cmd_times(\%cmd_times, \%cmd_no, $cmd, $duration);
            if ($duration > 1000) {
                push @cmd_log, "$duration $cmd\n";
            }
        } else {
            print "ERROR: unknown action: $log_action\n";
            next;
        }
    }
    close $LOG_FILE;
    for my $code (keys %start) {
        my $duration = $now - $start{$code};
        if ($duration > 1000) {
            push @cmd_log, "$duration+ " . _cut($cmds{$code}) . "\n";
        }
    }

    for my $cmd (sort { $cmd_times{$a} <=> $cmd_times{$b} } keys %cmd_times) {
        print $cmd_times{$cmd} . " (" . $cmd_no{$cmd} . ") " . _cut($cmd) . "\n";
    }

    print for @cmd_log;
}

sub raw_log {
    my $now = _log_time();
    if (!open LOG, $LOG_FILE) {
        print "can't open $LOG_FILE\n";
        return;
    }
    my $from_time = $now - _convert_to_sec($LOG_TIME) * 1000;
    while (<LOG>) {
        my ($time, $rest) = /(\d+)\s+(.*)/;
        if (!$time || !$rest) {
            print "ERROR: can't parse: $_\n";
            next;
        }
        if ($time < $from_time) {
            next;
        }
        print _format_time($time)." $rest\n";
    }
    close $LOG_FILE;
}

sub clear_log {
    if (!-e $LOG_FILE || !open LOG, ">$LOG_FILE") {
        print "can't open $LOG_FILE\n";
        return;
    }

    print LOG "";
}

# find the biggest common string with every previous command
sub compute_cmd_times {
    my $cmd_times = shift;
    my $cmd_no = shift;
    my $cmd = shift;
    my $duration = shift;
    my $min_length = index " ", $cmd;
    $min_length = length $cmd if $min_length < 0;

    for my $o_cmd (keys %$cmd_times) {
        INNER: for (my $l = (length $o_cmd); $l >= $min_length; $l--) {
            my $s1 = substr $cmd, 0, $l;
            my $s2 = substr $o_cmd, 0, $l;
            if ($s1 eq $s2) {
                $$cmd_times{$s1} += $duration;
                $$cmd_no{$s1}++;
                last INNER;
            }
        }
    }
    if (!$$cmd_times{$cmd}) {
        $$cmd_times{$cmd} += $duration;
        $$cmd_no{$cmd}++;
    }
}

sub _format_time {
    my $time = substr shift, 0, 10;
    my ($s, $min, $h, $d, $m, $y) = (localtime($time))[0..5];
    $m++;
    $y += 1900;
    return sprintf "%04d-%02d-%02d %02d:%02d:%02d", $y, $m, $d, $h, $min, $s;
}

sub _convert_to_sec {
    my $time = shift;
    my ($t, $unit) = $time =~ /(\d+)(\D+)/;
    return $LOG_TIME if !$t;
    return $t if !$unit;
    if ("s" eq $unit) {
        return $t;
    } elsif ("m" eq $unit) {
        return $t * 60;
    } elsif ("h" eq $unit) {
        return $t * 60 * 60;
    } elsif ("d" eq $unit) {
        return $t * 60 * 60 * 24;
    }
    return $LOG_TIME;
}

sub _cut {
    my $line = shift;
    my $max_line = 160;
    if (length $line > $max_line) {
        return (substr $line, 0, $max_line - 3) . "...";
    }
    return $line;
}
