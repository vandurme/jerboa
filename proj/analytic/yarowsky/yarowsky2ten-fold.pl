#!/usr/bin/perl -w

# Benjamin Van Durme, vandurme@cs.jhu.edu,  9 Aug 2011

# Purpose: takes a file in David Yarowsky's format, reads in all unique
# communicants, splits them into 10 directories for 10-fold cross validation.

$input = shift;

open IN, $input;

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

$unit = int(0.1 * ($#communicants+1));
for ($i = 0; $i < 10; $i++) {
    my %test_table;
    if ($i < 9) {
	foreach $j (($i*$unit) .. ((($i+1)*$unit) - 1)) {
	    $test_table{$communicants[$j]} = 1;
	}
    } else {
	foreach $j ((9*$unit) .. $#communicants) {
	    $test_table{$communicants[$j]} = 1;
	}
    }
    `mkdir -p $i`;
    open TRAIN, ">$i/data.train.txt";
    open TEST, ">$i/data.test.txt";
    open IN, $input;
    $in_test = 0;
    while (<IN>) {
	if (/^<message/) {
	    /communicant.(\S+)/;
	    $communicant = $1;
	    if (defined($test_table{$communicant})) {
		print TEST;
		$in_test = 1;
	    } else {
		print TRAIN;
	    }
	} elsif (/^<\/message/) {
	    if ($in_test) {
		print TEST;
		$in_test = 0;
	    } else {
		print TRAIN;
	    }
	} else {
	    if ($in_test) {
		print TEST;
	    } else {
		print TRAIN;
	    }
	}
    }
    close IN;
}
