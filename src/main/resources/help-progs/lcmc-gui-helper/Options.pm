package Options;
# options: --log, ...
# action options: all, get-cluster-events ...
sub parse {
    my $args = shift;
    my %options;
    my @action_options;
    for (@$args) {
        if ($_ !~ /^--/) {
            # old options
            push @action_options, $_;
            next;
        }
        if (/=/) {
            my @parts = split /=/, $_, 2;
            $options{$parts[0]} = $parts[1];
        }
        else {
            $options{$_} = 1;
        }
    }
    return(\%options, \@action_options);
}
