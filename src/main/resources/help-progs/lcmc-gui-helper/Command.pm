package Command;

our $COMMAND_ERRNO; # is set in _exec function

sub _system {
    my $cmd = shift;
    my $level = shift // 1;
    return _execute($cmd, $level, 0);
}

sub _exec_or_die {
    my $result = _exec(shift);
    my $ret = $?;
    exit $ret if $ret != 0;
    return $result;
}

sub _exec {
    my $cmd = shift;
    my $level = shift // 1;
    return _execute($cmd, $level, 1);
}

sub _execute {
    my $cmd = shift;
    if (!$cmd) {
        Log::_log("ERROR: $0 unspecified command: line: " . (caller(1))[2], 1);
    }
    my $level = shift;
    my $wantoutput = shift;

    my $cmd_log = $cmd;
    $cmd_log =~ s/\n/ \\n /g;

    my $cmd_code = sprintf("%05x", rand(16 ** 5));
    my $start_time = Log::_log_time();
    Log::_log($start_time . " start $cmd_code: $cmd_log", $level);
    my @out;
    my $out;
    if ($wantoutput) {
        if (wantarray) {
            @out = `$cmd`;
        }
        else {
            $out = `$cmd` || "";
        }
    }
    else {
        $out = system($cmd);
    }
    $COMMAND_ERRNO = $?;
    my $end_time = Log::_log_time();
    Log::_log($end_time . " done  $cmd_code: $cmd_log", $level);
    if ($wantoutput) {
        if (wantarray) {
            return @out;
        }
        else {
            return $out;
        }
    }
    else {
        return $out;
    }
}
