(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.util :as util]))

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)

(defn link-did-mount
  [node viz-name]
  (let [{:keys [stroke-width stroke]} (<sub [:link-config viz-name])]
    (rid3-> node
            {:stroke-width stroke-width
             :stroke       stroke})))

(defn node-or-text-did-mount
  [node viz-name]
  (-> node
      (.call (<sub [:drag-fn viz-name]))
      (util/set-ons
        :mouseover (fn [_ i] (>evt [:set-hovered viz-name i true]))
        :mouseout (fn [_ i] (>evt [:set-hovered viz-name i false]))
        :click (fn [_ i] (>evt [:expand-node viz-name i])))))

(defn node-did-mount
  [node viz-name]
  (-> node
      (rid3-> {:r    (<sub [:node-size viz-name])
               :fill (fn [_ i] (<sub [:node-color viz-name i]))})
      (node-or-text-did-mount viz-name)))

(defn text-did-mount
  [node viz-name]
  (-> node
      (rid3-> {:text-anchor "middle"})
      (.text (fn [_ i] (<sub [:node-name viz-name i])))
      (node-or-text-did-mount viz-name)))

(defn force-viz [viz-name ratom]
  [rid3/viz
   {:id     (name viz-name)
    :ratom  ratom
    :svg    {:did-mount  (fn [_ v]
                           (rf/dispatch-sync [:initialize-force-layout
                                              viz-name]))


             :did-update (fn [node ratom]
                           (let [{:keys [width height]} @ratom]
                             (rid3-> node
                                     {:width  width
                                      :height height
                                      :style  {:background-color "grey"}})))}

    :pieces [{:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node _]
                                 (rf/dispatch-sync
                                   [:set-link-elems viz-name
                                    (link-did-mount node viz-name)]))
              :prepare-dataset (fn [_] (<sub [:get-links-js viz-name]))}
             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node _]
                                 (rf/dispatch-sync
                                   [:set-node-elems viz-name
                                    (node-did-mount node viz-name)]))
              :prepare-dataset (fn [_] (<sub [:get-nodes-js viz-name]))}
             {:kind            :elem-with-data
              :tag             "text"
              :class           "texts"
              :did-mount       (fn [node _]
                                 (rf/dispatch-sync
                                   [:set-text-elems viz-name
                                    (text-did-mount node viz-name)]))
              :prepare-dataset (fn [_] (<sub [:get-nodes-js viz-name]))}]}])

(defn node-size-text-box []
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     (<sub [:node-size :network])
            :on-change #(>evt [:resize-nodes :network
                               (util/text-value %)])}]])

(defn add-node-btn
  []
  [:div
   [:button {:type     "button"
             :on-click #(>evt [:add-node :network])}
    "Add Node"]
   [:input {:type      "text"
            :value     (<sub [:node-to-add :network])
            :on-change #(>evt [:set-node-to-add :network
                               (util/text-value %)])}]])

(defn main-panel []
  [:div
   [:h1 @(rf/subscribe [:name])]
   [node-size-text-box]
   [add-node-btn]
   (let [viz-name :network
         ratom    (rf/subscribe [:force-layout viz-name])]
     [force-viz viz-name ratom])])