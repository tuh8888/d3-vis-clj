(ns d3-vis-clj.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources]]
    #_[ring.middleware.json :refer [wrap-json-response]
            [ring.util.response :refer [response]]]))

(def test-route (GET "/hello" []
                  "hello" #_(wrap-json-response (response {:hello "world!"}))))

(comment (test-route {:server-port    80
                      :server-name    "127.0.0.1"
                      :remote-addr    "127.0.0.1"
                      :uri            "/hello"
                      :scheme         :http
                      :headers        {}
                      :request-method :get}))

(defn home-routes [endpoint]
  (routes
    (GET "/" _
      (-> "public/index.html"
          io/resource
          io/input-stream
          response
          (assoc :headers {"Content-Type" "text/html; charset=utf-8"})))
    test-route
    (resources "/")))

