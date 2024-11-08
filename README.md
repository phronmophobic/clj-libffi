# clj-libffi

A wrapper for libffi.

## Rationale

On the jvm, using clj-libffi is worse than other similar options like jna and jdk16's ffi. However, when compiling to native using graalvm, only static calls to c libraries are possible (afaik). Using libffi, loading and calling arbitrary functions from arbitrary shared libraries can be achieved.

## Deps

Leiningen/Boot

`[com.phronemophobic/clj-lbbffi "1.4"]`

Clojure CLI/deps.edn

`com.phronemophobic/clj-libffi {:mvn/version "1.4"}`

## Usage

```clojure

(require '[com.phronemophobic.clj-libffi :as ffi])

(ffi/call "cos" :float64 :float64 Math/PI)
;; -1.0

(ffi/load-library "libmy.dylib")

(ffi/call "myfn" :void
          :int64 42
          :int8 -1)

```

### Graalvm

To compile for graalvm, you must link against libffi. See [examples/ffitest/scripts/compile-test.sh](examples/ffitest/scripts/compile-test.sh).

### Limitations

* Currently only runs on Mac OSX. If you'd like to see support for other platforms, please comment or upvote the issues for [linux](https://github.com/phronmophobic/clj-libffi/issues/1) and [windows](https://github.com/phronmophobic/clj-libffi/issues/2).

## License

Copyright © 2021 Adrian Smith

Distributed under the Eclipse Public License version 1.0.
