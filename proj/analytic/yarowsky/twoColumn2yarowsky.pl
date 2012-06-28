#!/usr/bin/perl

# Benjamin Van Durme, vandurme@cs.jhu.edu, 16 Sep 2011

# Purpose: takes a file of the form:
#   <text> <tab> <label>
# And converts it to: (where n  is the line number)
# <message communicant=x<n> attribute=<label>>
#  <text>
# </message>

# Usage: twoColum2yarowsky.pl file > output

while (<>) {
    chomp();
    ($text,$label) = split(/\t/);
    $label =~ s/^\+//;
    print "<message id=$. communicant=x$. attribute=$label>\n";
    print $text . "\n";
    print "</message>\n";
}
