(ns run
  (:use [remvee.ring.middleware.partial-content] :reload-all)
  (:use [clojure.test]))

(deftest all
  (run-tests 'remvee.ring.middleware.partial-content))