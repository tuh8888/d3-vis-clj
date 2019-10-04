(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.drag :as drag]
            [d3-vis-clj.d3-force :as force]))

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

(defn sim-did-mount [ratom]
  (let [sim        (force/force-sim ratom)
        nodes      (prepare-data ratom :nodes)
        links      (prepare-data ratom :links)
        node-elems @(rf/subscribe [:get-data :node-elems])
        link-elems @(rf/subscribe [:get-data :link-elems])]
    (force/sim-nodes! sim nodes :tick (tick-handler sim node-elems link-elems))
    (force/sim-links! sim links)
    (rf/dispatch [:set-sim sim])))

(defn sim-did-update [ratom])

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
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node ratom]
                                 (let [{{:keys [stroke-width stroke]} :link-config} @ratom
                                       r (rid3-> node
                                                 {:stroke-width stroke-width
                                                  :stroke       stroke})]
                                   (rf/dispatch-sync [:set-data :link-elems r])))
              :prepare-dataset #(prepare-data % :links)}

             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node ratom]
                                 (let [{{:keys [r fill]} :node-config} @ratom
                                       r (-> node
                                             (rid3-> {:r    r
                                                      :fill fill})
                                             (drag/call-drag))]
                                   (rf/dispatch-sync [:set-data :node-elems r])))
              :prepare-dataset #(prepare-data % :nodes)}
             {:kind       :raw
              :did-mount  sim-did-mount
              :did-update sim-did-update}]}])

(defn node-size-text-box
  []
  [:div
   "Node size: "
   [:input {:type "text"
            :value @(rf/subscribe [:node-size])
            :on-change #(rf/dispatch [:resize-nodes (-> % .-target .-value)])}]])

(defn main-panel []
  (rf/dispatch-sync [:window-resize])
  [:div
   [node-size-text-box]
   (let [data (rf/subscribe [:db])]
     [force-viz data])])