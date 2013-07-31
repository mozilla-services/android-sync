#!/usr/bin/env python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

import errno
import os
from StringIO import StringIO

# Copied from mozbuild.util.

def ensureParentDir(path):
    """Ensures the directory parent to the given file exists."""
    d = os.path.dirname(path)
    if d and not os.path.exists(path):
        try:
            os.makedirs(d)
        except OSError, error:
            if error.errno != errno.EEXIST:
                raise

class FileAvoidWrite(StringIO):
    """File-like object that buffers output and only writes if content changed.

    We create an instance from an existing filename. New content is written to
    it. When we close the file object, if the content in the in-memory buffer
    differs from what is on disk, then we write out the new content. Otherwise,
    the original file is untouched.
    """
    def __init__(self, filename):
        StringIO.__init__(self)
        self.filename = filename

    def close(self):
        """Stop accepting writes, compare file contents, and rewrite if needed.

        Returns a tuple of bools indicating what action was performed:

            (file existed, file updated)
        """
        buf = self.getvalue()
        StringIO.close(self)
        existed = False
        try:
            existing = open(self.filename, 'rU')
            existed = True
        except IOError:
            pass
        else:
            try:
                if existing.read() == buf:
                    return True, False
            except IOError:
                pass
            finally:
                existing.close()

        ensureParentDir(self.filename)
        with open(self.filename, 'w') as file:
            file.write(buf)

        return existed, True

    def __enter__(self):
        return self
    def __exit__(self, type, value, traceback):
        self.close()
