(ns d3-vis-clj.drag
  (:require [re-frame.core :as rf]
            [d3-vis-clj.d3-force :as force]))


(defn call-drag
  [node sim]
  (letfn [(started [_ i]
            (println "start")
            (let [d    (force/sim-node sim i)]
              (when-not (force/event-active?)
                (-> sim
                    (force/set-alpha-target! 0.3)
                    (.restart)))
              (force/constrain-x! d (force/coord d :x))
              (force/constrain-y! d (force/coord d :y))))

          (dragged [_ i]
            (let [d (force/sim-node sim i)]
              (force/constrain-x! d (force/coord js/d3.event :x))
              (force/constrain-y! d (force/coord js/d3.event :y))))

          (ended [_ i]
            (println "end")
            (let [d   (force/sim-node sim i)]
              (when-not (force/event-active?)
                (force/set-alpha-target! sim 0))
              (force/constrain-x! d nil)
              (force/constrain-y! d nil)))]
    (.call node (reduce (fn [x [on on-fn]]
                          (.on x (name on) on-fn))
                        (js/d3.drag)
                        {:start started
                         :drag  dragged
                         :end   ended}))))