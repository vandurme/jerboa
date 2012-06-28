#!/usr/bin/perl -w

# Benjamin Van Durme, vandurme@cs.jhu.edu,  9 Aug 2011

# Purpose: takes a file in David Yarowsky's format, reads in all unique
# communicants, splits them into train/dev/test, writes all the communications
# for those entries into separate files.

$train = 0.9;
$dev = 0.01;
# test is what remains

$input = shift;
$prefix = shift;

open IN, $input;
$in_message = 0;

while (<IN>) {
    if (/^<message/) {
	/communicant=(\S+)/;
	$communicant_table{$1} = 1;
    }
}
close IN;

@communicants = keys(%communicant_table);
## shuffle three times for good measure
for ($i = 0; $i <= $#communicants; $i++) {
    $tmp = $communicants[$i];
    $j = $i + int(rand($#communicants + 1 - $i));
    $communicants[$i] = $communicants[$j];
    $communicants[$j] = $tmp;
}
@communicants = reverse @communicants;
for ($i = 0; $i <= $#communicants; $i++) {
    $tmp = $communicants[$i];
    $j = $i + int(rand($#communicants + 1 - $i));
    $communicants[$i] = $communicants[$j];
    $communicants[$j] = $tmp;
}
@communicants = reverse @communicants;
for ($i = 0; $i <= $#communicants; $i++) {
    $tmp = $communicants[$i];
    $j = $i + int(rand($#communicants + 1 - $i));
    $communicants[$i] = $communicants[$j];
    $communicants[$j] = $tmp;
}


$train_boundary = int(($#communicants-1) * $train);
$dev_boundary = $train_boundary + int(($#communicants-1) * $dev);
$test_boundary = $#communicants;
#print "train boundary: $train_boundary   dev boundary: $dev_boundary   test boundary: $test_boundary\n";
foreach $i (0 .. $train_boundary) {
    $train_table{$communicants[$i]} = 1;
}
foreach $i ($train_boundary .. $dev_boundary) {
    $dev_table{$communicants[$i]} = 1;
}
foreach $i ($dev_boundary .. $test_boundary) {
    $test_table{$communicants[$i]} = 1;
}

`rm $prefix.train`;
`rm $prefix.dev`;
`rm $prefix.test`;
open IN, $input;
while (<IN>) {
    if (/^<message/) {
	$in_message = 1;
	/communicant=(\S+)/;
	$communicant = $1;
	if (defined($train_table{$communicant})) {
	    open OUT, ">>$prefix.train";
	} elsif (defined($dev_table{$communicant})) {
	    open OUT, ">>$prefix.dev";
	} elsif (defined($test_table{$communicant})) {
	    open OUT, ">>$prefix.test";
	} else {
	    die "ERROR: Should never be here";
	}
	print OUT;
    } elsif (/^<\/message/) {
	print OUT;
	close OUT;
	$in_message = 0;
    } else {
	print OUT;
    }
}
close IN;


