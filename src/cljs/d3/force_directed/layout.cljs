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

(defn set-forces!
  [sim config & {:keys [reset?]}]
  (letfn [(link-force []
            (let [{:keys [distance strength]} (get-in config [:layout-config :link])]
              (cond-> (-> (js/d3.forceLink)
                          (.id d3-util/get-id))
                      strength (.strength (fn [link] (.-strength link)))
                      distance (.distance distance))))

          (charge-force []
            (let [{:keys [strength]} (get-in config [:layout-config :charge])]
              (cond-> (js/d3.forceManyBody)
                      strength (.strength strength))))

          (center-force []
            (let [center (get-in config [:layout-config :center])
                  [width height] @(rf/subscribe [:window-dims])]
              (when center
                (js/d3.forceCenter (/ width 2)
                                   (/ height 2)))))

          (collide-force []
            (let [collide (get-in config [:layout-config :collide])
                  r       (get-in config [:node-config :r])]
              (when collide
                (-> (js/d3.forceCollide)
                    (.radius r)))))]
    (let [links   (when reset? (util/get-links sim))
          force-m {:link    link-force
                   :collide collide-force
                   :charge  charge-force
                   :center  center-force}]
      (reduce (fn [sim [force force-fn]]
                (.force sim (name force) (force-fn)))
              sim force-m)
      (when reset? (set-links! sim links)))))

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
  [sim ratom]
  (println "Setting tick")
  (-> sim
      (.on "tick"
           (fn []
             (doto sim
               (set-forces! @ratom :reset? true)
               (update-node-elems (get-in @ratom [:data :node-elems]))
               (update-link-elems (get-in @ratom [:data :link-elems])))))
      (util/set-alpha-target! 0.3)
      (.restart)))

(defn ^:private -restart
  "Restarts the simulation"
  [sim ratom & {:keys [nodes links]}]
  (println "Restarting sim")
  (let [sim (doto sim
              (->
                (set-nodes! (clj->js (or nodes
                                         (get-in @ratom [:data :nodes]))))
                (set-links! (clj->js (or links
                                         (get-in @ratom [:data :links])))))
              (set-tick! ratom))]
    (.log js/console "Sim restarted"
             "nodes:" (-> sim (util/get-nodes) (alength))
             "links:" (-> sim (util/get-links) (alength)))
    sim))

(defn restart
  "Restarts the simulation"
  [config & {:keys [nodes links]}]
  ((:restart config) :nodes nodes :links links))

(defn new-sim
  "Creates and starts a new simulation."
  [ratom]
  (let [sim (doto (js/d3.forceSimulation)
              (set-forces! @ratom)
              (-restart ratom))]
    {:sim     sim
     :restart (partial -restart sim ratom)}))
