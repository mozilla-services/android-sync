#!/usr/bin/env python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

r'''
Substitute preprocessor definitions into Android services files.
'''

from __future__ import print_function

import ConfigParser
import io
import os
import shutil
import sys
import time
import getpass

from tools.argparse_importer import argparse
from tools.preprocessor_helper import preprocess

def get_defines(config_files):
    '''
    Parse list of configuration files and return dictionary of
    preprocessor definitions.

    The file names in `config_files` are parsed in order, so later
    files override earlier files.
    '''
    cp = ConfigParser.SafeConfigParser()
    cp.optionxform = str # preserve case of keys

    # these default values are available for interpolation only
    defaults = {
        "USERNAME": getpass.getuser(),
        "TIMESTAMP": str(int(time.time())),
        }
    # set defaults after optionxform so that key case is preserved
    for k in defaults:
        cp.set("DEFAULT", k, defaults[k])

    cp.read(config_files)

    # extract definitions from configuration and then remove the
    # defaults that were set above (defaults are interpolation only)
    defines = dict(cp.items("defines"))
    for k in defaults:
        del defines[k]

    return defines

def _parse_args(args):
    parser = argparse.ArgumentParser(description="Preprocess Android services files.")
    group = parser.add_mutually_exclusive_group(required=False)
    group.add_argument("-q", "--quiet", dest="print_files", action="store_false",
                       default=True,
                       help="do not print output file names")
    group.add_argument("-v", "--verbose", dest="verbose", action="store_true",
                       default=False,
                       help="verbose output")
    parser.add_argument("-C", "--configs", nargs="+", metavar="INIFILE", dest="configs",
                        default=["preprocess.ini.default", "preprocess.ini"],
                        help="configuration file(s)")

    return parser.parse_args(args)

def _create_preprocess_ini(verbose):
    '''
    Create preprocess.ini if it does not already exist.

    Returns false if preprocess.ini already existed.
    '''
    if os.path.exists("preprocess.ini"):
        return False
    shutil.copy("preprocess.ini.default", "preprocess.ini")
    return True
    
def main():
    cmdargs = _parse_args(sys.argv[1:])

    if _create_preprocess_ini(cmdargs.verbose):
        if cmdargs.verbose:
            print("Wrote local preprocess.ini definitions.")

    if cmdargs.verbose:
        print("Reading preprocessor definitions from %s (later files override earlier files)." % cmdargs.configs)

    defines = get_defines(cmdargs.configs)

    if cmdargs.verbose:
        for k in sorted(defines.keys()):
            print("%s = %s" % (k, defines[k])) 

    java_dot_ins = [
        "src/main/java/org/mozilla/gecko/AppConstants.java.in",
        ]

    other_dot_ins = [
        "AndroidManifest.xml.in",
        "test/AndroidManifest.xml.in",
        "README.rst.in",
        ]

    dot_templates = {
        "strings/strings.xml.template":    "res/values/strings.xml",
        }

    for input_filename in java_dot_ins:
        if not input_filename.endswith('.in'):
            raise Exception("filename %s should end in .in" % input_filename)
        output_filename = input_filename[:-3]
        preprocess(input_filename, output_filename, defines, True, "//#")
        if cmdargs.verbose:
            print("Preprocessing %s to %s" % (input_filename, output_filename))
        else:
            if cmdargs.print_files:
                print("%s" % output_filename)

    for input_filename in other_dot_ins:
        if not input_filename.endswith('.in'):
            raise Exception("filename %s should end in .in" % input_filename)
        output_filename = input_filename[:-3]
        preprocess(input_filename, output_filename, defines)
        if cmdargs.verbose:
            print("Preprocessing %s to %s" % (input_filename, output_filename))
        else:
            if cmdargs.print_files:
                print("%s" % output_filename)

    for input_filename in dot_templates:
        if not input_filename.endswith('.template'):
            raise Exception("filename %s should end in .template" % input_filename)
        output_filename = dot_templates[input_filename]
        preprocess(input_filename, output_filename, defines)
        if cmdargs.verbose:
            print("Preprocessing %s to %s" % (input_filename, output_filename))
        else:
            if cmdargs.print_files:
                print("%s" % output_filename)

if __name__ == "__main__":
    main()
