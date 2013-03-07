#!/usr/bin/python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

r"""
Re-package Nightly Fennec Native builds for Android Services developers.

Signs and aligns an unsigned and unaligned Gecko APK with an Android
debug key.
"""

from __future__ import print_function

import os
import errno
import sys
import logging

import shutil
import tempfile
import subprocess

from argparse_importer import argparse

class FennecRepackager:
    def __init__(self, input_filename, logger=None):
        self.input_filename = input_filename

        if logger is not None:
            self.logger = logger
        else:
            self.logger = logging.getLogger()

    def repackage(self, output_filename):
        self.logger.info("Re-packaging %s to %s." % (self.input_filename, output_filename))

        temp_dir = tempfile.mkdtemp()
        self.logger.info("Created temporary directory %s." % temp_dir)

        try:
            gecko = self._repackage(temp_dir)
            self.logger.info("Re-packaged %s." % gecko)

            shutil.copy(gecko, output_filename)
            self.logger.info("Wrote to %s." % output_filename)
        finally:
            if temp_dir is not None:
                shutil.rmtree(temp_dir)
                self.logger.info("Deleted temporary directory %s." % temp_dir)

    def _ensure_debugkeystore(self):
        path = os.path.expanduser("~/.android")

        try:
            os.makedirs(path)
        except OSError as exception:
            if exception.errno != errno.EEXIST:
                raise

        keystore = os.path.join(path, "debug.keystore")

        if not os.path.exists(keystore):
            # we can create an android debug keystore.
            args = [ "keytool",
                     "-genkey",
                     "-v",
                     "-keystore", keystore,
                     "-storepass", "android",
                     "-alias", "androiddebugkey",
                     "-keypass", "android",
                     "-dname", "CN=Android Debug,O=Android,C=US" ]
            subprocess.check_call(args)

        return keystore

    def _repackage(self, temp_dir):
        gecko_u = os.path.join(temp_dir, "gecko.u.apk")
        shutil.copy(self.input_filename, gecko_u)

        keystore = self._ensure_debugkeystore()

        # jarsigner updates APK in place.
        args = [ "jarsigner",
                 "-digestalg", "SHA1",
                 "-sigalg", "MD5withRSA",
                 "-keystore", keystore,
                 "-storepass", "android",
                 gecko_u,
                 "androiddebugkey" ]
        subprocess.check_call(args)

        # zipalign outputs a new file.
        gecko = os.path.join(temp_dir, "gecko.apk")
        args = [ "zipalign",
                 "-f", "4",
                 gecko_u,
                 gecko ]
        subprocess.check_call(args)

        return gecko

if __name__ == '__main__':
    # parse command line arguments

    parser = argparse.ArgumentParser(description="Re-package Nightly Fennec Native builds for Android Services developers.")
    parser.add_argument("-v", dest="verbose", action="store_true", default=False, help="verbose output")
    parser.add_argument("input", default="gecko-unsigned-unaligned.apk", help="Unsigned and unaligned APK filename.")
    parser.add_argument("output", default="gecko.apk", help="Signed and aligned APK filename.")
    cmdargs = parser.parse_args(sys.argv[1:])

    logger = logging.getLogger('fennec_repackager')
    ch = logging.StreamHandler()
    logger.addHandler(ch)

    if cmdargs.verbose:
        logger.setLevel(logging.INFO)
        ch.setLevel(logging.INFO)

    frp = FennecRepackager(cmdargs.input, logger=logger)
    frp.repackage(cmdargs.output)
