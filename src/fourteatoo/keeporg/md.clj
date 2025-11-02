(ns fourteatoo.keeporg.md
  (:require [clojure.string :as s]
            [fourteatoo.keeporg.common :as c]
            [java-time.api :as jt]))


(defn output-markdown-takeout-header [out file]
  (binding [*out* out]
    (println "# Google Keep notes\n")
    (println "Takeout:" file)
    (println "Converted:" (jt/local-date-time))))

(defn make-note->markdown [heading-level]
  (let [convert-note (fn [note]
                       (str "\n" (s/join (repeat heading-level "#"))
                            " " (s/trim (:title note))
                            "\n\n"
                            (cond (:list-content note)
                                  (c/format-list (:list-content note))

                                  (:text-content note)
                                  (s/trim (:text-content note)))
                            \newline))]
    (fn [note]
      (assoc note :converted
             (convert-note (:keep-note note))))))

