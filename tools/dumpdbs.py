#!/usr/bin/python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

import argparse
import sys
import datetime
import time
import subprocess
import getpass
import os

# parse command line arguments
parser = argparse.ArgumentParser(description='Pull and dump Fennec databases from devices.')
parser.add_argument('-v', dest='verbose', action='store_true', default=False, help='verbose output')
parser.add_argument('-P', dest='profile', default='default', help='profile')
parser.add_argument('-d', dest='db',    default=None, help='database to dump')
parser.add_argument('-t', dest='table', default=None, help='table to dump')
args = parser.parse_args(sys.argv[1:])

DATETIME  = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
TIMESTAMP = int(time.time())

WHOAMI     = getpass.getuser()
ROOT       = "/data/data/org.mozilla.fennec_%s/files/mozilla" % WHOAMI

OUTPUT_DIR = "/sdcard"
TEMP_DIR   = "/tmp"
ADB        = "adb"
SQLITE     = "sqlite3"

TABLES = [
    ("browser.db", "bookmarks"),
    ("tabs.db", "tabs"),
    ("tabs.db", "clients"),
    ("signons.sqlite", "moz_deleted_logins"),
    ("signons.sqlite", "moz_logins"),
    # ("permissions.db", "permissions"),
    # ("browser.db", "history"), # too much?
    ("formhistory.sqlite", "moz_deleted_formhistory"),
    ("formhistory.sqlite", "moz_formhistory"), ]

if args.db and args.table:
    TABLES = [ (args.db, args.table) ]
elif args.db:
    TABLES = [ (k, v) for (k, v) in TABLES if args.db in k ] # filter
elif args.table:
    TABLES = [ (k, v) for (k, v) in TABLES if args.table in v ] # filter

HTML_HEADER = """<html>
<head>
<title>dumpdbs at %s (%s)</title>
<style>
table {
  border-collapse: collapse;
}
td, th {
  border: 1px solid LightGray;
}
</style>
</head>
<body>
<h1>dumpdbs at %s (%s)</h1>
""" % (DATETIME, TIMESTAMP, DATETIME, TIMESTAMP)
HTML_FOOTER = """</body>
</html>"""

HTML_TABLE_HEADER = """<table>"""
HTML_TABLE_FOOTER = """</table>"""

output = subprocess.check_output([ADB, 'shell', 'run-as org.mozilla.fennec_%s ls %s' % (WHOAMI, ROOT)])

MANGLED_PROFILE = None
for line in output.split():
    line = line.strip()
    if not line:
        continue
    if not args.profile in line:
        continue
    MANGLED_PROFILE = line

if not MANGLED_PROFILE:
    print >> sys.stderr, "Couldn't find profile '%s'" % args.profile
    exit(1)
else:
    if args.verbose:
        print >> sys.stderr, "Found profile '%s'" % (MANGLED_PROFILE)

FILES_TO_COPY = set([ k for k, _ in TABLES ])
COPIED = {}

if args.verbose:
    print >> sys.stderr, "Copying %s files..." % len(FILES_TO_COPY)
for FILE in FILES_TO_COPY:
    I = "%s/%s/%s" % (ROOT, MANGLED_PROFILE, FILE) # file on device, not pull-able
    O = "%s/%s-%s" % (OUTPUT_DIR, FILE, TIMESTAMP) # file on device, pull-able
    L = "%s/%s-%s" % (TEMP_DIR, FILE, TIMESTAMP)   # file in temp storage on desktop
    try:
        print >> sys.stderr, subprocess.check_output([ADB, "shell", "run-as org.mozilla.fennec_%s dd if=%s of=%s" % (WHOAMI, I, O)]) # move it to pull-able
        print >> sys.stderr, subprocess.check_output([ADB, "pull", O, L]) # pull it
    except:
        if args.verbose:
            print >> sys.stderr, "Couldn't dd and pull %s" % FILE
        continue

    COPIED[FILE] = True
    try:
        print >> sys.stderr, subprocess.check_output([ADB, "shell", "rm -f %s" % O]) # delete pull-able file
    except:
        if args.verbose:
            print >> sys.stderr, "Couldn't rm %s" % O
        continue

if args.verbose:
    print >> sys.stderr, "Copying %s files... DONE" % len(FILES_TO_COPY)
    print >> sys.stderr, "Copied %s of %s files." % (len(COPIED), len(FILES_TO_COPY))

print HTML_HEADER

for (db, table) in TABLES:
    if not COPIED.has_key(db):
        continue

    SQL = "select * from %s;" % table
    L = "%s/%s-%s" % (TEMP_DIR, db, TIMESTAMP)   # file in temp storage on desktop
    try:
        output = subprocess.check_output([SQLITE, "-html", "-header", L, SQL])
    except:
        if args.verbose:
            print >> sys.stderr, "Couldn't select * from %s in db %s" % (table, db)
        continue

    print "<div>"
    print "<h2>%s</h2>" % SQL
    print HTML_TABLE_HEADER
    print output
    print HTML_TABLE_FOOTER
    print "</div>"

print HTML_FOOTER

for db in COPIED.keys():
    L = "%s/%s-%s" % (TEMP_DIR, db, TIMESTAMP)   # file in temp storage on desktop
    try:
        os.remove(L)
    except OSError:
        if args.verbose:
            print >> sys.stderr, "Couldn't rm %s" % L
