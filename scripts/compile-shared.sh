#!/bin/bash

set -e
set -x

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"/..

clojure -X:depstar-ffi

INITIALIZE_AT_BUILD_TIME='clojure,clojure.stacktrace,tech.v3.datatype,sci.impl,babashka.nrepl,babashka.impl,hiccup,org.httpkit,tech.v3.resource,bencode,flatland.ordered,com.phronemophobic,com.phronemophobic.mobiletest,babashka.nrepl.impl,datascript,edamame.impl,tech.v3,tech.v3.parallel,sci.addons,tech.v3.datatype.ffi,sci,primitive_math,primitive_math$unuse_primitive_operators,primitive_math$using_primitive_operators_QMARK_,primitive_math$use_primitive_operators,primitive_math$variadic_predicate_proxy,primitive_math$variadic_proxy,primitive_math$unuse_primitive_operators,primitive_math$using_primitive_operators_QMARK_,primitive_math$use_primitive_operators,primitive_math$variadic_predicate_proxy,primitive_math$variadic_proxy,membrane.example,membrane,com.rpl.specter,riddley,com.rpl'


time \
    $GRAALVM_HOME/bin/native-image \
    --report-unsupported-elements-at-runtime \
    --initialize-at-build-time="$INITIALIZE_AT_BUILD_TIME" \
    --no-fallback \
    --no-server \
    -jar ./target/clj-libffi-uber.jar \
    --native-compiler-options='-L/opt/local/lib' \
    --native-compiler-options='-lffi' \
    --native-compiler-options="$DIR"'/../csource/clj_ffi.o' \
    -H:+ReportExceptionStackTraces \
    -J-Dclojure.spec.skip-macros=true \
    -J-Dclojure.compiler.direct-linking=true \
    -J-Dtech.v3.datatype.graal-native=true \
    -H:Name=./ffi_test
