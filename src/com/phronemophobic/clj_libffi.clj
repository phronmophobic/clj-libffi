(ns com.phronemophobic.clj-libffi
  (:require [tech.v3.datatype.ffi :as dt-ffi]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.ffi.size-t :as ffi-size-t]
            [tech.v3.datatype.native-buffer :as native-buffer]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.datatype.struct :as dt-struct]
            [tech.v3.datatype.graal-native :as graal-native])
  (:import [tech.v3.datatype.native_buffer NativeBuffer]
           [tech.v3.datatype.ffi Pointer])
  (:gen-class))

(set! *warn-on-reflection* true)

(def ^{:private true
       :dynamic true} *pool* nil)

(defn ->address
  "Coerce a container to a pointer, but keep a reference to the container in *pool*"
  [o]
  (var-set #'*pool* (conj *pool* o))
  (.address ^Pointer (dt-ffi/->pointer o)))

(defn long->pointer [n]
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

;; (def FFI_DEFAULT_ABI (int 2))
(def ^:dynamic *abi*
  "The ABI to use for ffi calls. The abi value is platform dependent and there's no known way to get the default ABI generically at runtime. aarch64 seems to be 1. x86 seems to be 2."
  (int 1))

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

      :dlopen {:rettype :pointer?
               :argtypes [['path :pointer?]
                          ['mode :int32]]}

      :dlsym {:rettype :pointer?
              :argtypes [['handle :pointer]
                         ['symbol :pointer]]}
      ,})

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


(dt-struct/define-datatype! :ffi_type
  [{:name :size :datatype (ffi-size-t/size-t-type)}
   {:name :alignment :datatype :int16}
   {:name :type :datatype :int16}
   {:name :elements :datatype :pointer}])

;; see csource/print_ffi_types.c
(def ^:private ffi-type-infos
  [["ffi_type_double", 8, 8, 3, 0x0]
   ["ffi_type_float", 4, 4, 2, 0x0]
   ["ffi_type_pointer", 8, 8, 14, 0x0]
   ["ffi_type_sint16", 2, 2, 8, 0x0]
   ["ffi_type_sint32", 4, 4, 10, 0x0]
   ["ffi_type_sint64", 8, 8, 12, 0x0]
   ["ffi_type_sint8", 1, 1, 6, 0x0]
   ["ffi_type_uint16", 2, 2, 7, 0x0]
   ["ffi_type_uint32", 4, 4, 9, 0x0]
   ["ffi_type_uint64", 8, 8, 11, 0x0]
   ["ffi_type_uint8", 1, 1, 5, 0x0]
   ["ffi_type_void", 1, 1, 0, 0x0]])
(def ^:private ffi-type-ks [:size :alignment :type :elements])

(def ^:private ffi-types-manual
  (delay
    (into {}
          (for [[ffi-type & vals] ffi-type-infos]
            {ffi-type
             (reduce (fn [struct [k v]]
                       (.put ^tech.v3.datatype.struct.Struct struct k v)
                       struct)
                     (dt-struct/new-struct :ffi_type {:container-type :native-heap})
                     (zipmap ffi-type-ks vals))}))))


(defn ^:private find-ffi-type [sym]
  []
  (graal-native/if-defined-graal-native
   (if-let [type (dlsym RTLD_DEFAULT (dt-ffi/string->c sym))]
     type
     (get @ffi-types-manual sym))
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

(defn ptr-array [ptrs]
  (dtype/make-container :native-heap
                        (ffi-size-t/ptr-t-type)
                        (into
                         []
                         (map ->address)
                         ptrs)))

(declare argtype->ffi-type)
(defn struct-def->ffi-type [sdef]
  (let [ftype (dt-struct/new-struct :ffi_type
                                    {:container-type :native-heap})
        elements (ptr-array
                  (concat
                   (eduction
                    (map :datatype)
                    (map argtype->ffi-type)
                    (:data-layout sdef))
                   ;; null terminated
                   [(long->pointer 0)]))]
    (dt-struct/map->struct!
     {:type 13
      :elements (->address elements)}
     ftype)))

(defn ^:private argtype->ffi-type [type]
  (case type
    :void  @ffi_type_void
    (:pointer? :pointer) @ffi_type_pointer
    :int8  @ffi_type_sint8
    :int16 @ffi_type_sint16
    :int32 @ffi_type_sint32
    :int64 @ffi_type_sint64
    :uint8  @ffi_type_uint8
    :uint16 @ffi_type_uint16
    :uint32 @ffi_type_uint32
    :uint64 @ffi_type_uint64
    :float32 @ffi_type_float
    :float64 @ffi_type_double
    ;; else
    (if (dt-struct/struct-datatype? type)
      (let [sdef (dt-struct/get-struct-def type)]
        (struct-def->ffi-type sdef))
      (throw (ex-info "Unknown argtype"
                      {:type type})))))

(defn load-library
  "Loads a shared library.

  <libname> can be either a lib name \"ffi\" or a path to a shared library.
  "
  [libname]
  (dlopen (dt-ffi/string->c libname)
          RTLD_LAZY))

(comment
  (def zlib (dlopen (dt-ffi/string->c "/opt/local/lib/libz.dylib")
                    RTLD_LAZY))

  (def compress (dlsym RTLD_DEFAULT (dt-ffi/string->c "compress"))))


(defn ^:private make-ptr-uninitialized
  "Make an object convertible to a pointer that points to  single value of type
  `dtype`."
  (^NativeBuffer [dtype options]
   (let [dtype (ffi-size-t/numeric-size-t-type dtype)
         ;; In most situations, ‘libffi’ will handle promotion according to the ABI. However, for historical reasons, there is a special case with return values that must be handled by your code. In particular, for integral (not struct) types that are narrower than the system register size, the return value will be widened by ‘libffi’. ‘libffi’ provides a type, ffi_arg, that can be used as the return type. For example, if the CIF was defined with a return type of char, ‘libffi’ will try to store a full ffi_arg into the return value.
         dtype (case dtype
                 :int8 :int64
                 :int16 :int64
                 :int32 :int64
                 dtype)

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

(defn ^:private lower-type [t]
  (ffi-size-t/numeric-size-t-type t))

(defn call
  "Calls a c function with fname. Function must be
  already linked or loaded using `load-library`.

  fname: String name of the function
  ret-type: keyword return type of the function. See types below.
  types-and-args: pairs of <arg-type> <arg-value>. See types below.

  Types:

  Same as dtype-next.
  :void
  :pointer
  :pointer?
  :int8
  :int16
  :int32
  :int64
  :float32
  :float64

  Example:

  (call \"cosf\" :float32 :float32 42)
"
  [fname ret-type & types-and-args]
  (assert (even? (count types-and-args)))
  (binding [*pool* []]
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
                                                     (map ->address))
                                               arg-types))]

      (ffi_prep_cif cif *abi* nargs (argtype->ffi-type ret-type)
                    arg-type-ptrs)

      (let [ret-ptr (if (= ret-type :void)
                      (long->pointer 0)
                      (make-ptr-uninitialized (lower-type ret-type)))
            value-ptrs (mapv (fn [argtype arg]
                               (if (dt-struct/struct-datatype? argtype)
                                 arg
                                 (dt-ffi/make-ptr (lower-type argtype)
                                                  (if (#{:pointer :pointer?} argtype)
                                                    (->address arg)
                                                    arg))))
                             arg-types
                             args)
            values (dtype/make-container :native-heap
                                         :int64
                                         (mapv ->address value-ptrs))]
        (ffi_call cif fptr ret-ptr values)
        ;; prevent garbage collection of values and value-ptrs
        (identity [values value-ptrs])
        (when (not= ret-type :void)
          (if (#{:pointer :pointer?} ret-type)
            (long->pointer (nth ret-ptr 0))
            (nth ret-ptr 0)))))))


(comment
  (call "cosf" :float32
        :float32 42)

  ,)



(defn -main [& args]
  #_(prn "args" args)
  #_(println "cos(42) =  " (call (nth args 0 "cos") :float64
                               :float64 (Double/parseDouble (nth args 1 "42"))))

    (dlopen (dt-ffi/string->c "/Users/adrian/workspace/clj-libffi/csource/libcall.dylib")
            RTLD_LAZY)

  (dt-struct/define-datatype! :time
    [{:name :tm_sec :datatype :int32}
     {:name :tm_min :datatype :int32}
     {:name :tm_hour :datatype :int32}
     {:name :tm_mday :datatype :int32}
     {:name :tm_mon :datatype :int32}
     {:name :tm_year :datatype :int32}
     {:name :tm_wday :datatype :int32}
     {:name :tm_yday :datatype :int32}
     {:name :tm_isdst :datatype :int32}
     {:name :__tm_gmtoff__ :datatype  :int32}
     {:name :__tm_zone__ :datatype :pointer}])
  (binding [*pool* []]
   (call "print_time" :void
         :time (dt-struct/map->struct :time {:tm_sec 42
                                             :tm_isdst 32
                                             :__tm_zone__ (->address (dt-ffi/string->c "woohoo"))
                                             } :gc))))
