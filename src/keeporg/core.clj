(ns keeporg.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.pprint :as pp]
   [cheshire.core :as json]
   [java-time.api :as jt]
   [camel-snake-kebab.core :as csk])
  (:import
   [org.apache.commons.compress.archivers ArchiveEntry]
   [org.apache.commons.compress.archivers.tar TarArchiveInputStream]
   [org.apache.commons.compress.archivers.zip ZipArchiveInputStream]
   [java.util.zip GZIPInputStream]))


(def ^:dynamic *options*)

(defn file-name [f]
  (.getName (io/as-file f)))

(defn file-parent [f]
  (.getParent (io/as-file f)))

;; No implementation of method: :as-file of protocol:
;; #'clojure.java.io/Coercions found for class:
;; org.apache.commons.compress.archivers.tar.TarArchiveEntry


(extend-protocol clojure.java.io/Coercions
  ArchiveEntry
  (as-file [f] (io/as-file (.getName f)))
  (as-url [f] (io/as-url (.getName f))))

(defn file-suffix [file]
  (let [[ext & more] (reverse (s/split (file-name file) #"\."))]
    (if (#{"gz" "xz" "z"} ext)
      (str (first more) "." ext)
      ext)))

(defmulti archive-input-stream file-suffix)

(defmethod archive-input-stream "tgz"
  [file]
  (TarArchiveInputStream.
   (GZIPInputStream.
    (io/input-stream file))))

(defmethod archive-input-stream "tar,gz"
  [file]
  (TarArchiveInputStream.
   (GZIPInputStream.
    (io/input-stream file))))

(defmethod archive-input-stream "zip"
  [file]
  (ZipArchiveInputStream.
   (io/input-stream file)))

(defn json-entry? [entry]
  (and (not (.isDirectory entry))
       (= "json" (file-suffix entry))))

(defn process-archive [file f]
  (with-open [stream (archive-input-stream file)]
    (loop [entry (.getNextEntry stream)]
      (when entry
        (when-not (.isDirectory entry)
          (f entry stream))
        (recur (.getNextEntry stream))))))

(defn map-archive [f file]
  (let [stream (archive-input-stream file)]
    (letfn [(next-entry []
              (let [entry (.getNextEntry stream)]
                (if entry
                  (lazy-seq (cons (f entry stream) (next-entry)))
                  (.close stream))))]
      (next-entry))))

(defn- format-list [elements]
  (s/join "\n"
          (map (fn [item]
                 (str "  - [" (if (:is-checked item) "X" " ") "] "
                      (:text item)))
               elements)))

(defn- instant-usec [usec]
  (jt/instant (quot usec 1000)))

(defn- timestamp-usec->org [usec]
  (str (instant-usec usec)))

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
  (let [
        convert-note (fn [note]
                       (str "\n" (s/join (repeat heading-level "*"))
                            " " (s/trim (:title note)) \newline
                            (format-properties note)
                            "\n"
                            (cond (:list-content note)
                                  (format-list (:list-content note))

                                  (:text-content note)
                                  (s/trim (:text-content note)))
                            \newline))]
    (fn [note]
      (assoc note :converted
             (convert-note (:keep-note note))))))

(defn make-note->markdown [heading-level]
  (let [convert-note (fn [note]
                       (str "\n" (s/join (repeat heading-level "#"))
                            " " (s/trim (:title note))
                            "\n\n"
                            (cond (:list-content note)
                                  (format-list (:list-content note))

                                  (:text-content note)
                                  (s/trim (:text-content note)))
                            \newline))]
    (fn [note]
      (assoc note :converted
             (convert-note (:keep-note note))))))

(defn replace-suffix [file suffix]
  (let [file (io/as-file file)]
    (io/file (file-parent file)
             (s/replace (file-name file) #"\.[^.]*$" (str "." suffix)))))

(defn as-org-file [file]
  (replace-suffix file "org"))

(defn as-markdown-file [file]
  (replace-suffix file "md"))

(defn write-note-to-file [note change-suffix]
  (let [output-file (change-suffix (get-in note [:archive-entry :name]))]
    (println output-file)
    (io/make-parents note)
    (spit output-file
          (:converted note))))

(defn write-note-to-stream [note out]
  (binding [*out* out]
    (print (:converted note))))

(defn- verbosity []
  (or (:verbose *options*) 0))

(defn read-archive [file]
  (->> file
       (map-archive (fn [entry stream]
                      (when (json-entry? entry)
                        {:archive-entry (bean entry)
                         :keep-note (json/parse-stream (io/reader stream) csk/->kebab-case-keyword)})))
       (remove nil?)
       (map (fn [note]
              (when (> (verbosity) 0)
                (pp/pprint (:keep-note note)))
              note))))

(defn- usage [summary errors]
  (println "usage: keeporg [options ...]")
  (doseq [e errors]
    (println e))
  (when summary
    (println summary))
  (System/exit -1))

(def ^:private cli-options
  [["-m" "--output-markdown" "output Markdown files instead of Emacs org"]
   ["-s" "--split" "write notes each in its own file"]
   ["-v" "--verbose" "increase logging verbosity"
    :default 0
    :update-fn inc]
   ["-h" "--help" "Show program usage"]])

(defn- parse-cli [args]
  (let [{:keys [arguments options summary errors] :as result} (parse-opts args cli-options)]
    (when (or errors
              (:help options))
      (usage summary errors))
    result))

(defn -main [& args]
  (let [{:keys [options summary arguments]} (parse-cli args)]
    (binding [*options* options]
      (if (:split options)
        (run! (fn [f]
                (->> (read-archive f)
                     (map (if (:output-markdown options)
                            (make-note->markdown 1)
                            (make-note->org 1)))
                     (run! #(write-note-to-file % (if (:output-markdown options)
                                                    as-markdown-file
                                                    as-org-file)))))
              arguments)
        (run! (fn [f]
                (with-open [out (io/writer (if (:output-markdown options)
                                             (as-markdown-file f)
                                             (as-org-file f)))]
                  (->> (read-archive f)
                       (map (if (:output-markdown options)
                              (make-note->markdown 2)
                              (make-note->org 2)))
                       (run! #(write-note-to-stream % out)))))
              arguments)))))

(comment
  (-main  "/home/wcp/Downloads/takeout-20251020T091417Z-1-001.tgz"))
