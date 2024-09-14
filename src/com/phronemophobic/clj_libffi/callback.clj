(ns com.phronemophobic.clj-libffi.callback
  (:require [tech.v3.datatype.ffi :as dt-ffi]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.ffi.size-t :as ffi-size-t]
            [tech.v3.datatype.native-buffer :as native-buffer]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.datatype.struct :as dt-struct]
            [tech.v3.datatype.graal-native :as graal-native]
            [com.phronemophobic.clj-libffi :as ffi])
  (:import [tech.v3.datatype.native_buffer NativeBuffer]
           [java.util.concurrent.atomic AtomicLong]
           [tech.v3.datatype.ffi Pointer])
  (:gen-class))

(set! *warn-on-reflection* true)

(defonce callbacks
  (atom {}))

(defn com_phronemophobic_clj_libffi_callback [key ^Pointer ret ^Pointer args]
  (try
    (let [{:keys [ret-type arg-types f] :as info} (get @callbacks key)
          
          argbuf (-> (native-buffer/wrap-address (.address args)
                                                 (* ffi-size-t/size-t-size
                                                    (count arg-types))
                                                 nil)
                     (native-buffer/set-native-datatype :uint64))
          result (apply f
                        (eduction
                         (map-indexed (fn [i dtype]
                                        (let [ptr (ffi/long->pointer (nth argbuf i))]
                                          (case dtype
                                            ;; 
                                            (:uint8 :int8) (.getByte (native-buffer/unsafe) (.address ptr))
                                            (:uint16 :int16) (.getShort (native-buffer/unsafe) (.address ptr))
                                            (:uint32 :int32) (.getInt (native-buffer/unsafe) (.address ptr))
                                            (:uint64 :int64) (.getLong  (native-buffer/unsafe) (.address ptr))

                                            :float32 (.getFloat  (native-buffer/unsafe) (.address ptr))
                                            :float64 (.getDouble  (native-buffer/unsafe) (.address ptr))

                                            (:pointer :pointer?)
                                            (ffi/long->pointer (.getLong  (native-buffer/unsafe) (.address ptr)))

                                            ;; else
                                            ;; I think throwing is a bad idea
                                            (throw (ex-info "Unsupported arg dtype"
                                                            {:dtype dtype
                                                             :callback info}))))))
                         arg-types))]
      (when (not= :void ret-type)
        (case ret-type
          ;; 
          (:uint8 :int8) (.putByte (native-buffer/unsafe) (.address ret) (unchecked-byte result) )
          (:uint16 :int16) (.putShort (native-buffer/unsafe) (.address ret) (unchecked-short result))
          (:uint32 :int32) (.putInt (native-buffer/unsafe) (.address ret) (unchecked-int result))
          (:uint64 :int64) (.putLong  (native-buffer/unsafe) (.address ret) (unchecked-long result))

          :float32 (.putFloat  (native-buffer/unsafe) (.address ret) (float result))
          :float64 (.putDouble  (native-buffer/unsafe) (.address ret) (double result))
          
          (:pointer :pointer?)
          (.putLong  (native-buffer/unsafe) (.address ret) (unchecked-long
                                                          (.address (dt-ffi/->pointer result))))

          ;; else
          ;; I think throwing is a bad idea
          (throw (ex-info "Unsupported arg dtype"
                          {:dtype ret-type
                           :callback info})))))
    (catch Exception e
      (prn e))))

(defn compile-interface-class [& args]
  ((requiring-resolve 'tech.v3.datatype.ffi.graalvm/expose-clojure-functions)
   {#'com_phronemophobic_clj_libffi_callback
    {:rettype :void
     :argtypes [['key :int64]
                ['ret :pointer]
                ['args :pointer]]}}

   'com.phronemophobic.clj_libffi.callback.interface nil)
  )

(when *compile-files*
  (compile-interface-class))

(def lib
  (dt-ffi/define-library-interface
    {
     :ffi_closure_alloc {:rettype :pointer
                         :argtypes [['size :int64]
                                    ['fptr :pointer]]}

     :ffi_prep_closure_loc {:rettype :int32
                            :argtypes [['closure :pointer]
                                       ['cif :pointer]
                                       ['callback :pointer]
                                       ['userdata :pointer]
                                       ['wrapped :pointer?]]}

     ;; currently, this implementation depends on
     ;; a small native library that looks like the following
     ;; it is currently not included

;; typedef void (*operation_t)(void*,void*,void*,void*);
;; void clj_generic_callback(void *cif, void *ret, void* args,
;;                     void *userdata)
;; {
;;     long long int key = *((long long int *)userdata);
;;     graal_isolatethread_t* thread = ...;
;;     com_phronemophobic_clj_libffi_callback(thread , key, ret, args);
;; }
;;
;;
;; operation_t clj_get_generic_callback_address(){
;;     return &clj_generic_callback;
;; }
     
     :clj_generic_callback {:rettype :void
                            :argtypes [['cif :pointer]
                                       ['ret :pointer]
                                       ['args :pointer]
                                       ['userdata :pointer]]}

     :clj_get_generic_callback_address {:rettype :pointer
                                        :argtypes []}


     ,}))

(def generic-callback*
  (delay
    (clj_get_generic_callback_address)))

(defonce refs (atom #{}))
(defn ref! [o]
  (swap! refs conj o)
  o)

(defonce counter (AtomicLong. 0))
(defn make-callback [f ret-type arg-types]
  ;; ffi_closure_alloc(sizeof(ffi_closure), &bound_puts);
  (binding [ffi/*pool* []]
    (let [callback-ptr (ref! (dt-ffi/make-ptr :pointer 0))

          ;; measured at 56
          ;; hopefully some extra breathing room
          closure-size 80
          closure (ref! (ffi_closure_alloc closure-size
                                      callback-ptr))
          cif (ref! (ffi/make-cif ret-type arg-types))

          ;; ffi_prep_closure_loc(closure, &cif, puts_binding,
          ;;                               stdout, bound_puts)
          key (.incrementAndGet ^AtomicLong counter)
          key* (ref! (dt-ffi/make-ptr :int64
                                 key))

          gc @generic-callback*
          fptr (ref! (ffi/long->pointer (nth callback-ptr 0)))

          _ (assert (zero?
                     (ffi_prep_closure_loc closure
                                           cif
                                           gc
                                           key*
                                           fptr)))
          ]
      (swap! callbacks assoc key {:key key
                                  :ret-type ret-type
                                  :arg-types arg-types
                                  :f f})
      fptr)))



