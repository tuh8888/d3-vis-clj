(ns d3-vis-clj.d3-force
  (:require [d3-vis-clj.util :as util]
            [cljsjs.d3]
            [re-frame.core :as rf]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]))

(defn translate [x y]
  (str "translate(" x "," y ")"))

(defn coord [d x-y]
  (case x-y
    :x (.-x d)
    :y (.-y d)))

(defn set-links!
  [sim new-links]
  (-> sim
      (.force "link")
      (.links new-links)))

(defn get-links
  [sim]
  (-> sim
      (.force "link")
      (.links)))

(defn set-nodes!
  [sim new-nodes]
  (.nodes sim new-nodes))

(defn get-nodes [sim]
  (.nodes sim))

(defn get-node
  ([sim i]
   (-> sim (get-nodes) (get i)))
  ([sim i c]
   (-> sim (get-node i) (coord c))))

(defn link-endpoint
  [link end]
  (case end
    :source (.-source link)
    :target (.-target link)))

(defn get-link
  ([sim i]
   (-> sim (get-links) (get i)))
  ([sim i end]
   (-> sim (get-link i) (link-endpoint end)))
  ([sim i end c]
   (-> sim (get-link i end) (coord c))))

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

(defn constrain-pos!
  "Constrains pos of d to (x,y)."
  [d x y]
  (doto d
    (constrain-x! x)
    (constrain-y! y)))

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
        r       @(rf/subscribe [:node-size])]
    (when collide
      (-> (js/d3.forceCollide)
          (.radius r)))))

(defn set-forces!
  [sim & {:keys [reset?]}]
  (let [links   (when reset? (get-links sim))
        force-m {:link    (link-force)
                 :collide (collide-force)
                 :charge  (charge-force)
                 :center  (center-force)}]
    (reduce (fn [sim [force force-fn]]
              (.force sim (name force) force-fn))
            sim force-m)
    (when reset? (set-links! sim links))))

(defn update-link-elems
  "Updates link elements with position provided by simulation"
  [sim link-elems]
  (rid3-> link-elems
          {:x1 (fn [_ i] (get-link sim i :source :x))
           :y1 (fn [_ i] (get-link sim i :source :y))
           :x2 (fn [_ i] (get-link sim i :target :x))
           :y2 (fn [_ i] (get-link sim i :target :y))}))


(defn update-node-elems
  "Updates node elements with position provided by simulation"
  [sim node-elems]
  (rid3-> node-elems
          {:transform (fn [_ i]
                        (translate (get-node sim i :x)
                                   (get-node sim i :y)))}))

(defn set-tick!
  [sim]
  (println "Setting tick")
  (-> sim
      (.on "tick"
           (fn []
             (doto sim
               (set-forces! :reset? true)
               (update-node-elems @(rf/subscribe [:get-data :node-elems]))
               (update-link-elems @(rf/subscribe [:get-data :link-elems])))))
      (set-alpha-target! 0.3)
      (.restart)))

(defn call-drag
  [node sim]
  (letfn [(started [_ i]
            (when-not (event-active?)
              (-> sim
                  (set-alpha-target! 0.3)
                  (.restart)))
            (let [d (get-node sim i)]
              (constrain-pos! d (coord d :x)
                                (coord d :y))))
          (dragged [_ i] (-> sim
                             (get-node i)
                             (constrain-pos! (coord js/d3.event :x)
                                             (coord js/d3.event :y))))
          (ended [_ i]
            (when-not (event-active?)
              (set-alpha-target! sim 0))
            (let [d (get-node sim i)]
              (-> sim
                  (get-node i)
                  (constrain-pos! nil
                                  nil))))]
    (.call node (reduce (fn [x [on on-fn]]
                          (.on x (name on) on-fn))
                        (js/d3.drag) {:start started
                                      :drag  dragged
                                      :end   ended}))))

(defn restart
  "Restarts the simulation"
  [sim nodes links]
  (println "Restarting sim")
  (-> sim
      (set-nodes! (clj->js nodes))
      (set-links! (clj->js links)))
  (set-tick! sim))


