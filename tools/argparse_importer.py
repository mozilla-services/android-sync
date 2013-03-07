# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this file,
# You can obtain one at http://mozilla.org/MPL/2.0/.

r"""
argparse is distributed only with Python 2.7 and above. ci.mozilla.org
is running an ancient Python, so fall back to the version of argparse
that we distribute.
"""

try:
    import argparse as argparse
except ImportError:
    import os
    import sys
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "argparse-1.2.1")
    sys.path.append(path)
    import argparse as argparse
