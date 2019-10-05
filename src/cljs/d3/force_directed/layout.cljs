(ns d3.force-directed.layout
  (:require [d3-vis-clj.util :as d3-util]
            [cljsjs.d3]
            [re-frame.core :as rf]
            [rid3.core :refer [rid3->]]
            [d3.force-directed.util :as util]))

(defn ^:private set-links!
  [sim new-links]
  (-> sim
      (.force "link")
      (.links new-links)))

(defn ^:private set-nodes!
  [sim new-nodes]
  (.nodes sim new-nodes))

(defn ^:private link-force
  [viz-name]
  (let [{:keys [distance strength]} @(rf/subscribe [:layout-config viz-name :link])]
    (cond-> (-> (js/d3.forceLink)
                (.id d3-util/get-id))
            strength (.strength (fn [link] (.-strength link)))
            distance (.distance distance))))

(defn ^:private charge-force
  [viz-name]
  (let [{:keys [strength]} (rf/subscribe [:layout-config viz-name :charge])]
    (cond-> (js/d3.forceManyBody)
            strength (.strength strength))))

(defn ^:private center-force
  [viz-name]
  (let [center @(rf/subscribe [:layout-config viz-name :center])
        [width height] @(rf/subscribe [:window-dims])]
    (when center
      (js/d3.forceCenter (/ width 2)
                         (/ height 2)))))

(defn ^:private collide-force
  [viz-name]
  (let [collide @(rf/subscribe [:layout-config viz-name :collide])
        r       @(rf/subscribe [:node-size viz-name])]
    (when collide
      (-> (js/d3.forceCollide)
          (.radius r)))))

(defn set-forces!
  [sim viz-name & {:keys [reset?]}]
  (let [links   (when reset? (util/get-links sim))
        force-m {:link    (link-force viz-name)
                 :collide (collide-force viz-name)
                 :charge  (charge-force viz-name)
                 :center  (center-force viz-name)}]
    (reduce (fn [sim [force force-fn]]
              (.force sim (name force) force-fn))
            sim force-m)
    (when reset? (set-links! sim links))))

(defn ^:private update-link-elems
  "Updates link elements with position provided by simulation"
  [sim link-elems]
  (rid3-> link-elems
          {:x1 (fn [_ i] (util/get-link sim i :source :x))
           :y1 (fn [_ i] (util/get-link sim i :source :y))
           :x2 (fn [_ i] (util/get-link sim i :target :x))
           :y2 (fn [_ i] (util/get-link sim i :target :y))}))


(defn ^:private update-node-elems
  "Updates node elements with position provided by simulation"
  [sim node-elems]
  (rid3-> node-elems
          {:transform (fn [_ i]
                        (util/translate (util/get-node sim i :x)
                                        (util/get-node sim i :y)))}))

(defn ^:private set-tick!
  [sim viz-name]
  (println viz-name "Setting tick")
  (-> sim
      (.on "tick"
           (fn []
             (doto sim
               (set-forces! viz-name :reset? true)
               (update-node-elems @(rf/subscribe [:get-data viz-name :node-elems]))
               (update-link-elems @(rf/subscribe [:get-data viz-name :link-elems])))))
      (util/set-alpha-target! 0.3)
      (.restart)))

(defn restart
  "Restarts the simulation"
  [sim viz-name nodes links]
  (println viz-name "Restarting sim")
  (let [sim (doto sim
              (->
                (set-nodes! (clj->js nodes))
                (set-links! (clj->js links)))
              (set-tick! viz-name))]
    (println viz-name "Sim restarted"
             "nodes:" (alength (util/get-nodes sim))
             "links:" (alength (util/get-links sim)))
    sim))

(defn new-sim
  "Creates and starts a new simulation."
  [viz-name nodes links]
  (doto (js/d3.forceSimulation)
    (set-forces! viz-name)
    (restart viz-name
             nodes links)))