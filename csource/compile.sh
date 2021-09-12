#!/bin/bash

set -e
set -x

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

cc \
    `pkg-config --static --libs --cflags libffi` \
    -l ffi \
    -dynamiclib \
    -arch x86_64 \
    clj_ffi.c \
    -o libcljffi.dylib


cc \
    `pkg-config --cflags libffi` \
    -arch x86_64 \
    clj_ffi.c \
    -c \
    -o clj_ffi.o

cc \
    `pkg-config --static --libs --cflags libffi` \
    -l ffi \
    -DTEST \
    -arch x86_64 \
    clj_ffi.c \
    -o ffi_test

cp libcljffi.dylib ../resources

