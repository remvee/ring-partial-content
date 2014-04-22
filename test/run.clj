(ns run
  (:use [ring.middleware.partial-content] :reload-all)
  (:use [clojure.test]))

(deftest all
  (run-tests 'ring.middleware.partial-content))
