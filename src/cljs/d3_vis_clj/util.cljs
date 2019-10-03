(ns d3-vis-clj.util
  (:require [goog.object :as gobj]))

(defn get-label
  [d]
  (gobj/get d "label"))

(defn get-data [ratom]
  (:data @ratom))

(defn get-width [ratom]
  (:width @ratom))

(defn get-height [ratom]
  (let [width (get-width ratom)]
    (* 0.8 width)))

(defn get-value [d]
  (gobj/get d "value"))

(defn get-id [d]
  (gobj/get d "id"))

(defn get-x [d]
  (gobj/get d "x"))

(defn get-y [d]
  (gobj/get d "y"))

(defn set-coords [node-elements x-param-name y-param-name]
  (-> node-elements
      (.attr x-param-name get-x)
      (.attr y-param-name get-y)))

(defn set-line-coords [line-elements]
  (-> line-elements
      (.attr "x1" (fn [link]
                    (aget link "source" "x")))
      (.attr "y1" (fn [link]
                    (aget link "source" "y")))
      (.attr "x2" (fn [link]
                    (aget link "target" "x")))
      (.attr "y2" (fn [link]
                    (aget link "target" "y")))))

(defn ->link-elements [node links]
  (-> js/d3
      (.select (str "#" node " svg .rid3-main-container"))
      (.append "g")
      (.attr "class" "links")
      (.selectAll "line")
      (.data links)
      .enter
      (.append "line")))

(defn ->node-elements [node nodes]
  (-> js/d3
      (.select (str "#" node " svg .rid3-main-container"))
      (.append "g")
      (.attr "class" "nodes")
      (.selectAll "circle")
      (.data nodes)
      .enter
      (.append "circle")))

(defn ->text-elements [node nodes]
  (-> js/d3
      (.select (str "#" node " svg .rid3-main-container"))
      (.append "g")
      (.attr "class" "texts")
      (.selectAll "text")
      (.data nodes)
      .enter
      (.append "text")))

(defn get-node-color [node]
  (let [level (gobj/get node "level")]
    (if (= 1 level)
      "red"
      "grey")))

(defn link-force []
  (-> js/d3
      .forceLink
      (.id get-id)
      (.strength (fn [link] (gobj/get link "strength")))))

(defn charge-force []
  (.strength (js/d3.forceManyBody) -10))