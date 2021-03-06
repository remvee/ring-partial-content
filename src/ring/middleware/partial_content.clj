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
  (:import [java.io ByteArrayInputStream File FileInputStream InputStream]))

(defmulti slice (fn [value start end] (class value)))

(defmethod slice InputStream [in start end]
  (.skip in start)
  (let [n (atom (- end start))]
    (proxy [InputStream] []
      (read
        ([]
         (if (pos? @n)
           (do
             (swap! n dec)
             (.read in))
           -1))
        ([b]
         ;; delegate to arity-3
         (.read this b 0 (count b)))
        ([b off len]
         (if (> @n off)
           (let [rem (min len @n)]
             (swap! n - (+ off len))
             (.read in b off rem))
           -1))))))

(defmethod slice File [file start end]
  (slice (FileInputStream. file) start end))

(defmethod slice String [value start end]
  (slice (ByteArrayInputStream. (.getBytes value)) start end))

(defn can-slice? [value]
  (some? (get-method slice (class value))))

(defn wrap-partial-content
  "Wrap an app such that a request for a range will respond with a 206
   Partial Content response with the appropriate headers set and the
   body trimmed accordingly.  Only single byte range requests and
   original 200 OK responses with Content-Length set and a File,
   InputStream or String body will be handled.

   This wrapper works fine with ring.middleware.file.wrap-file."
  [app]
  (fn [req]
    (let [{:keys [status body] :as res} (app req)
          content-length                (when-let [v (get-in res [:headers "Content-Length"])]
                                          (Long/valueOf v))
          [start end]                   (when-let [v (get-in req [:headers "range"])]
                                          (map #(and (not= "" %) (Long/valueOf %))
                                               (rest (re-matches #"bytes=(\d*)-(\d*)" v))))]
      (if (and (= 200 status) content-length (can-slice? body))
        (let [res (assoc-in res [:headers "Accept-Ranges"] "bytes")]
          (if (or start end)
            (let [start (or start 0)
                  end   (or end (dec content-length))
                  body  (slice body start (inc end))]
              (-> res
                  (assoc :status 206)
                  (assoc-in [:headers "Content-Range"]
                            (str "bytes " start "-" end "/" content-length))
                  (assoc-in [:headers "Content-Length"]
                            (str (- (inc end) start)))
                  (assoc :body body)))
            res))
        res))))
