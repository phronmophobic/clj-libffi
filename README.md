# clj-libffi

A wrapper for libffi.

## Rationale

On the jvm, using clj-libffi is worse than other similar options like jna and jdk16's ffi. However, when compiling to native using graalvm, only static calls to c libraries are possible (afaik). Using libffi, loading and calling arbitrary functions from arbitrary shared libraries can be achieved.

## Usage

```clojure

(ffi/call "cos" :float64 :float64 Math/PI)
;; -1.0

(ffi/load-library "libmy.dylib>")

(ffi/call "myfn" :void
          :int64 42
          :int8 -1)

```

### Graalvm

To compile for graalvm, you must link against libffi and `csource/clj_ffi.o`. See `examples/ffitest`.

### Limitations

* Currently only tested on Mac OSX.
* No support for passing structs by value yet.

## License

Copyright © 2021 Adrian Smith

Distributed under the Eclipse Public License version 1.0.
