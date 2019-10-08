(ns d3.force-directed.drag
  (:require [d3.force-directed.util :as util]
            [d3-vis-clj.util :as d3-util]))

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

(defn drag
  [ratom]
  (letfn [(started [_ i]
            (when-not (util/event-active?)
              (-> (get @ratom :sim)
                  (util/set-alpha-target! 0.3)
                  (.restart)))
            (let [d (util/get-node (get @ratom :sim) i)]
              (constrain-pos! d (util/coord d :x)
                              (util/coord d :y))))
          (dragged [_ i] (-> (get @ratom :sim)
                             (util/get-node i)
                             (constrain-pos! (util/coord js/d3.event :x)
                                             (util/coord js/d3.event :y))))
          (ended [_ i]
            (when-not (util/event-active?)
              (util/set-alpha-target! (get @ratom :sim) 0))
            (let [d (util/get-node (get @ratom :sim) i)]
              (constrain-pos! d nil nil)))]
    (d3-util/set-ons (js/d3.drag)
                     {:start started
                      :drag  dragged
                      :end   ended})))
