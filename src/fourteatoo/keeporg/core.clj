(ns fourteatoo.keeporg.core
  (:gen-class)
  (:require
   [camel-snake-kebab.core :as csk]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.tools.cli :refer [parse-opts]]
   [fourteatoo.keeporg.common :as c]
   [fourteatoo.keeporg.md :as md]
   [fourteatoo.keeporg.org :as org])
  (:import
   (java.util.zip GZIPInputStream)
   (org.apache.commons.compress.archivers.tar TarArchiveInputStream)
   (org.apache.commons.compress.archivers.zip ZipArchiveInputStream)))

(defmulti archive-input-stream c/file-suffix)

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

(defn- json-entry? [entry]
  (and (not (.isDirectory entry))
       (= "json" (c/file-suffix entry))))

(defn map-archive [f file]
  (let [stream (archive-input-stream file)]
    (letfn [(next-entry []
              (let [entry (.getNextEntry stream)]
                (if entry
                  (lazy-seq (cons (f entry stream) (next-entry)))
                  (.close stream))))]
      (next-entry))))

(defn write-note-to-file [note change-suffix]
  (let [output-file (change-suffix (get-in note [:archive-entry :name]))]
    (println output-file)
    (io/make-parents note)
    (spit output-file
          (:converted note))))

(defn write-note-to-stream [note out]
  (binding [*out* out]
    (print (:converted note))))

(defn read-archive [file]
  (->> file
       (map-archive (fn [entry stream]
                      (when (json-entry? entry)
                        {:archive-entry (bean entry)
                         :keep-note (json/parse-stream (io/reader stream) csk/->kebab-case-keyword)})))
       (remove nil?)
       (map (fn [note]
              (when (> (c/verbosity) 0)
                (pp/pprint (:keep-note note)))
              note))))

(defn- usage [summary errors]
  (println "usage: keeporg [option ...] file ...")
  (doseq [e errors]
    (println e))
  (when summary
    (println summary))
  (System/exit -1))

(def ^:private cli-options
  [["-m" "--markdown" "output Markdown instead of Emacs org"]
   ["-s" "--split" "write each note in separate file"]
   ["-v" "--verbose" "increase verbosity"
    :default 0
    :update-fn inc]
   ["-h" "--help" "show program usage"]])

(defn- parse-cli [args]
  (let [{:keys [arguments options summary errors] :as result} (parse-opts args cli-options)]
    (when (or errors
              (:help options))
      (usage summary errors))
    result))

(defn- process-archive-split [file]
  (->> (read-archive file)
       (map (if (c/opt :output-markdown)
              (md/make-note->markdown 1)
              (org/make-note->org 1)))
       (run! #(write-note-to-file % (if (c/opt :output-markdown)
                                      c/as-markdown-file
                                      c/as-org-file)))))

(defn- process-archive-whole [file]
  (with-open [out (io/writer (if (c/opt :output-markdown)
                               (c/as-markdown-file file)
                               (c/as-org-file file)))]
    ((if (c/opt :output-markdown)
       md/output-markdown-takeout-header
       org/output-org-takeout-header) out file)
    (->> (read-archive file)
         (map (if (c/opt :output-markdown)
                (md/make-note->markdown 2)
                (org/make-note->org 2)))
         (run! #(write-note-to-stream % out)))))


(defn -main [& args]
  (let [{:keys [options summary arguments]} (parse-cli args)]
    (binding [c/*options* options]
      (run! (if (:split options)
              process-archive-split
              process-archive-whole)
            arguments))))

(comment
  (-main  "/home/wcp/Downloads/takeout-20251020T091417Z-1-001.tgz"))
