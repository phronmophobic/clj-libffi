#!/bin/bash

set -e
set -x

clojure -M -m ffitest.genc

cc \
    -dynamiclib \
    -arch x86_64 \
    ffitestlib.c \
    -o libffitestlib.dylib
