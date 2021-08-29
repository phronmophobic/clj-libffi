(ns com.phronemophobic.clj-libffi
  (:require [tech.v3.datatype.ffi :as dt-ffi]))

(set! *warn-on-reflection* true)




(dt-ffi/define-library-interface
  {

   :callc {:rettype :void
           :argtypes [['fptr :pointer]
                      ['rettype :int32]
                      ['ret :pointer]
                      ['nargs :int32]
                      ['argtypes :pointer]
                      ['args :pointer]]}

   ,}
  :libraries (graal-native/if-defined-graal-native
              []
              ["ffi"])
  ;; :libraries ["foo.h"]
  ;; :header-files ["adsf"]
  ;; :classname 'Foo.Baz
  )


(defn call [])
