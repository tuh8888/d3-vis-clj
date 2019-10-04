(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.drag :as drag]
            [goog.object :as gobj]
            [d3-vis-clj.d3-force :as force]))

(defn get-node-color [node]
  (let [level (gobj/get node "level")]
    (case level
      1 "red"
      2 "blue"
      3 "green")))

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
              :prepare-dataset (fn [_]
                                 (clj->js @(rf/subscribe [:get-data :links])))}

             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node _]
                                 (let [node-elems (-> node
                                                      (rid3-> {:r    @(rf/subscribe [:node-size])
                                                               :fill get-node-color})
                                                      (force/call-drag @(rf/subscribe [:sim])))]
                                   (rf/dispatch-sync [:set-data :node-elems node-elems])))
              :prepare-dataset (fn [_]
                                 (clj->js @(rf/subscribe [:get-data :nodes])))}]}])

(defn node-size-text-box []
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     @(rf/subscribe [:node-size])
            :on-change #(rf/dispatch [:resize-nodes (-> % .-target .-value)])}]])

(defn add-node-btn
  []
  [:div
   [:button {:type     "button"
             :on-click #(rf/dispatch [:add-node])}
    "Add Node"]])

(defn main-panel []
  [:div
   [node-size-text-box]
   [add-node-btn]
   (let [data (rf/subscribe [:db])]
     [force-viz data])])