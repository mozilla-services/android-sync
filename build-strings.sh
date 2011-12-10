#!/bin/sh

# Or use the one in mozilla-central.
PREPROCESSOR=tools/Preprocessor.py
python $PREPROCESSOR strings.xml.template > res/values/strings.xml
