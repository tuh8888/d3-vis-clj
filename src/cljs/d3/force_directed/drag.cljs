(ns d3.force-directed.drag
  (:require [d3.force-directed.util :as util]))

(defn ^:private constrain-x!
  [d x]
  (set! (.-fx d) x))

(defn ^:private constrain-y!
  [d y]
  (set! (.-fy d) y))

(defn ^:private constrain-pos!
  "Constrains pos of d to (x,y)."
  [d x y]
  (doto d
    (constrain-x! x)
    (constrain-y! y)))

(defn call-drag
  [node sim]
  (letfn [(started [_ i]
            (when-not (util/event-active?)
              (-> sim
                  (util/set-alpha-target! 0.3)
                  (.restart)))
            (let [d (util/get-node sim i)]
              (constrain-pos! d (util/coord d :x)
                              (util/coord d :y))))
          (dragged [_ i] (-> sim
                             (util/get-node i)
                             (constrain-pos! (util/coord js/d3.event :x)
                                             (util/coord js/d3.event :y))))
          (ended [_ i]
            (when-not (util/event-active?)
              (util/set-alpha-target! sim 0))
            (let [d (util/get-node sim i)]
              (constrain-pos! d nil nil)))]
    (.call node (reduce (fn [x [on on-fn]]
                          (.on x (name on) on-fn))
                        (js/d3.drag) {:start started
                                      :drag  dragged
                                      :end   ended}))))
