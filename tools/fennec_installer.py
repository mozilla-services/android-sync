#!/usr/bin/python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

r"""
Download and install Nightly Fennec Native builds for Android Services developers.

Downloads unsigned and unaligned Gecko APKs, then signs them with
Android debug key, installs them onto device, and launches to
initialize databases for testing.
"""

from __future__ import print_function

import os
import errno
import sys

import logging
import argparse
import shutil
import tempfile
import subprocess
import urllib2

from fennec_repackager import FennecRepackager

DEFAULT_URL = "https://ftp.mozilla.org/pub/mozilla.org/mobile/nightly/latest-mozilla-central-android/gecko-unsigned-unaligned.apk"
ADB = "adb"

ACTIVITY_PACKAGE = "org.mozilla.fennec"
ACTIVITY_CLASS = ".App"

class FennecInstaller:
    def __init__(self, url, logger=None):
        self.url = url

        if logger is not None:
            self.logger = logger
        else:
            self.logger = logging.getLogger()

    def download(self, output_filename):
        request = urllib2.Request(self.url)
        response = urllib2.urlopen(request)

        self.logger.info("Downloading " + self.url + "...")
        with open(output_filename, 'wb') as fp:
            shutil.copyfileobj(response, fp)
        self.logger.info("Downloading " + self.url + "... done.")
        self.logger.info("Downloaded " + output_filename + ".")

    def install(self, filename):
        # install APK.
        self.logger.info("Installing " + filename + "...")
        args = [ ADB,
                 "install", "-r",
                 filename ]
        subprocess.check_call(args)
        self.logger.info("Installing " + filename + "... done.")

    def launch(self, activity_package=ACTIVITY_PACKAGE, activity_class=ACTIVITY_CLASS):
        # launch activity, so that all databases are initialized correctly.
        activity = activity_package + "/" + activity_class

        self.logger.info("Launching " + activity + "...")
        args = [ ADB,
                 "shell", "am", "start",
                 "-a", "android.intent.action.MAIN",
                 "-n", activity ]
        subprocess.check_call(args)
        self.logger.info("Launched " + activity + ".")

    def test(self):
        self.logger.info("Downloading " + self.url + "...")

if __name__ == '__main__':
    # parse command line arguments
    parser = argparse.ArgumentParser(description="Download and install Nightly Fennec Native builds for Android Services developers.")
    parser.add_argument("-v", dest="verbose", action="store_true", default=False, help="verbose output")
    parser.add_argument("-u", "--url", default=DEFAULT_URL, help="URL of unsigned and unaligned APK to download and install.")
    parser.add_argument("-o", "--output", default="gecko.apk", help="Signed and aligned APK filename.")
    parser.add_argument("--package", dest="activity_package", default=ACTIVITY_PACKAGE, help="Android package containing Activity to launch.")
    parser.add_argument("--class", dest="activity_class", default=ACTIVITY_CLASS, help="Java class name of Activity to launch.")
    cmdargs = parser.parse_args(sys.argv[1:])

    logger = logging.getLogger('fennec_repackager')
    ch = logging.StreamHandler()
    logger.addHandler(ch)

    if cmdargs.verbose:
        logger.setLevel(logging.INFO)
        ch.setLevel(logging.INFO)
        
    with tempfile.NamedTemporaryFile() as temp_file:
        installer = FennecInstaller(cmdargs.url, logger=logger)
        frp = FennecRepackager(temp_file.name, logger=logger)

        installer.download(temp_file.name)

        frp.repackage(cmdargs.output)

        installer.install(cmdargs.output)
        installer.launch(cmdargs.activity_package, cmdargs.activity_class)
