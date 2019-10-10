(ns d3-vis-clj.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response]]
            [cheshire.core :refer :all]))

(def test-route
  (GET "/hello/:id" [id]
    (println id)
    (generate-string {:hello (case id
                               "hello" "world!"
                               "goodbye" "hasta luego"
                               "what did you say?")})))

(comment (test-route {:server-port    80
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
    test-route
    (resources "/")))

