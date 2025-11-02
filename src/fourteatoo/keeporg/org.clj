(ns fourteatoo.keeporg.org
  (:require [clojure.string :as s]
            [fourteatoo.keeporg.common :as c]))


(defn output-org-takeout-header [out file]
  (binding [*out* out]
    (println "* Google Keep notes\n")
    (println ":PROPERTIES:")
    (println "TAKEOUT:" file)
    (println "CONVERTED:" (jt/local-date-time))
    (println ":END:")))

(defn- timestamp-usec->org [usec]
  (str (c/instant-usec usec)))

;; It would be ince to include the annotations.  Example:
;; :annotations
;; [{:description "",
;;   :source "WEBLINK",
;;   :title "- YouTube",
;;   :url
;;   "https://www.youtube.com/shorts/LR3RwzSVmQc?si=tinv-okV42EPRjVg"}],

(defn- format-properties [note]
  (str ":PROPERTIES:\n"
       (when (:is-trashed note)
         "TRASHED: true\n")
       (when (:is-pinned note)
         "PINNED: true\n")
       (when (:is-archived note)
         "ARCHIVED: true\n")
       (when (:created-timestamp-usec note)
         (str "CREATED: " (timestamp-usec->org (:created-timestamp-usec note)) "\n"))
       (when (:user-edited-timestamp-usec note)
         ;; 1724223307269000
         (str "EDITED: " (timestamp-usec->org (:user-edited-timestamp-usec note)) "\n"))
       ":END:\n"))

(defn make-note->org [heading-level]
  (let [convert-note (fn [note]
                       (str "\n" (s/join (repeat heading-level "*"))
                            " " (s/trim (:title note)) \newline
                            (format-properties note)
                            "\n"
                            (cond (:list-content note)
                                  (c/format-list (:list-content note))

                                  (:text-content note)
                                  (s/trim (:text-content note)))
                            \newline))]
    (fn [note]
      (assoc note :converted
             (convert-note (:keep-note note))))))

