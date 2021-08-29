(ns com.phronemophobic.clj-libffi
  (:require [tech.v3.datatype.ffi :as dt-ffi]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.ffi.size-t :as ffi-size-t]
            [tech.v3.datatype.native-buffer :as native-buffer]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.datatype.graal-native :as graal-native])
  (:import [tech.v3.datatype.native_buffer NativeBuffer]
           [tech.v3.datatype.ffi Pointer])
  (:gen-class))

(set! *warn-on-reflection* true)

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

(dt-ffi/define-library-interface
  {

   :callc {:rettype :void
           :argtypes [['fptr :pointer]
                      ['rettype :int32]
                      ['ret :pointer?]
                      ['nargs :int32]
                      ['argtypes :pointer]
                      ['args :pointer]]}

   :dlopen {:rettype :pointer
            :argtypes [['path :pointer]
                       ['mode :int32]]}

   :dlsym {:rettype :pointer
           :argtypes [['handle :pointer]
                      ['symbol :pointer]]}
   

   ,}
  :libraries (graal-native/if-defined-graal-native
              []
              ["cljffi"]))

(comment
  (def zlib (dlopen (dt-ffi/string->c "/opt/local/lib/libz.dylib")
                    RTLD_LAZY))

  (def compress (dlsym RTLD_DEFAULT (dt-ffi/string->c "compress"))))


(defn make-ptr-uninitialized
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




(defn argtype->int [kw]
  ;; values chosen arbitrarily
  ;; must match clj_objc.mm
  (case kw
    :void 0                ;; ffi_type_void
    (:pointer? :pointer) 1 ;; ffi_type_pointer
    :int8 2 ;; ffi_type_sint8
    :int16 3               ;; ffi_type_sint16
    :int32 4               ;; ffi_type_sint32
    :int64 5               ;; ffi_type_sint64
    :float32 6               ;; ffi_type_float
    :float64 7              ;; ffi_type_double
    ))


(defn call [fname ret-type & types-and-args]
  (assert (even? (count types-and-args)))
  (let [        
        ret-ptr (if (= ret-type :void)
                  (long->pointer 0)
                  (make-ptr-uninitialized (if (= ret-type :pointer)
                                            :int64
                                            ret-type)))

        arg-types (take-nth 2 types-and-args)
        args (->>  types-and-args
                   (drop 1)
                   (take-nth 2))

        nargs (count args)

        rettype-int (argtype->int ret-type)
        argtype-ints (dtype/make-container :native-heap
                                           :int32
                                           (mapv argtype->int
                                                 arg-types))
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
                                           value-ptrs))

        fptr (dlsym RTLD_DEFAULT (dt-ffi/string->c fname))]


    ;; (println fptr)
    ;; (prn (list 'call_objc rettype-int ret-ptr nargs argtype-ints values))
    (prn "--------")
    (doseq [[k v] (partition 2
                             [:fptr fptr
                              :nargs nargs
                              :nvalues (count values)
                              :values values])]
      (prn k v))
    (callc fptr rettype-int ret-ptr nargs argtype-ints values)
    ;; no garbage collect
    (identity [values value-ptrs])
    
    (when (not= ret-type :void)
      (if (= ret-type :pointer)
        (long->pointer (nth ret-ptr 0))
        (nth ret-ptr 0)))))

(comment
  

  ,)

(defn -main [& args]
  (prn "args" args)
  (println "cos(42) =  " (call (nth args 0) :float64
                               :float64 (Double/parseDouble (nth args 1)))))
