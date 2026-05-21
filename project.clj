(defproject io.github.fourteatoo/keeporg "0.1.0-SNAPSHOT"
  :description "Google Keep to Emacs Org migration tool"
  :url "http://github.com/fourteatoo/keeporg"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [org.apache.commons/commons-compress "1.28.0"]
                 [org.clj-commons/hickory "0.7.7"]
                 [camel-snake-kebab "0.4.3"]
                 [org.clojure/tools.cli "1.2.245"]
                 [clojure.java-time "1.4.3"]
                 [cheshire "6.1.0"]]
  :main ^:skip-aot fourteatoo.keeporg.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  ;; don't deploy on Clojars; this is not a library!
  :deploy-repositories ^:replace [["releases" :no-op] ["snapshots" :no-op]]
  :release-tasks ^:replace [["vcs" "assert-committed"]
                            ["change" "version" "leiningen.release/bump-version" "release"]
                            ["vcs" "commit"]
                            ["vcs" "tag" "v" "--no-sign"]
                            ;; ["deploy"]
                            ["change" "version" "leiningen.release/bump-version"]
                            ["vcs" "commit"]
                            ["vcs" "push"]])
