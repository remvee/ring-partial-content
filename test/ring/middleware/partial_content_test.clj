;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns ring.middleware.partial-content-test
  "Tests for Partial Content middleware for ring."
  {:author "Remco van 't Veer"}
  (:require [clojure.test :refer [are deftest is testing]]
            [ring.middleware.partial-content :as sut])
  (:import java.io.StringBufferInputStream))

(deftest slice

  (testing "string"
    (let [f (fn [value start end]
              (slurp (sut/slice value start end)))]
      (is (= "3456" (f "0123456789" 3 7)))
      (is (= "0123456" (f "0123456789" 0 7)))
      (is (= "3456789" (f "0123456789" 3 10)))
      (is (= 7 (count (.getBytes "34😊6"))) ;; sanity check
          (= "34😊6" (f "01234😊6789" 3 10)))))

  (testing "inputstream"

    (testing "slurp"
      (let [f (fn [value start end]
                (slurp (sut/slice (StringBufferInputStream. value) start end)))]
        (is (= "3456" (f "0123456789" 3 7)))
        (is (= "0123456" (f "0123456789" 0 7)))
        (is (= "3456789" (f "0123456789" 3 10)))))

    (testing "read"
      (let [buf (byte-array 2)
            in (sut/slice (StringBufferInputStream. "0123456789") 3 6)]
        (is (and (= 2 (.read in buf))
                 (= (byte \3) (aget buf 0))
                 (= (byte \4) (aget buf 1))))
        (is (and (= 1 (.read in buf))
                 (= (byte \5) (aget buf 0))))
        (is (and (= -1 (.read in buf))))))))

(deftest wrap-partial-content

  (testing "non range request"
    (let [res {:status  200
               :headers {"Content-Length" "10"}
               :body    "0123456789"}
          f   (sut/wrap-partial-content (constantly res))]
      (is (= {:status  200
              :headers {"Accept-Ranges"  "bytes"
                        "Content-Length" "10"}
              :body    "0123456789"}
             (f {}))
          "only Accept-Ranges added")))

  (testing "range cases"
    (let [res {:status  200
               :headers {"Content-Length" "10"}
               :body    "0123456789"}
          f   (sut/wrap-partial-content (constantly res))]
      (are [req expected]
          (= expected
             (-> (f req)
                 (update :body slurp)))

        {:headers {"range" "bytes=5-7"}}
        {:status  206
         :headers {"Accept-Ranges"  "bytes"
                   "Content-Length" "3"
                   "Content-Range"  "bytes 5-7/10"}
         :body    "567"}

        {:headers {"range" "bytes=-7"}}
        {:status  206
         :headers {"Accept-Ranges"  "bytes"
                   "Content-Length" "8"
                   "Content-Range"  "bytes 0-7/10"}
         :body    "01234567"}

        {:headers {"range" "bytes=5-"}}
        {:status  206
         :headers {"Accept-Ranges"  "bytes"
                   "Content-Length" "5"
                   "Content-Range"  "bytes 5-9/10"}
         :body    "56789"})))

  (testing "unexpected body type, leave body untouched"
    (let [res {:status  200
               :headers {"Content-Length" "10"}
               :body    (atom "dummy")}
          f   (sut/wrap-partial-content (constantly res))]
      (is (= (f {:headers {"range" "bytes=5-7"}})
             res)))))
