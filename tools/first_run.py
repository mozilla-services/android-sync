#!/usr/bin/env python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

r'''
Configure android-sync repository for development.

Creates Eclipse projects.  Use File > Import... > Existing Projects
into Workspace.
'''

from __future__ import print_function

import os
import shutil
import sys

from argparse_importer import argparse

def eclipsify(topsrcdir, eclipse_directories, print_files=True, verbose=False):
    r'''Create Eclipse project files.

Writes .project and .classpath into directories for import into
Eclipse.  Use File > Import... > Existing Projects into Workspace.'''

    # Preprocess input file named key to output file named value in
    # each Eclipse directory.
    eclipse_filenames = {
        'example.project': '.project',
        'example.classpath': '.classpath',
        }

    if print_files or verbose:
        print('Creating Eclipse project files.')

    for directory in eclipse_directories:
        for (input_filename, output_filename) in eclipse_filenames.items():
            input_filename = os.path.join(directory, input_filename)
            output_filename = os.path.join(directory, output_filename)

            # Shouldn't need to make directories here.
            shutil.copy(os.path.join(topsrcdir, input_filename),
                        os.path.join(topsrcdir, output_filename))

            if verbose:
                print('Copying %s to %s' % (input_filename, output_filename))

        if not verbose and print_files:
            print('%s' % directory)

def _determine_topsrcdir():
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

if __name__ == '__main__':
    # parse command line arguments
    parser = argparse.ArgumentParser(description='Configure android-sync repository for development.')
    group = parser.add_mutually_exclusive_group(required=False)
    group.add_argument('-q', '--quiet', dest='print_output', action='store_false',
                       default=True,
                       help='do not print output')
    group.add_argument('-v', '--verbose', dest='verbose', action='store_true',
                       default=False,
                       help='verbose output')

    eclipse_group = parser.add_mutually_exclusive_group(required=False)
    eclipse_group.add_argument('-e', '--eclipse', dest='eclipse', action='store_true',
                               default=True,
                               help='write Eclipse project files')
    eclipse_group.add_argument('-E', '--no-eclipse', dest='eclipse', action='store_false',
                               help='do not write Eclipse project files')

    cmdargs = parser.parse_args(sys.argv[1:])

    topsrcdir = _determine_topsrcdir()

    if (cmdargs.verbose):
        print('Using TOPSRCDIR %s' % topsrcdir)

    if (cmdargs.eclipse):
        # Directories that should be filled with Eclipse project files.
        eclipse_directories = [
            '.', # root directory
            'test',
            'bagheera-client-test',
            ]
        eclipsify(topsrcdir, eclipse_directories, cmdargs.print_output, cmdargs.verbose)

        if cmdargs.print_output or cmdargs.verbose:
            print(r'''
Now run `mvn -D"/path/to/workspace" eclipse:configure-workspace` and
use File > Import... > Existing Projects into Workspace to add the
created projects to your Eclipse workspace.''')
