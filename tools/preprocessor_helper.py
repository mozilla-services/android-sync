#!/usr/bin/env python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

r'''
High level interface to Preprocessor.
'''

from Preprocessor import Preprocessor
from util import FileAvoidWrite

import os

def preprocess(input_filename, output_filename, defines, makedirs=True, marker="#"):
    '''
    Preprocess `input_filename` into `output_filename`, substituting
    definitions from `defines`.

    Directories needed to write output file will be created if
    `makedirs` is truthy.
    '''
    pp = Preprocessor()
    pp.context.update(defines)
    pp.setLineEndings("lf")
    pp.setMarker(marker)
    pp.do_filter("substitution")

    # make sure we can actually write to output directory
    if makedirs:
        dirname = os.path.dirname(output_filename)
        if dirname and not os.path.exists(dirname):
            os.makedirs(dirname)

    # Avoid writing unchanged files.  This trades memory (since output
    # is buffered) to save disk access (refreshes in Eclipse and
    # recompiles in Maven).
    with FileAvoidWrite(output_filename) as fo:
        pp.out = fo
        with open(input_filename, "rt") as fi:
            pp.do_include(fi)
