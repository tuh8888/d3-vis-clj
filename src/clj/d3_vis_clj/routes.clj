(ns d3-vis-clj.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response]]
            [ring.middleware.transit :as t]
            [d3-vis-clj.db :as db]))

(defn mop
  [id]
  (println id)
  (select-keys (:mops db/example-mops)
               [(keyword id)]))
(defn hierarchy
  [_]
  (:hierarchy db/example-mops))

(defn names
  [_]
  (let [mops (:mops db/example-mops)]
    (->> mops
         (vals)
         (map #(select-keys % [:name :id]))
         (zipmap (keys mops)))))

(defn mop [id]
  (-> db/example-mops
      :mops
      (select-keys [(keyword id)])))

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
          (-> (mop id)
              (response)))
        (t/wrap-transit-response))
    (-> "/hierarchy/"
        (GET request
          (-> request
              (hierarchy)
              (response)))
        (t/wrap-transit-response))
    (-> "/names/"
        (GET request
          (-> request
              (names)
              (response)))
        (t/wrap-transit-response))
    (resources "/")))

