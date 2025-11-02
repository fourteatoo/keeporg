(defproject fourteatoo.keeporg "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
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
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
