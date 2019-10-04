(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.drag :as drag]
            [reagent.core :as reagent]
            [goog.object :as gobj]
            [d3-vis-clj.d3-force :as force]))

(defonce app-state (reagent/atom {}))

(defn prepare-data [ratom v]
  (-> @ratom
      :data
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

(defn sim-did-mount [sim _]
  (let [nodes      (clj->js @(rf/subscribe [:get-data :nodes]))
        links      (clj->js @(rf/subscribe [:get-data :links]))
        node-elems @(rf/subscribe [:get-data :node-elems])
        link-elems @(rf/subscribe [:get-data :link-elems])]
    (force/sim-nodes! sim nodes :tick (tick-handler sim node-elems link-elems))
    (force/set-forces! sim links)))

(defn sim-did-update [sim _]
  (let [nodes      (.nodes sim)
        links      (clj->js @(rf/subscribe [:get-data :links]))
        node-elems @(rf/subscribe [:get-data :node-elems])
        link-elems @(rf/subscribe [:get-data :link-elems])]
    (force/sim-nodes! sim nodes :tick (tick-handler sim node-elems link-elems))
    (force/set-forces! sim links)))

(defn get-node-color [node]
  (let [level (gobj/get node "level")]
    (case level
      1 "red"
      2 "blue"
      3 "green")))

(def sim (js/d3.forceSimulation))

(defn force-viz [ratom]
  [rid3/viz
   {:id     "force"
    :ratom  ratom
    :svg    {:did-mount (fn [node _]
                          (let [[width height] @(rf/subscribe [:window-dims])]
                            (rid3-> node
                                    {:width  width
                                     :height height
                                     :style  {:background-color "grey"}})))}

    :pieces [{:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node _]
                                 (let [{:keys [stroke-width stroke]} @(rf/subscribe [:link-config])
                                       r (rid3-> node
                                                 {:stroke-width stroke-width
                                                  :stroke       stroke})]
                                   (rf/dispatch-sync [:set-data :link-elems r])))
              :prepare-dataset #(prepare-data % :links)}

             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node _]
                                 (let [node-elems (-> node
                                                      (rid3-> {:r    @(rf/subscribe [:node-size])
                                                               :fill get-node-color})
                                                      (drag/call-drag sim))]
                                   (rf/dispatch-sync [:set-data :node-elems node-elems])))
              :prepare-dataset #(prepare-data % :nodes)}

             {:kind       :raw
              :did-mount  #(sim-did-mount sim %)
              :did-update #(sim-did-update sim %)}]}])

(defn node-size-text-box []
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     @(rf/subscribe [:node-size])
            :on-change #(rf/dispatch [:resize-nodes (-> % .-target .-value)])}]])

(defn add-node-btn
  []
  [:div
   [:button {:type      "button"
             :on-click #(rf/dispatch [:add-node sim])}
    "Add Node"]])

(defn main-panel []
  (rf/dispatch-sync [:window-resize])
  [:div
   [node-size-text-box]
   [add-node-btn]
   (let [data (rf/subscribe [:db])]
     [force-viz data])])