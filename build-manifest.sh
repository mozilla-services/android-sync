#!/bin/sh

# Or use the one in mozilla-central.
PREPROCESSOR=tools/Preprocessor.py
python $PREPROCESSOR AndroidManifest.xml.in > AndroidManifest.xml
