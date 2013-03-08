#!/usr/bin/python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

r'''
High level interface to Preprocessor.
'''

from Preprocessor import Preprocessor
import os

def preprocess(input_filename, output_filename, defines, makedirs=True):
    '''
    Preprocess `input_filename` into `output_filename`, substituting
    definitions from `defines`.

    Directories needed to write output file will be created if
    `makedirs` is truthy.
    '''
    pp = Preprocessor()
    pp.context.update(defines)
    pp.setLineEndings("lf")
    pp.setMarker("#")
    pp.do_filter("substitution")

    # make sure we can actually write to output directory
    if makedirs:
        dirname = os.path.dirname(output_filename)
        if dirname and not os.path.exists(dirname):
            os.makedirs(dirname)

    with open(output_filename, "wt") as fo:
        pp.out = fo
        with open(input_filename, "rt") as fi:
            pp.do_include(fi)
