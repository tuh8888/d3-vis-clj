(ns d3-vis-clj.drag
  (:require [re-frame.core :as rf]
            [d3-vis-clj.d3-force :as force]))

(defn started [_ i]
  (println "start")
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (force/sim-node sim i)]
    (when-not (force/event-active?)
      (-> sim
          (force/set-alpha-target! 0.3)
          (.restart)))
    (force/constrain-x! d (force/coord d :x))
    (force/constrain-y! d (force/coord d :y))))

(defn dragged [_ i]
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (force/sim-node sim i)]
    (force/constrain-x! d (force/coord js/d3.event :x))
    (force/constrain-y! d (force/coord js/d3.event :y))))

(defn ended [_ i]
  (println "end")
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (force/sim-node sim i)]
    (when-not (force/event-active?)
      (force/set-alpha-target! sim 0))
    (force/constrain-x! d nil)
    (force/constrain-y! d nil)))

(defn call-drag
  [node & {:as ons}]
  (.call node (reduce (fn [x [on on-fn]]
                        (.on x (name on) on-fn))
                      (js/d3.drag)
                      {:start started
                       :drag dragged
                       :end ended})))
