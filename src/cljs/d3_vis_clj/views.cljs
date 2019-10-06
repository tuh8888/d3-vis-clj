(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3.force-directed.drag :as drag]
            [goog.object :as gobj]
            [d3-vis-clj.util :as util]))

(defn get-node-color [d]
  (let [id (keyword (util/get-id d))]
    (cond (gobj/get d (name :hovered)) "yellow"
          (isa? @(rf/subscribe [:hierarchy]) id :A) "red"
          (isa? @(rf/subscribe [:hierarchy]) id :B) "blue"
          :default "green")))

(defn link-did-mount
  [node viz-name]
  (let [{:keys [stroke-width stroke]} @(rf/subscribe [:link-config viz-name])]
    (rid3-> node
            {:stroke-width stroke-width
             :stroke       stroke})))

(defn node-did-mount
  [node viz-name]
  (-> node
      (rid3-> {:r    @(rf/subscribe [:node-size viz-name])
               :fill (fn [_ i] @(rf/subscribe [:node-color viz-name i]))})
      (drag/call-drag (rf/subscribe [:force-layout viz-name]))
      (util/set-ons
        :mouseover (fn [_ i] (rf/dispatch [:set-hovered viz-name i true]))
        :mouseout (fn [_ i] (rf/dispatch [:set-hovered viz-name i false]))
        :click (fn [_ i] (rf/dispatch [:expand-node viz-name i])))))

(defn text-did-mount
  [node _]
  (-> node
      (rid3-> {:text-anchor "middle"})
      (.text (fn [d] (gobj/get d "name")))))

(defn prepare-data
  [viz-name var]
  (-> @(rf/subscribe [:force-layout viz-name])
      (get-in [:data var])
      (clj->js)))

(defn force-viz [ratom]
  (let [viz-name :network]
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
                                   (rf/dispatch-sync
                                     [:set-data viz-name
                                      :link-elems (link-did-mount node viz-name)]))
                :prepare-dataset (fn [_] (prepare-data viz-name :links))}
               {:kind            :elem-with-data
                :tag             "circle"
                :class           "node"
                :did-mount       (fn [node _]
                                   (rf/dispatch-sync
                                     [:set-data viz-name
                                      :node-elems (node-did-mount node viz-name)]))
                :prepare-dataset (fn [_] (prepare-data viz-name :nodes))}
               {:kind            :elem-with-data
                :tag             "text"
                :class           "texts"
                :did-mount       (fn [node _]
                                   (rf/dispatch-sync
                                     [:set-data viz-name
                                      :text-elems (text-did-mount node viz-name)]))
                :prepare-dataset (fn [_] (prepare-data viz-name :nodes))}]}]))


(defn node-size-text-box []
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     @(rf/subscribe [:node-size :network])
            :on-change #(rf/dispatch [:resize-nodes :network
                                      (util/text-value %)])}]])

(defn add-node-btn
  []
  [:div
   [:button {:type     "button"
             :on-click #(rf/dispatch [:add-node :network])}
    "Add Node"]
   [:input {:type      "text"
            :value     @(rf/subscribe [:node-to-add :network])
            :on-change #(rf/dispatch [:set-node-to-add :network
                                      (util/text-value %)])}]])

(defn main-panel []
  [:div
   [:h1 @(rf/subscribe [:name])]
   [node-size-text-box]
   [add-node-btn]
   (let [data (rf/subscribe [:force-layout :network])]
     [force-viz data])])