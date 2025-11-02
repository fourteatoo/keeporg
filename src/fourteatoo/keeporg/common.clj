(ns fourteatoo.keeporg.common
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [java-time.api :as jt])
  (:import [org.apache.commons.compress.archivers ArchiveEntry]))


(def ^:dynamic *options*)

(defn opt [k]
  (get *options* k))

(defn verbosity []
  (or (opt :verbose) 0))

(defn instant-usec [usec]
  (jt/instant (quot usec 1000)))

(defn format-list [elements]
  (s/join "\n"
          (map (fn [item]
                 (str "  - [" (if (:is-checked item) "X" " ") "] "
                      (:text item)))
               elements)))

(defn file-name [f]
  (.getName (io/as-file f)))

(defn file-parent [f]
  (.getParent (io/as-file f)))

(extend-protocol io/Coercions
  ArchiveEntry
  (as-file [f] (io/as-file (.getName f)))
  (as-url [f] (io/as-url (.getName f))))

(defn file-suffix [file]
  (let [[ext & more] (reverse (s/split (file-name file) #"\."))]
    (if (#{"gz" "xz" "z"} ext)
      (str (first more) "." ext)
      ext)))

(defn replace-suffix [file suffix]
  (let [file (io/as-file file)]
    (io/file (file-parent file)
             (s/replace (file-name file) #"\.[^.]*$" (str "." suffix)))))

(defn as-org-file [file]
  (replace-suffix file "org"))

(defn as-markdown-file [file]
  (replace-suffix file "md"))

