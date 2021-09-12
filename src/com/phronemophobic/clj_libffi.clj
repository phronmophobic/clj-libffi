(ns com.phronemophobic.clj-libffi
  (:require [tech.v3.datatype.ffi :as dt-ffi]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.ffi.size-t :as ffi-size-t]
            [tech.v3.datatype.native-buffer :as native-buffer]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.datatype.graal-native :as graal-native]
            [tech.v3.datatype.ffi.graalvm-runtime :as graalvm-runtime])
  (:import [tech.v3.datatype.native_buffer NativeBuffer]
           [tech.v3.datatype.ffi Pointer])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn ^:private long->pointer [n]
  (Pointer. n))

(def RTLD_LAZY (int 0x1))
(def RTLD_NOW (int 0x2))
(def RTLD_LOCAL (int 0x4))
(def RTLD_GLOBAL (int 0x8))


(def RTLD_NOLOAD (int 0x10))
(def RTLD_NODELETE (int 0x80))
(def RTLD_FIRST (int 0x100))


(def RTLD_NEXT (long->pointer -1)) ;;   /* Search subsequent objects. */
(def RTLD_DEFAULT (long->pointer -2)) ;;   /* Use default search algorithm. */
(def RTLD_SELF (long->pointer -3)) ;;   /* Search this and subsequent objects (Mac OS X 10.5 and later) */
(def RTLD_MAIN_ONLY (long->pointer -5)) ;;   /* Search main executable only (Mac OS X 10.5 and later) */

(def FFI_DEFAULT_ABI (int 2))

(def
  ffi-lib
  (dt-ffi/define-library-interface
    (merge
     {
      :ffi_prep_cif {:rettype :int32
                     :argtypes [['cif :pointer]
                                ['abi :int32]
                                ['nargs :int32]
                                ['rettype :pointer]
                                ['argtypes :pointer?]
                                ]}

      :ffi_call {:rettype :void
                 :argtypes [['cif :pointer]
                            ['fptr :pointer]
                            ['rvalue :pointer?]
                            ['args :pointer?]]}

      :dlopen {:rettype :pointer
               :argtypes [['path :pointer]
                          ['mode :int32]]}

      :dlsym {:rettype :pointer
              :argtypes [['handle :pointer]
                         ['symbol :pointer]]}
      ,}
     (graal-native/when-defined-graal-native
      {
       :argtype_to_ffi_type {:rettype :pointer
                             :argtypes [['argtype :int32]]}}))

    :symbols
    (graal-native/if-defined-graal-native
     #{}
     '#{ffi_type_complex_double
        ffi_type_complex_float
        ffi_type_double
        ffi_type_float
        ffi_type_pointer
        ffi_type_sint16
        ffi_type_sint32
        ffi_type_sint64
        ffi_type_sint8
        ffi_type_uint16
        ffi_type_uint32
        ffi_type_uint64
        ffi_type_uint8
        ffi_type_void})

    :header-files (graal-native/if-defined-graal-native
                   []
                   ["<ffi.h>"])
    :libraries (graal-native/if-defined-graal-native
                []
                ["ffi"])))


(declare argtype_to_ffi_type)
(defn ^:private find-ffi-type [sym]
  []
  (graal-native/if-defined-graal-native
   ;; values chosen arbitrarily
   ;; must match csource/clj_ffi.c
   (argtype_to_ffi_type
    (case sym
      "ffi_type_complex_double" 1
      "ffi_type_complex_float" 2
      "ffi_type_double" 3
      "ffi_type_float" 4
      "ffi_type_pointer" 5
      "ffi_type_sint16" 6
      "ffi_type_sint32" 7
      "ffi_type_sint64" 8
      "ffi_type_sint8" 9
      "ffi_type_uint16" 10
      "ffi_type_uint32" 11
      "ffi_type_uint64" 12
      "ffi_type_uint8" 13
      "ffi_type_void" 14))
   (dt-ffi/find-symbol sym)))


(def ^:private ffi_type_complex_double (delay (find-ffi-type "ffi_type_complex_double")))
(def ^:private ffi_type_complex_float (delay (find-ffi-type "ffi_type_complex_float")))
(def ^:private ffi_type_double (delay (find-ffi-type "ffi_type_double")))
(def ^:private ffi_type_float (delay (find-ffi-type "ffi_type_float")))
(def ^:private ffi_type_pointer (delay (find-ffi-type "ffi_type_pointer")))
(def ^:private ffi_type_sint16 (delay (find-ffi-type "ffi_type_sint16")))
(def ^:private ffi_type_sint32 (delay (find-ffi-type "ffi_type_sint32")))
(def ^:private ffi_type_sint64 (delay (find-ffi-type "ffi_type_sint64")))
(def ^:private ffi_type_sint8 (delay (find-ffi-type "ffi_type_sint8")))
(def ^:private ffi_type_uint16 (delay (find-ffi-type "ffi_type_uint16")))
(def ^:private ffi_type_uint32 (delay (find-ffi-type "ffi_type_uint32")))
(def ^:private ffi_type_uint64 (delay (find-ffi-type "ffi_type_uint64")))
(def ^:private ffi_type_uint8 (delay (find-ffi-type "ffi_type_uint8")))
(def ^:private ffi_type_void (delay (find-ffi-type "ffi_type_void")))

(defn ^:private argtype->ffi-type [type]
  (case type
    :void  @ffi_type_void
    (:pointer? :pointer) @ffi_type_pointer
    :int8  @ffi_type_sint8
    :int16 @ffi_type_sint16
    :int32 @ffi_type_sint32
    :int64 @ffi_type_sint64
    :float32 @ffi_type_float
    :float64 @ffi_type_double)
  )


(comment
  (def zlib (dlopen (dt-ffi/string->c "/opt/local/lib/libz.dylib")
                    RTLD_LAZY))

  (def compress (dlsym RTLD_DEFAULT (dt-ffi/string->c "compress"))))


(defn ^:private make-ptr-uninitialized
  "Make an object convertible to a pointer that points to  single value of type
  `dtype`."
  (^NativeBuffer [dtype options]
   (let [dtype (ffi-size-t/lower-ptr-type dtype)
         ^NativeBuffer nbuf (-> (native-buffer/malloc
                                 (casting/numeric-byte-width dtype)
                                 options)
                                (native-buffer/set-native-datatype dtype))]
     nbuf))
  (^NativeBuffer [dtype]
   (make-ptr-uninitialized dtype {:resource-type :auto
                                  :uninitialized? true})))

;; usual size is 32
;; using 64 for extra space in case it grows in the future
(def ffi_cif_sizeof 64)
(defn ^:private cif-ptr-init ^NativeBuffer []
  (-> (native-buffer/malloc
       ffi_cif_sizeof
       {:resource-type :auto
        :uninitialized? true})))

(defn call [fname ret-type & types-and-args]
  (assert (even? (count types-and-args)))
  (let [;; this also initializes the ffi library
        ;; so that find symbol works :p
        fptr (dlsym RTLD_DEFAULT (dt-ffi/string->c fname))
        _ (assert fptr (str "function not found: " fname))

        cif (cif-ptr-init)

        arg-types (take-nth 2 types-and-args)
        args (->>  types-and-args
                   (drop 1)
                   (take-nth 2))

        nargs (count args)

        arg-type-ptrs (dtype/make-container :native-heap
                                            :int64
                                            (into
                                             []
                                             (comp (map argtype->ffi-type)
                                                   (map dt-ffi/->pointer)
                                                   (map #(.address ^Pointer %)))
                                             arg-types))]
    (ffi_prep_cif cif FFI_DEFAULT_ABI nargs (argtype->ffi-type ret-type)
                  arg-type-ptrs)

    (let [ret-ptr (if (= ret-type :void)
                    (long->pointer 0)
                    (make-ptr-uninitialized (if (= ret-type :pointer)
                                              :int64
                                              ret-type)))

          value-ptrs (mapv (fn [argtype arg]
                             (dt-ffi/make-ptr (if (= argtype :pointer)
                                                :int64
                                                argtype)
                                              (if (= argtype :pointer)
                                                (.address ^Pointer
                                                          (dt-ffi/->pointer arg))
                                                arg)))
                           arg-types
                           args)
          values (dtype/make-container :native-heap
                                       :int64
                                       (mapv #(.address ^tech.v3.datatype.native_buffer.NativeBuffer %)
                                             value-ptrs))]
      (ffi_call cif fptr ret-ptr values)
      ;; prevent garbage collection of values and value-ptrs
      (identity [values value-ptrs])
      (when (not= ret-type :void)
        (if (= ret-type :pointer)
          (long->pointer (nth ret-ptr 0))
          (nth ret-ptr 0))))))


(comment
  (call "cosf" :float32
        :float32 42)

  ,)

(defn -main [& args]
  (prn "args" args)
  (println "cos(42) =  " (call (nth args 0 "cos") :float64
                               :float64 (Double/parseDouble (nth args 1 "42")))))
