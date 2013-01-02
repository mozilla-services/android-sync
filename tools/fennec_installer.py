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

ARM_URL = "https://ftp.mozilla.org/pub/mozilla.org/mobile/nightly/latest-mozilla-central-android/gecko-unsigned-unaligned.apk"
X86_URL = "https://ftp.mozilla.org/pub/mozilla.org/mobile/nightly/latest-mozilla-central-android-x86/gecko-unsigned-unaligned.apk"
DEFAULT_URL = ARM_URL

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
    parser = argparse.ArgumentParser(description="Download, install, and launch Nightly Fennec Native builds.")
    parser.add_argument("-v", dest="verbose", action="store_true", default=True, help="verbose output")
    parser.add_argument("-q", dest="verbose", action="store_false", help="quiet output")

    group = parser.add_mutually_exclusive_group()
    group.add_argument("-u", "--url", dest="url", default=DEFAULT_URL, help="URL of unsigned and unaligned APK to download.")
    group.add_argument("--arm", dest="url", action="store_const", const=ARM_URL, help="Download ARM APK.")
    group.add_argument("--x86", dest="url", action="store_const", const=X86_URL, help="Download x86 APK.")

    parser.add_argument("-o", "--output", default="gecko.apk", help="Signed and aligned APK filename.")
    parser.add_argument("--package", dest="activity_package", default=ACTIVITY_PACKAGE, help="Android package containing Activity to launch.")
    parser.add_argument("--class", dest="activity_class", default=ACTIVITY_CLASS, help="Java class name of Activity to launch.")

    subparsers = parser.add_subparsers(dest="action")
    parser_all      = subparsers.add_parser("dil", help="download, install, and launch.")
    parser_download = subparsers.add_parser("d", help="download.")
    parser_install  = subparsers.add_parser("i", help="install.")
    parser_launch   = subparsers.add_parser("l", help="launch.")

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

        if 'd' in cmdargs.action:
            installer.download(temp_file.name)
            frp.repackage(cmdargs.output)

        if 'i' in cmdargs.action:
            installer.install(cmdargs.output)

        if 'l' in cmdargs.action:
            # Future: `aapt dump badging gecko.apk` will tell us the
            # "package" and "launchable-activity".
            installer.launch(cmdargs.activity_package, cmdargs.activity_class)
