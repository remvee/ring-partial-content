;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns ring.middleware.partial-content
  "Partial Content middleware for ring."
  {:author "Remco van 't Veer"}
  (:refer-clojure :exclude [subs])
  (:use clojure.test)
  (:import [java.io
            File
            InputStream
            FileInputStream
            StringBufferInputStream]))

(defmulti
  #^{:private true
     :test (fn []
             (is (= "3456" (subs "0123456789" 3 7)))
             (is (= "0123456" (subs "0123456789" 0 7)))
             (is (= "3456789" (subs "0123456789" 3 10)))
             (let [sss (fn [v s e]
                         (slurp (subs (StringBufferInputStream. v) s e)))]
               (is (= "3456" (sss "0123456789" 3 7)))
               (is (= "0123456" (sss "0123456789" 0 7)))
               (is (= "3456789" (sss "0123456789" 3 10)))))}
  subs (fn [val start end] (class val)))

(defmethod subs String [val start end]
  (.substring val start end))

(defmethod subs InputStream [val start end]
  (.skip val start)
  (let [n (atom (- end start))]
    (proxy [InputStream] []
      (read
        ([]
           (dosync
            (if (> @n 0)
              (do
                (swap! n dec)
                (.read val))
              -1)))
        ([b]
           (.read this b 0 (count b)))
        ([b off len]
           (dosync
            (if (> @n off)
              (do
                (let [rem (min len @n)]
                  (swap! n - (+ off len))
                  (.read val b off rem)))
              -1)))))))

(defmethod subs File [val start end]
  (subs (FileInputStream. val) start end))

(defn wrap-partial-content

  "Wrap an app such that a request for a range will respond with a 206
   Partial Content response with the appropriate headers set and the
   body trimmed accordingly.  Only single byte range requests and
   original 200 OK responses with Content-Length set and a File,
   InputStream or String body will be handled.

   This wrapper works fine with ring.middleware.file.wrap-file."

  {:test
   (fn []
     (are [req expected]
          (= expected
             ((wrap-partial-content
               (fn [_]
                 {:status 200
                  :headers {"Content-Length" "10"}
                  :body "0123456789"}))
              req))

          {:headers {"range" "bytes=5-7"}}
          {:status 206
           :headers {"Content-Length" "3"
                     "Content-Range"  "bytes 5-7/10"}
           :body "567"}

          {:headers {"range" "bytes=-7"}}
          {:status 206
           :headers {"Content-Length" "8"
                     "Content-Range"  "bytes 0-7/10"}
           :body "01234567"}

          {:headers {"range" "bytes=5-"}}
          {:status 206
           :headers {"Content-Length" "5"
                     "Content-Range"  "bytes 5-9/10"}
           :body "56789"}))}
  [app]
  (fn [req]
    (let [res (app req)
          range ((:headers req) "range")
          len (and (:headers res)
                   ((:headers res) "Content-Length")
                   (Long/valueOf ((:headers res) "Content-Length")))
          [start end] (and range
                           (map #(and (not= "" %) (Long/valueOf %))
                                (rest (re-matches #"bytes=(\d*)-(\d*)"
                                                  range))))]
      (if (and (= 200 (:status res))
               range
               len
               (or start end))
        (let [body  (:body res)
              start (or start 0)
              end   (or end (dec len))]
          (reduce (fn [m [k v]]
                    (if (map? (get m k))
                      (assoc m k (merge (get m k) v))
                      (assoc m k v)))
                  res
                  {:status 206
                   :headers {"Content-Range" (str "bytes " start
                                                  "-" end
                                                  "/" len)
                             "Content-Length" (str (- (inc end) start))}
                   :body (subs body start (inc end))}))
        res))))
