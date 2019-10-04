(ns d3-vis-clj.d3-force
  (:require [d3-vis-clj.util :as util]
            [cljsjs.d3]
            [re-frame.core :as rf]
            [goog.object :as gobj]))

(defn translate [x y]
  (str "translate(" x "," y ")"))

(defn coord [d x-y]
  (case x-y
    :x (.-x d)
    :y (.-y d)))

(defn sim-links!
  [sim new-links]
  (-> sim
      (.force "link")
      (.links new-links)))

(defn sim-links
  [sim]
  (-> sim
      (.force "link")
      (.links)))

(defn sim-nodes!
  [sim new-nodes & {:keys [tick]}]
  (-> sim
      (.nodes new-nodes)
      (.on "tick" tick)))

(defn sim-nodes [sim]
  (.nodes sim))

(defn sim-node
  ([sim i]
   (-> sim (sim-nodes) (get i)))
  ([sim i c]
   (-> sim (sim-node i) (coord c))))

(defn link-endpoint
  [link end]
  (case end
    :source (.-source link)
    :target (.-target link)))

(defn sim-link
  ([sim i]
   (-> sim (sim-links) (get i)))
  ([sim i end]
   (-> sim (sim-link i) (link-endpoint end)))
  ([sim i end c]
   (-> sim (sim-link i end) (coord c))))

(defn event-active? []
  (-> js/d3
      (.-event)
      (.-active)
      (zero?)
      (not)))

(defn constrain-x! [d x]
  (set! (.-fx d) x))

(defn constrain-y! [d y]
  (set! (.-fy d) y))

(defn set-alpha-target! [sim alpha-target]
  (.alphaTarget sim alpha-target))

(defn link-force
  []
  (let [{:keys [distance strength]} @(rf/subscribe [:layout-config :link])]
    (cond-> (-> (js/d3.forceLink)
                (.id util/get-id))
            strength (.strength (fn [link]
                                  (gobj/get link "strength")))
            distance (.distance distance))))

(defn charge-force []
  (let [{:keys [strength]} (rf/subscribe [:layout-config :charge])]
    (cond-> (js/d3.forceManyBody)
            strength (.strength strength))))

(defn center-force []
  (let [center @(rf/subscribe [:layout-config :center])
        [width height] @(rf/subscribe [:window-dims])]
    (when center
      (js/d3.forceCenter (/ width 2)
                         (/ height 2)))))

(defn collide-force []
  (let [collide @(rf/subscribe [:layout-config :collide])
        r @(rf/subscribe [:node-size])]
    (when collide
      (-> (js/d3.forceCollide)
          (.radius (fn [node] r))))))

(defn set-forces!
  [sim links]
  (reduce (fn [sim [force force-fn]]
            (.force sim (name force) force-fn))
          sim
          {:link (link-force)
           :collide (collide-force)
           :charge (charge-force)
           :center (center-force)})
  (sim-links! sim links))
