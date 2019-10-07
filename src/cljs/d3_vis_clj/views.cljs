(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.util :refer [<sub >evt] :as util]))

(defn link-did-mount
  [node viz-id]
  (let [{:keys [stroke-width stroke]} (<sub [:link-config viz-id])]
    (rid3-> node
            {:stroke-width stroke-width
             :stroke       stroke})))

(defn node-or-text-did-mount
  [node viz-id]
  (-> node
      (.call (<sub [:drag-fn viz-id]))
      (util/set-ons
        :mouseover (fn [_ i] (>evt [:set-hovered viz-id i true]))
        :mouseout (fn [_ i] (>evt [:set-hovered viz-id i false]))
        :click (fn [_ i] (>evt [:expand-node viz-id i])))))

(defn node-did-mount
  [node viz-id]
  (-> node
      (rid3-> {:r    (<sub [:node-size viz-id])
               :fill (fn [_ i] (<sub [:node-color viz-id i]))})
      (node-or-text-did-mount viz-id)))

(defn text-did-mount
  [node viz-id]
  (-> node
      (rid3-> {:text-anchor "middle"})
      (.text (fn [_ i] (<sub [:node-name viz-id i])))
      (node-or-text-did-mount viz-id)))

(defn force-viz-graph [viz-id]
  [rid3/viz
   {:id     (str (name viz-id) "-graph")
    :ratom  (rf/subscribe [:force-layout viz-id])
    :svg    {:did-mount  (fn [_ _]
                           (rf/dispatch-sync [:initialize-force-layout
                                              viz-id]))

             :did-update (fn [node _]
                           (rid3-> node
                                   {:width  (<sub [:window-width])
                                    :height (<sub [:window-height])
                                    :style  {:background-color "grey"}}))}

    :pieces [{:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node _]
                                 (rf/dispatch-sync
                                   [:set-link-elems viz-id
                                    (link-did-mount node viz-id)]))
              :prepare-dataset (fn [_] (<sub [:get-links-js viz-id]))}
             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node _]
                                 (rf/dispatch-sync
                                   [:set-node-elems viz-id
                                    (node-did-mount node viz-id)]))
              :prepare-dataset (fn [_] (<sub [:get-nodes-js viz-id]))}
             {:kind            :elem-with-data
              :tag             "text"
              :class           "texts"
              :did-mount       (fn [node _]
                                 (rf/dispatch-sync
                                   [:set-text-elems viz-id
                                    (text-did-mount node viz-id)]))
              :prepare-dataset (fn [_] (<sub [:get-nodes-js viz-id]))}]}])

(defn node-size-text-box
  [viz-id]
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     (<sub [:node-size viz-id])
            :on-change #(>evt [:resize-nodes viz-id
                               (util/text-value %)])}]])

(defn add-node-btn
  [viz-id]
  [:div
   [:button {:type     "button"
             :on-click #(>evt [:add-node viz-id])}
    "Add Node"]
   [:input {:type      "text"
            :value     (<sub [:node-to-add viz-id])
            :on-change #(>evt [:set-node-to-add viz-id
                               (util/text-value %)])}]])

(defn force-viz
  [viz-id]
  [:div
   [node-size-text-box viz-id]
   [add-node-btn viz-id]
   [force-viz-graph viz-id]])

(defn main-panel []
  [:div
   [:h1 (<sub [:name])]
   [force-viz :network]])