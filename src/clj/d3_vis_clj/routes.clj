(ns d3-vis-clj.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response]]
            [ring.middleware.transit :as t]
            [d3-vis-clj.db :as db]))

(defn home-routes [_]
  (routes
    (GET "/" _
      (-> "public/index.html"
          (io/resource)
          (io/input-stream)
          (response)
          (assoc :headers {"Content-Type" "text/html; charset=utf-8"})))
    (-> "/mop/"
        (GET [id]
          (-> id
              (keyword)
              (vector)
              (db/select-mops)
              (response)))
        (t/wrap-transit-response))
    (-> "/hierarchy/"
        (GET _
          (-> (db/get-hierarchy)
              (response)))
        (t/wrap-transit-response))
    (-> "/names/"
        (GET _
          (-> (db/get-names)
              (response)))
        (t/wrap-transit-response))
    (resources "/")))

