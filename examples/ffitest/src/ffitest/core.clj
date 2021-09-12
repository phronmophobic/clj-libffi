(ns ffitest.core
  (:require [ffitest.genc :as genc]
            [tech.v3.datatype.ffi :as dt-ffi]
            [com.phronemophobic.clj-libffi :as ffi]

            )
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main [& args]
  (assert
   (ffi/dlopen (dt-ffi/string->c "libffitestlib.dylib") ffi/RTLD_LAZY)
     "Library not found")


  (doseq [[rettype arglist] genc/fspecs
          :when (not= :void rettype)
          ]
      (let [fname (genc/->fname rettype arglist)]
        (print "calling " fname "... ")

        (let [call-args
              (concat
               [(name fname) rettype]
               (sequence
                cat
                (for [argtype arglist]
                  [argtype -1])))]

          (print (pr-str (cons 'ffi/call call-args)) " ")
          (let [ret-val (apply ffi/call call-args)]
            (print ret-val)
            (println " done.")
            (if (= :void rettype)
              (assert (nil? ret-val))
              (assert (= (long ret-val) (* -1 (max 1 (count arglist))))))))

        (System/gc)))

  ,)


