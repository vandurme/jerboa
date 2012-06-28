#!/usr/bin/perl

# Benjamin Van Durme, vandurme@cs.jhu.edu, 28 Dec 2011

# Purpose: takes a model.logistic file, and feature-map.tsv, spits out a two
# column version where the features are aligned to their model weights

$model = shift;
$feature_map = shift;

open IN, $model;
while (<IN>) {
    chomp();
    $table{($.-6)} = $_;
}
close IN;

open IN, $feature_map;
while (<IN>) {
    chomp();
    @toks = split(/\t/);
    print $toks[0] . "\t" . $table{$toks[1]};
    print "\n";
}
close IN;
