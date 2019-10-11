(ns d3-vis-clj.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response]]
            [cheshire.core :refer :all]
            [d3-vis-clj.db :as db]))

(def mop
  (GET "/mop/" [id]
    (println id)
    (generate-string (select-keys (:mops db/example-mops)
                                  [(keyword id)]))))

(comment (mop {:server-port    80
               :server-name    "127.0.0.1"
               :remote-addr    "127.0.0.1"
               :uri            "/hello"
               :scheme         :http
               :headers        {}
               :request-method :get}))

(defn home-routes [_]
  (routes
    (GET "/" _
      (-> "public/index.html"
          io/resource
          io/input-stream
          response
          (assoc :headers {"Content-Type" "text/html; charset=utf-8"})))
    mop
    (resources "/")))

