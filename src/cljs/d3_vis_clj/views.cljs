(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]
            [d3-vis-clj.d3-force :as force]))

(defn prepare-data [ratom v]
  (-> @ratom
      :dataset
      (get v)
      (clj->js)))

(defn tick-handler [sim node-elems link-elems]
  (fn []
    (rid3-> node-elems
            {:transform (fn [_ i]
                          (force/translate (force/sim-node sim i :x)
                                           (force/sim-node sim i :y)))})
    (rid3-> link-elems
            {:x1 (fn [_ i] (force/sim-link sim i :source :x))
             :y1 (fn [_ i] (force/sim-link sim i :source :y))
             :x2 (fn [_ i] (force/sim-link sim i :target :x))
             :y2 (fn [_ i] (force/sim-link sim i :target :y))})))

(defn sim-did-mount [ratom]
  (let [sim        (force/force-sim ratom)
        nodes      (prepare-data ratom :nodes)
        links      (prepare-data ratom :links)
        node-elems @(rf/subscribe [:get-var :node-elems])
        link-elems @(rf/subscribe [:get-var :link-elems])]
    (force/sim-nodes! sim nodes :tick (tick-handler sim node-elems link-elems))
    (force/sim-links! sim links)
    (rf/dispatch [:set-var :sim sim])))

(defn sim-did-update [ratom])


(defn drag-started [d i]
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

(defn drag-ended [_ i]
  (println "end")
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (force/sim-node sim i)]
    (when-not (force/event-active?)
      (force/set-alpha-target! sim 0))
    (force/constrain-x! d nil)
    (force/constrain-y! d nil)))


(defn force-viz [ratom]
  [rid3/viz
   {:id     "force"
    :ratom  ratom
    :svg    {:did-mount (fn [node ratom]
                          (rid3-> node
                                  {:width  (:width @ratom)
                                   :height (:height @ratom)
                                   :style  {:background-color "grey"}}))}

    :pieces [{:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node ratom]
                                 (let [{{:keys [r fill]} :node-config} @ratom
                                       r (-> node
                                             (rid3-> {:r    r
                                                      :fill fill})
                                             (force/drag :start drag-started
                                                         :drag dragged
                                                         :end drag-ended))]
                                   (rf/dispatch-sync [:set-var :node-elems r])))
              :prepare-dataset #(prepare-data % :nodes)}

             {:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node ratom]
                                 (let [{{:keys [stroke-width stroke]} :link-config} @ratom
                                       r (rid3-> node
                                                 {:stroke-width stroke-width
                                                  :stroke       stroke})]
                                   (rf/dispatch-sync [:set-var :link-elems r])))
              :prepare-dataset #(prepare-data % :links)}
             {:kind       :raw
              :did-mount  sim-did-mount
              :did-update sim-did-update}]}])



(defn main-panel []
  (rf/dispatch-sync [:window-width js/window.innerWidth])
  (rf/dispatch-sync [:window-height js/window.innerHeight])

  (let [data (rf/subscribe [:data])]
    [force-viz data]))