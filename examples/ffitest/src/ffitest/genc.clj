(ns ffitest.genc
  (:require [clojure.math.combinatorics :as combo])
  
  (:gen-class))


(set! *warn-on-reflection* true)

(def rettypes [:void
               :int8
               :int16
               :int32
               :int64
               :float32
               :float64])

(def argtypes [:int8
               :int16
               :int32
               :int64
               :float32
               :float64])

(def arglists
  (for [n (range 3)
        arg-types (apply combo/cartesian-product
                         (repeat n argtypes))]
    arg-types))

(def fspecs (combo/cartesian-product rettypes arglists))

(defn ->fname [rettype arglist]
  (symbol (clojure.string/join
           "_"
           (into
            ["f"
             (name rettype)]
            (map name arglist))))
  )


(defn ->ctype [argtype]
  (case argtype
    :void "void"
    :int8 "char"
    :int16 "short"
    :int32 "int"
    :int64 "long long"
    :float32 "float"
    :float64 "double"))

(defn csource []
  (clojure.string/join
   "\n\n"
   (for [[rettype arglist] fspecs]
     (str (->ctype rettype) " " (->fname rettype arglist)
          "(" (clojure.string/join
               ", "
               (for [[i argtype] (map-indexed vector arglist)]
                 (str (->ctype argtype) " x" i)))
          ")"
          "{\n"
          (when (not= :void rettype)
            (str "return "
                 (if (seq arglist)
                   (clojure.string/join " + "
                                        (for [i (range (count arglist))]
                                          (str "x" i)))
                   "-1")
                 ";\n"))
          "}")
     ))

)

(defn generatec []
  (spit "ffitestlib.c" (csource)))


(defn -main [& args]
  (generatec))
