#!/usr/bin/python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

import argparse
import sys
import os
import re

# parse command line arguments
parser = argparse.ArgumentParser(description='Update headers in source files.')
parser.add_argument('-l', dest='first_line', required=True, help='source file contents before FIRST_LINE will be replaced ...')
parser.add_argument('-f', dest='header_file', required=True, help='... with contents of HEADER_FILE')
parser.add_argument('-x', dest='exclude_pat', default='', help='ignore source files matching EXCLUDE_PAT')
parser.add_argument('-d', dest='dry_run', action='store_true', default=False, help='do not write updated source files')
parser.add_argument('-v', dest='verbose', action='store_true', default=False, help='verbose output')
parser.add_argument('-s', dest='print_summary', default=True, help='print summary')
parser.add_argument('files', nargs='*', help='files and directories to update')

args = parser.parse_args(sys.argv[1:])

FIRSTLINE_REGEXP = re.compile(args.first_line)

EXCLUDE_REGEXP = False # we use if EXCLUDE_REGEXP below
if args.exclude_pat:
    EXCLUDE_REGEXP = re.compile(args.exclude_pat)

HEADER = file(args.header_file, "rt").read()

ERROR = 1
CORRECT = 2
REPLACED = 3
SKIPPED = 4

stats = {} # filename -> ERROR | CORRECT | REPLACED | SKIPPED

def update_file_named(filename):
    if EXCLUDE_REGEXP and EXCLUDE_REGEXP.search(filename):
        stats[filename] = SKIPPED
        if (args.verbose):
            print filename + ": skipped."
        return

    fdata = file(filename, "r+").read()

    firstline = FIRSTLINE_REGEXP.search(fdata)
    if (firstline is None):
        stats[filename] = ERROR
        if (args.verbose):
            print filename + ": first line missing!"
        return

    pre_firstline = fdata[:firstline.start()]
    if pre_firstline == HEADER:
        stats[filename] = CORRECT
        if (args.verbose):
            print filename + ": header correct."
        return

    stats[filename] = REPLACED

    if args.dry_run:
        if (args.verbose):
            print filename + ": header needs to be updated."
        return

    fdata = HEADER + fdata[firstline.start():]
    file(filename, "w").write(fdata)

    if (args.verbose):
        print filename + ": header updated."

def recursive_traversal(filename):
    if os.path.isdir(filename):
        children = os.listdir(filename)
        for child in children:
            recursive_traversal(os.path.join(filename, child))
    else:
        update_file_named(filename)

for filename in args.files:
    recursive_traversal(filename)

# summary statistics
error = 0
correct = 0
replaced = 0
skipped = 0
for k, v in stats.items():
    if v == ERROR:
        error += 1
    if v == CORRECT:
        correct += 1
    if v == REPLACED:
        replaced += 1
    if v == SKIPPED:
        skipped += 1

if args.print_summary:
    if (args.dry_run):
        print "%d skipped, %d with first line missing, %d with header correct, %d with header needs to be updated." % (skipped, error, correct, replaced)
    else:
        print "%d skipped, %d with first line missing, %d with header correct, %d with header updated." % (skipped, error, correct, replaced)

if error > 0:
    exit(2)
elif replaced > 0:
    exit(1)
else:
    exit(0)
