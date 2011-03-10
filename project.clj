(defproject ring-partial-content "0.0.1"
  :description "Partial Content middleware for ring."
  
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  
  :dev-dependencies [[autodoc "0.7.1"]]
  
  :autodoc {:description "Ring middleware to provide Partial Content responses as described in RFC2616 section 10.2.7."
            :copyright "Copyright (c) Remco van 't Veer."
            :web-src-dir "http://github.com/remvee/ring-partial-content/blob/"})
